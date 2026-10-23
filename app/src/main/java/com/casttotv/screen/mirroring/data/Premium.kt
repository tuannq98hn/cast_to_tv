package com.casttotv.screen.mirroring.data

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Replaces `eq0.d()` (IKUtils.isUserIAPAvailable) and the ikame `IKBilling` wrapper with Play Billing.
 * The entitlement is cached in prefs so it is known before the billing service connects.
 */
object Premium {
    const val MONTHLY = "cgt_monthly_notrial_s1"
    const val YEARLY = "cgt_yearly_notrial_s1"
    const val LIFETIME = "cgt_lifetime_notrial_s1"
    const val YEARLY_INTRO = "sub_yearly_none_trial"
    const val SALE_YEARLY = YEARLY
    const val SUPER_SALE = "purchase_super_sale"

    /** `na.a` / `na.b`: every subscription and one-time product the app ever sold; owning any of them is premium. */
    private val SUBS = listOf(
        "sub_one_month", "sub_sale_month", "sub_sale_year", "sub_one_year", "sub_year_399", "sub_weekly", YEARLY_INTRO,
        "cgt_weekly_notrial_s1", MONTHLY, YEARLY, "cgt_weekly_notrial_s2", "cgt_monthly_notrial_s2", "cgt_yearly_notrial_s2",
        "cgt_weekly_notrial_s3", "cgt_monthly_notrial_s3", "cgt_yearly_3daystrial_s3", "cgt_weekly_notrial_s4",
        "cgt_monthly_notrial_s4", "cgt_yearly_3daystrial_s4", "sub_week_high",
    )
    private val INAPP = listOf(
        "remove_ads", "buy_forever", "purchase_onetime_sale", SUPER_SALE, LIFETIME, "cgt_lifetime_notrial_s2",
        "cgt_lifetime_notrial_s3", "cgt_lifetime_notrial_s4",
    )

    private lateinit var context: Context
    private var client: BillingClient? = null
    private val details = mutableMapOf<String, ProductDetails>()
    private var onResult: ((Boolean) -> Unit)? = null

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active
    val isActive: Boolean get() = _active.value

    fun init(context: Context) {
        this.context = context.applicationContext
        _active.value = prefs().getBoolean("is_premium", false)
        client = BillingClient.newBuilder(this.context)
            .setListener { result, purchases -> onPurchases(result, purchases) }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        connect()
    }

    private fun connect(then: () -> Unit = {}) {
        val c = client ?: return
        if (c.isReady) return then()
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                restore()
                queryDetails(then)
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryDetails(then: () -> Unit) {
        val c = client ?: return
        var pending = 2
        fun query(ids: List<String>, type: String) {
            val products = ids.map { QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(type).build() }
            c.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { _, list ->
                list.forEach { details[it.productId] = it }
                if (--pending == 0) then()
            }
        }
        query(SUBS, BillingClient.ProductType.SUBS)
        query(INAPP, BillingClient.ProductType.INAPP)
    }

    /** Owned subscriptions / lifetime decide the entitlement (`IKBilling` restore on start). */
    fun restore() {
        val c = client ?: return
        var owned = false
        var pending = 2
        listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP).forEach { type ->
            c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { _, purchases ->
                purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach {
                    owned = true
                    acknowledge(it)
                }
                if (--pending == 0) setActive(owned)
            }
        }
    }

    /** Formatted price of a product (`getPriceSubscribe` / `getPricePurchase`), or null before details arrive. */
    fun price(productId: String, onPrice: (String) -> Unit) = connect {
        val d = details[productId] ?: return@connect
        val price = d.oneTimePurchaseOfferDetails?.formattedPrice
            ?: d.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
            ?: return@connect
        onPrice(price)
    }

    /** `getPriceSubscribe(id, percent)`: the price plus the "regular" price it is shown as a [percent]% discount of. */
    fun salePrice(productId: String, percent: Int, onPrice: (price: String, regular: String) -> Unit) = connect {
        val d = details[productId] ?: return@connect
        val (micros, code, text) = d.oneTimePurchaseOfferDetails?.let { Triple(it.priceAmountMicros, it.priceCurrencyCode, it.formattedPrice) }
            ?: d.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()
                ?.let { Triple(it.priceAmountMicros, it.priceCurrencyCode, it.formattedPrice) }
            ?: return@connect
        val format = java.text.NumberFormat.getCurrencyInstance().apply { currency = java.util.Currency.getInstance(code) }
        onPrice(text, format.format(micros / 1_000_000.0 * 100 / (100 - percent)))
    }

    /** `subscribe` / `purchase`: launches the Play sheet; [result] gets true once the product is owned. */
    fun buy(activity: Activity, productId: String, result: (Boolean) -> Unit) = connect {
        val d = details[productId]
        if (d == null) {
            activity.runOnUiThread { result(false) }
            return@connect
        }
        onResult = result
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d)
        d.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let(params::setOfferToken)
        activity.runOnUiThread {
            client?.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params.build())).build())
        }
    }

    private fun onPurchases(result: BillingResult, purchases: List<Purchase>?) {
        val ok = result.responseCode == BillingClient.BillingResponseCode.OK &&
            purchases.orEmpty().any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        purchases.orEmpty().forEach(::acknowledge)
        if (ok) setActive(true)
        val callback = onResult
        onResult = null
        android.os.Handler(android.os.Looper.getMainLooper()).post { callback?.invoke(ok) }
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED || purchase.isAcknowledged) return
        client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) {}
    }

    private fun setActive(value: Boolean) {
        _active.value = value
        prefs().edit { putBoolean("is_premium", value) }
    }

    private fun prefs() = context.getSharedPreferences("app-content", Context.MODE_PRIVATE)
}

package com.casttotv.screen.mirroring.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.data.Premium
import com.casttotv.screen.mirroring.ui.activities.purchase.SubSaleActivity

/** Port of `RemindAlarmReceiver`: every 6 h (first after a day) non-premium users get a random sale reminder. */
class RemindAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION || Premium.isActive) return
        // `MyApplication.e`: no reminder while the app is on screen.
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val titles = listOf(
            R.string.text_title_sale_not_purchase2, R.string.text_title_sale_not_purchase3,
            R.string.text_title_sale_not_purchase4, R.string.text_title_sale_not_purchase5,
        )
        val bodies = listOf(
            R.string.text_content_sale_not_purchase2, R.string.text_content_sale_not_purchase3,
            R.string.text_content_sale_not_purchase4, R.string.text_content_sale_not_purchase5,
            R.string.text_content_sale_not_purchase6, R.string.text_content_sale_not_purchase7,
            R.string.text_content_sale_not_purchase8,
        )
        notify(context, context.getString(titles.random()), context.getString(bodies.random()))
    }

    /** `b(...)`. */
    private fun notify(context: Context, title: String, body: String) {
        val open = Intent(context, SubSaleActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setAction(ACTION)
            .putExtra(SubSaleActivity.EXTRA_CLICK_NOTIFY, 5)
        val tap = PendingIntent.getActivity(context, ID, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(ID)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(ACTION, ACTION, NotificationManager.IMPORTANCE_HIGH).apply {
                    setShowBadge(true)
                    description = ACTION
                },
            )
        }
        val notification = NotificationCompat.Builder(context, ACTION)
            .setSmallIcon(R.drawable.icon_app)
            .setLargeIcon(ContextCompat.getDrawable(context, R.drawable.icon_app)?.toBitmap())
            .setColor(ContextCompat.getColor(context, R.color.dc_primary))
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(ID, notification) }
    }

    companion object {
        const val ACTION = "com.casttotv.screen.mirroring.NOTIFY_ACTION"
        private const val ID = 8383

        /** `MainActivity.onCreate`: (re)arm the reminder for non-premium users. */
        fun schedule(context: Context) {
            if (Premium.isActive) return
            val alarm = context.getSystemService(AlarmManager::class.java)
            val pending = PendingIntent.getBroadcast(
                context, ID, Intent(context, RemindAlarmReceiver::class.java).setAction(ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarm.cancel(pending)
            alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY, 6 * AlarmManager.INTERVAL_HOUR, pending)
        }
    }
}

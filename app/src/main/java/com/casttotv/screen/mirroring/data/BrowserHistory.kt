package com.casttotv.screen.mirroring.data

import android.content.Context
import androidx.core.content.edit
import com.casttotv.screen.mirroring.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** `HistoryBrowser` row of the original Room table `history_browser`. */
data class HistoryBrowser(val id: Int, val title: String, val linkUrl: String, val type: Int, val date: String)

/**
 * Port of `HistoryViewModel` + its Room DAO (`SELECT * from history_browser ORDER BY id ASC`).
 * ponytail: JSON in SharedPreferences instead of Room; fine for a browsing history list.
 */
object BrowserHistory {
    private const val KEY = "history_browser"
    private lateinit var context: Context
    private val _items = MutableStateFlow<List<HistoryBrowser>>(emptyList())
    val items: StateFlow<List<HistoryBrowser>> = _items

    fun init(context: Context) {
        this.context = context.applicationContext
        _items.value = runCatching {
            val array = JSONArray(prefs().getString(KEY, "[]"))
            List(array.length()) { i ->
                array.getJSONObject(i).run {
                    HistoryBrowser(getInt("id"), getString("title"), getString("linkUrl"), getInt("type"), getString("date"))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(item: HistoryBrowser) = save(_items.value + item.copy(id = (_items.value.maxOfOrNull { it.id } ?: 0) + 1))

    fun remove(item: HistoryBrowser) = save(_items.value.filter { it.id != item.id })

    fun clear() = save(emptyList())

    private fun save(list: List<HistoryBrowser>) {
        _items.value = list
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject().put("id", it.id).put("title", it.title).put("linkUrl", it.linkUrl).put("type", it.type).put("date", it.date),
            )
        }
        prefs().edit { putString(KEY, array.toString()) }
    }

    private fun prefs() = context.getSharedPreferences("browser_history", Context.MODE_PRIVATE)

    /** `fl4.w`: site bucket used for the favicon. */
    fun siteOf(url: String): Int {
        val u = url.lowercase()
        return listOf("facebook", "tiktok", "buzzvideo", "google", "twitter", "instagram", "imdb", "twitch", "fox", "livestream", "espn", "dailymotion")
            .indexOfFirst { u.contains(it) }.let { if (it < 0) 3 else it }
    }

    /** `rt0` favicon per [siteOf] bucket. */
    fun iconOf(url: String) = when (siteOf(url)) {
        0 -> R.drawable.ic_facebook
        1 -> R.drawable.ic_tiktok
        2 -> R.drawable.ic_buzz_web
        4 -> R.drawable.ic_twitter_web
        5 -> R.drawable.ic_instagram
        6 -> R.drawable.ic_imdb_web
        7 -> R.drawable.ic_twitch_web
        8 -> R.drawable.ic_fox_web
        9 -> R.drawable.ic_live_vimeo_web
        10 -> R.drawable.ic_espn_web
        11 -> R.drawable.ic_dailymotion
        else -> R.drawable.ic_google_web
    }
}

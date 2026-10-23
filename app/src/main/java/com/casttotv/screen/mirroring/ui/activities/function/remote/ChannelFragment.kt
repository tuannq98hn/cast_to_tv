package com.casttotv.screen.mirroring.ui.activities.function.remote

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.databinding.FragmentChannelBinding
import com.casttotv.screen.mirroring.databinding.ItemChannelFireTvBinding
import com.casttotv.screen.mirroring.databinding.ItemChannelLgBinding
import com.casttotv.screen.mirroring.databinding.ItemChannelSonyBinding
import com.casttotv.screen.mirroring.tv.DeviceType
import com.casttotv.screen.mirroring.tv.FireTvAdb
import com.casttotv.screen.mirroring.tv.RokuEcp
import com.casttotv.screen.mirroring.tv.SamsungRemote
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.ui.activities.function.connect.SearchTVActivity
import com.connectsdk.core.AppInfo
import com.connectsdk.service.capability.Launcher
import com.connectsdk.service.command.ServiceCommandError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.random.Random

/** Port of `ge0` (channel tab): installed apps of the connected TV, tap to launch. */
class ChannelFragment : Fragment() {

    /** One tile: [icon] is a drawable or URL; tiles without an icon get a random color and the name. */
    private data class App(val name: String, val icon: Any? = null, val launch: () -> Unit)

    private var _binding: FragmentChannelBinding? = null
    private val binding get() = _binding!!
    private val http = OkHttpClient()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChannelBinding.inflate(inflater, container, false)
        binding.swiperefresh.setOnRefreshListener { load() }
        binding.btnConnect.setOnClickListener { SearchTVActivity.open(requireActivity()) }
        load()
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /** `q()`. */
    private fun load() {
        val b = _binding ?: return
        b.swiperefresh.isRefreshing = false
        if (!TvConnect.isConnected) {
            b.rcvListChannelLg.isVisible = false
            b.tvStatus.isVisible = true
            b.tvStatus.setText(R.string.can_channel_not_connect)
            return
        }
        b.tvStatus.isVisible = false
        b.rcvListChannelLg.isVisible = true
        when (TvConnect.device?.let(TvConnect::typeOf)) {
            DeviceType.ROKU -> async { rokuApps() }
            DeviceType.LG -> lgApps()
            DeviceType.SAMSUNG -> show(SAMSUNG_APPS.map { (id, icon, name) -> App(name, icon) { samsungLaunch(id) } }, grid = true)
            DeviceType.SONY -> async { sonyApps() }.also { b.rcvListChannelLg.layoutManager = LinearLayoutManager(requireContext()) }
            DeviceType.FIRE_TV -> async { fireTvApps() }
            else -> {
                b.tvStatus.setText(R.string.can_find_channel)
                b.tvStatus.isVisible = true
            }
        }
    }

    private fun async(fetch: suspend () -> List<App>) {
        binding.loading.isVisible = true
        viewLifecycleOwner.lifecycleScope.launch {
            val apps = runCatching { withContext(Dispatchers.IO) { fetch() } }.getOrDefault(emptyList())
            if (_binding == null) return@launch
            binding.loading.isVisible = false
            show(apps, grid = TvConnect.device?.let(TvConnect::typeOf) != DeviceType.SONY)
        }
    }

    private fun show(apps: List<App>, grid: Boolean) {
        val b = _binding ?: return
        b.loading.isVisible = false
        b.tvStatus.isVisible = apps.isEmpty()
        if (apps.isEmpty()) b.tvStatus.setText(R.string.can_find_channel)
        b.rcvListChannelLg.layoutManager = if (grid) GridLayoutManager(requireContext(), 3) else LinearLayoutManager(requireContext())
        b.rcvListChannelLg.adapter = AppAdapter(apps, TvConnect.device?.let(TvConnect::typeOf))
    }

    /** `query/apps` + `query/icon/<id>`. */
    private fun rokuApps(): List<App> {
        val ip = TvConnect.device?.ipAddress ?: return emptyList()
        val xml = http.newCall(Request.Builder().url("http://$ip:8060/query/apps").build()).execute().use { it.body?.string().orEmpty() }
        return Regex("""<app id="([^"]+)"[^>]*>([^<]*)</app>""").findAll(xml).map { m ->
            val id = m.groupValues[1]
            App(m.groupValues[2], "http://$ip:8060/query/icon/$id") { RokuEcp.launch(id) }
        }.toList().also { list -> TvConnect.rokuChannels.apply { clear(); addAll(list.map { it.icon.toString().substringAfterLast("/") }) } }
    }

    private fun lgApps() {
        val launcher = TvConnect.device?.getCapability(Launcher::class.java) ?: return show(emptyList(), true)
        binding.loading.isVisible = true
        launcher.getAppList(object : Launcher.AppListListener {
            override fun onSuccess(list: List<AppInfo>) {
                activity?.runOnUiThread { show(list.map { app -> App(app.name.orEmpty()) { launcher.launchAppWithInfo(app, null) } }, true) }
            }

            override fun onError(error: ServiceCommandError) {
                activity?.runOnUiThread { show(emptyList(), true) }
            }
        })
    }

    private fun samsungLaunch(appId: String) = SamsungRemote.send(
        JSONObject(
            """{"method":"ms.channel.emit","params":{"event":"ed.apps.launch","to":"host","data":{"appId":"$appId","action_type":"DEEP_LINK"}}}""",
        ),
    )

    /** `getApplicationList` on the Sony app-control service. */
    private fun sonyApps(): List<App> {
        val ip = TvConnect.device?.ipAddress ?: return emptyList()
        val body = """{"method":"getApplicationList","id":60,"params":[],"version":"1.0"}"""
        val json = http.newCall(
            Request.Builder().url("http://$ip/sony/appControl").header("X-Auth-PSK", "1234")
                .header("Cookie", com.casttotv.screen.mirroring.tv.SonyRemote.cookie)
                .post(body.toRequestBody("application/json".toMediaType())).build(),
        ).execute().use { JSONObject(it.body?.string().orEmpty()) }
        val list = json.optJSONArray("result")?.optJSONArray(0) ?: return emptyList()
        return (0 until list.length()).map { i ->
            val app = list.getJSONObject(i)
            App(app.optString("title"), app.optString("icon").takeIf { it.isNotEmpty() }) {
                com.casttotv.screen.mirroring.tv.SonyRemote.setActiveApp(app.optString("uri"))
            }
        }
    }

    /** Known Fire TV apps that `pm list packages` reports as installed (`p22`). */
    private suspend fun fireTvApps(): List<App> {
        val installed = withContext(Dispatchers.IO) { runCatching { FireTvAdb.listPackages() }.getOrDefault(emptySet()) }
        return FIRE_TV_APPS.filter { it.pkg in installed || installed.isEmpty() }
            .map { app -> App(app.name, app.icon) { FireTvAdb.shell("am start -n ${app.component}") } }
    }

    private inner class AppAdapter(private val apps: List<App>, private val type: DeviceType?) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val colors = listOf("#D10143", "#2196F3", "#2CB4F1", "#D91BFA", "#B40039", "#FFC107", "#FF9800", "#FF5722", "#2FF837", "#14D5ED", "#87EC13")

        override fun getItemCount() = apps.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = when (type) {
                DeviceType.SONY -> ItemChannelSonyBinding.inflate(inflater, parent, false).root
                DeviceType.FIRE_TV -> ItemChannelFireTvBinding.inflate(inflater, parent, false).root
                else -> ItemChannelLgBinding.inflate(inflater, parent, false).root
            }
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val app = apps[position]
            val view = holder.itemView
            when (type) {
                DeviceType.SONY -> with(ItemChannelSonyBinding.bind(view)) {
                    tvName.text = app.name
                    Glide.with(view).load(app.icon).into(iconLayout)
                }
                DeviceType.FIRE_TV -> with(ItemChannelFireTvBinding.bind(view)) {
                    tvName.text = app.name
                    Glide.with(view).load(app.icon).into(iconLayout)
                }
                else -> with(ItemChannelLgBinding.bind(view)) {
                    if (app.icon != null) {
                        iconColor.isVisible = false
                        iconLayout.isVisible = true
                        tvName.text = ""
                        Glide.with(view).load(app.icon).into(iconLayout)
                    } else {
                        iconLayout.isVisible = false
                        iconColor.isVisible = true
                        iconColor.setBackgroundColor(Color.parseColor(colors[Random.nextInt(colors.size)]))
                        tvName.text = app.name
                    }
                }
            }
            view.setOnClickListener { app.launch() }
        }
    }

    private data class FireTvApp(val name: String, val pkg: String, val icon: Int, val component: String)

    private companion object {
        /** `ge0.k()`: the Samsung tiles. */
        val SAMSUNG_APPS = listOf(
            Triple("3201907018807", R.drawable.netflix, "Netflix"), Triple("111299001912", R.drawable.youtube, "YouTube"),
            Triple("3201601007625", R.drawable.hulu, "Hulu"), Triple("3201910019365", R.drawable.primevideo, "Prime Video"),
            Triple("3201901017640", R.drawable.discovery, "Disney+"), Triple("3201807016597", R.drawable.appletv, "Apple TV"),
            Triple("3201907018784", R.drawable.internet, "Internet"), Triple("3201910019378", R.drawable.smart_thing, "SmartThings"),
            Triple("3201606009684", R.drawable.spotify, "Spotify"),
        )

        /** Names, packages, icons and launch components from `RemoteFireTVFragment`. */
        val FIRE_TV_APPS = listOf(
            FireTvApp("Netflix", "com.netflix.ninja", R.drawable.netflix, "com.netflix.ninja/.MainActivity"),
            FireTvApp("YouTube", "com.amazon.firetv.youtube", R.drawable.youtube, "com.amazon.firetv.youtube/dev.cobalt.app.MainActivity"),
            FireTvApp("Hulu", "com.hulu.plus", R.drawable.hulu, "com.hulu.plus/.SplashActivity"),
            FireTvApp("Disney+", "com.disney.disneyplus", R.drawable.disneyplus, "com.disney.disneyplus/com.bamtechmedia.dominguez.main.MainActivity"),
            FireTvApp("HBO Max", "com.hbo.hbonow", R.drawable.hbomax, "com.hbo.hbonow/com.hbo.go.LaunchActivity"),
            FireTvApp("Tubi - Watch Free Movies & TV Shows", "com.tubitv.ott", R.drawable.tubi, "com.tubitv.ott/com.tubitv.live.ChannelsSyncUpActivity"),
            FireTvApp("Downloader", "com.esaba.downloader", R.drawable.downloader, "com.esaba.downloader/.MainActivity"),
            FireTvApp("TikTok for TV", "com.tiktok.tv", R.drawable.tiktok_tv, "com.tiktok.tv/com.ss.android.ugc.aweme.tv.feed.MainTvActivity"),
            FireTvApp("Peacock TV", "com.peacock.peacockfiretv", R.drawable.peacock, "com.peacock.peacockfiretv/com.peacock.peacocktv.AmazonMainActivity"),
            FireTvApp("Sling TV", "com.sling", R.drawable.sling_tv, "com.sling/.livetv.LiveTvSyncActivity"),
            FireTvApp("STARZ", "com.starz.starzplay.firetv", R.drawable.starz, "com.starz.starzplay.firetv/com.starz.amznfiretv.SplashActivity"),
            FireTvApp("Pluto TV", "tv.pluto.android", R.drawable.plutotv, "tv.pluto.android/.EntryPoint"),
            FireTvApp("CBS News", "com.cbs.ott", R.drawable.cbs_news, "com.cbsnews.ott/.controllers.activities.MainActivity"),
            FireTvApp("Plex", "com.plexapp.android", R.drawable.plex, "com.plexapp.android/com.plexapp.plex.activities.SplashActivity"),
            FireTvApp("Spotify - Music and Podcasts", "com.spotify.tv.android", R.drawable.spotify, "com.spotify.tv.android/.SpotifyTVActivity"),
            FireTvApp("Philo: Live & On-Demand TV", "com.philo.philo", R.drawable.philo, "com.philo.philo/.tif.TvSetupActivity"),
            FireTvApp("Hotstar", "in.startv.hotstar", R.drawable.hotstar, "in.startv.hotstar/.ui.splash.TVSplashActivity"),
            FireTvApp("Apple TV", "com.apple.atve.amazon.appletv", R.drawable.appletv, "com.apple.atve.amazon.appletv/.MainActivity"),
            FireTvApp("ESPN for Fire TV", "com.espn.gtv", R.drawable.espn, "com.espn.gtv/com.espn.androidtv.ui.LoadingActivity"),
            FireTvApp("Amazon Silk - Web Browser", "com.amazon.cloud9", R.drawable.amazon_silk, "com.amazon.cloud9/.browsing.BrowserActivity"),
            FireTvApp("YouTube TV", "com.amazon.firetv.youtube.tv", R.drawable.youtube_tv, "com.amazon.firetv.youtube.tv/dev.cobalt.app.MainActivity"),
            FireTvApp("Prime Video", "com.amazon.firebat", R.drawable.primevideo, "com.amazon.firebat/.deeplink.DeepLinkRoutingActivity"),
            FireTvApp("Crunchyroll", "com.crunchyroll.crunchyroid", R.drawable.crunchyroll, "com.crunchyroll.crunchyroid/.MainActivity"),
            FireTvApp("Fawesome - Free Awesome TV & Movies", "com.future.moviesByFawesomeAndroidTV", R.drawable.fawsome, "com.future.moviesByFawesomeAndroidTV/.SplashActivity"),
            FireTvApp("Lionsgate Play", "com.lionsgateplay.videoapp", R.drawable.lionsgate, "com.lionsgateplay.videoapp/com.parsifal.starz.ui.features.splash.SplashActivity"),
            FireTvApp("SonyLIV", "com.onemainstream.sonyliv.android", R.drawable.sonyliv, "com.onemainstream.sonyliv.android/com.sonyliv.ui.splash.SplashActivity"),
            FireTvApp("NBA on Fire TV", "com.nbaimd.gametime.nba2011.amazon", R.drawable.nba, "com.nbaimd.gametime.nba2011.amazon/com.neulion.MainActivity"),
            FireTvApp("MX Player", "com.mxtech.videoplayer.ad", R.drawable.mxplayer, "com.mxtech.videoplayer.ad/.ActivityWelcomeMX"),
            FireTvApp("Discovery Plus", "com.discoveryplus.tv.fire", R.drawable.discovery, "com.discoveryplus.tv.fire/.DPlusFireTvActivity"),
            FireTvApp("ALTBalaji", "com.balaji.alt", R.drawable.altbalaji, "com.balaji.alt/com.balaji.play.tv.views.GhostActivity"),
            FireTvApp("Red Bull TV", "com.redbull.rbtv", R.drawable.redbull, "com.redbull.rbtv/com.redbull.launch.SplashActivity"),
            FireTvApp("ZEE5", "com.zee5.amazon", R.drawable.zee, "com.zee5.amazon/com.zee5.live.rich.RichTvInputSetupActivity"),
            FireTvApp("HappyKids", "com.future.HappyKids", R.drawable.happykids, "com.future.HappyKids/.SplashActivity"),
            FireTvApp("Twitch", "tv.twitch.android.viewer", R.drawable.twitch, "tv.twitch.android.viewer/tv.twitch.android.apps.TwitchActivity"),
            FireTvApp("BEE TV Network", "com.amazon.rialto.cordova.webapp.webappe9d3fd9a69254a278fefa093292e194f", R.drawable.beetv, "com.amazon.rialto.cordova.webapp.webappe9d3fd9a69254a278fefa093292e194f/.MainActivity"),
            FireTvApp("JioCinema: Movies TV Originals", "com.jio.media.stb.ondemand", R.drawable.jiocinema, "com.jio.media.stb.ondemand/com.jio.media.stb.jioondemand.ui.splash.SplashActivity"),
            FireTvApp("Internet Speed Test App", "com.rma.netpulsetv", R.drawable.internetspeedtest, "com.rma.netpulsetv/.ui.SplashActivity"),
            FireTvApp("Iron Dragon TV", "com.irondragonproductions.idtvflix", R.drawable.irondragontv, "com.irondragonproductions.idtvflix/com.irondragonproductions.irondragontv.LaunchActivity"),
            FireTvApp("PAC-MAN 256 - Endless Arcade Maze", "eu.bandainamcoent.pacman256", R.drawable.pacman, "eu.bandainamcoent.pacman256/com.unity3d.player.UnityPlayerNativeActivity"),
            FireTvApp("Free Movies Plus", "com.amazon.rialto.cordova.webapp.webapp490adaf842ab464a8f4f19c7a054d17c", R.drawable.freemoviepluus, "com.amazon.rialto.cordova.webapp.webapp490adaf842ab464a8f4f19c7a054d17c/.MainActivity"),
        )
    }
}

package com.casttotv.screen.mirroring.ui.activities.function.play

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityCastBinding
import com.casttotv.screen.mirroring.databinding.ItemImageBinding
import com.casttotv.screen.mirroring.databinding.ItemSlideImageBinding
import com.casttotv.screen.mirroring.databinding.ItemVideoDragBinding
import com.casttotv.screen.mirroring.models.CastSession
import com.casttotv.screen.mirroring.models.CastType
import com.casttotv.screen.mirroring.models.MediaItem
import com.casttotv.screen.mirroring.tv.DeviceType
import com.casttotv.screen.mirroring.tv.FileServer
import com.casttotv.screen.mirroring.tv.RokuEcp
import com.casttotv.screen.mirroring.tv.TvConnect
import com.casttotv.screen.mirroring.tv.WebReceiver
import com.connectsdk.core.MediaInfo
import com.connectsdk.service.capability.MediaControl
import com.connectsdk.service.capability.MediaPlayer
import com.connectsdk.service.capability.VolumeControl
import com.connectsdk.service.command.ServiceCommandError
import com.connectsdk.service.sessions.LaunchSession
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

/** Port of `CastMediaActivity`: sends one item of [CastSession] to the TV and remote-controls playback. */
class CastMediaActivity : BaseActivity() {

    private lateinit var binding: ActivityCastBinding
    private lateinit var castingDialog: Dialog
    private val type = CastSession.type
    private var items: MutableList<MediaItem> = CastSession.items.toMutableList()
    private var position = CastSession.position

    private var launchSession: LaunchSession? = null
    private var mediaControl: MediaControl? = null
    private var playing = false
    private var muted = false
    private var rotation = 0f
    private var progressTimer: Timer? = null
    private var slideshow: CountDownTimer? = null
    private var thumbAdapter: ThumbAdapter? = null

    private val player: MediaPlayer? get() = TvConnect.castDevice?.getCapability(MediaPlayer::class.java)
    private val isRoku get() = TvConnect.castDevice?.let(TvConnect::typeOf) == DeviceType.ROKU

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCastBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this) { finish() }

        castingDialog = Dialog(this).apply {
            setContentView(R.layout.dialog_casting)
            findViewById<View>(R.id.tv_help)?.setOnClickListener {
                // `s3` case 3.
                startActivity(android.content.Intent(this@CastMediaActivity, com.casttotv.screen.mirroring.ui.activities.support.HelpActivity::class.java))
            }
            show()
        }
        setupHeader()
        setupControls()
        if (type.isPhoto) setupPhotoMode() else setupVideoMode()
        castCurrent()
    }

    override fun onResume() {
        super.onResume()
        if (playing) startProgressTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressTimer?.cancel()
        slideshow?.cancel()
        launchSession?.close(null)
        FileServer.stop()
    }

    private fun setupHeader() = bindHeader(
        binding.include4,
        when (type) {
            CastType.PHOTO_OFFLINE -> "Cast Photo"
            CastType.PHOTO_ONLINE -> "Photo Online"
            CastType.PHOTO_BROWSER -> "Photo Browser"
            else -> "Cast Video"
        },
    )

    // region photo mode

    private fun setupPhotoMode() = with(binding) {
        slPhoto.visibility = View.VISIBLE
        slVideo.visibility = View.GONE
        val thumbs = ThumbAdapter(items) { vpImage.setCurrentItem(it, true) }
        thumbAdapter = thumbs
        val layoutManager = LinearLayoutManager(this@CastMediaActivity, LinearLayoutManager.HORIZONTAL, false)
        rcvImage.layoutManager = layoutManager
        rcvImage.adapter = thumbs
        vpImage.adapter = SlideAdapter(items)
        vpImage.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(page: Int) {
                thumbs.select(page)
                layoutManager.scrollToPosition(page)
                position = page
                castCurrent()
            }
        })
        thumbs.select(position)
        vpImage.setCurrentItem(position, false)
        rlBack.setOnClickListener { step(-1) }
        rlNext.setOnClickListener { step(1) }
        rlStopImage.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        rlRote.setOnClickListener { rotateCurrent() }
        rltPlayPauseMediaPlayer.setOnClickListener { toggleSlideshow() }
    }

    /** `sb0`: 8 s countdown in `tv_time`, then the next photo, repeating. */
    private fun toggleSlideshow() = with(binding) {
        if (slideshow != null) {
            tvTime.visibility = View.GONE
            imgPlayPauseMediaPlayer.setImageResource(R.drawable.ic_play_media_player)
            slideshow?.cancel()
            slideshow = null
        } else {
            tvTime.visibility = View.VISIBLE
            imgPlayPauseMediaPlayer.setImageResource(R.drawable.ic_pause_media_player)
            slideshow = object : CountDownTimer(8000, 1000) {
                override fun onTick(left: Long) {
                    tvTime.text = (left / 1000).toString()
                }

                override fun onFinish() {
                    step(1)
                    start()
                }
            }.start()
        }
    }

    private fun rotateCurrent() {
        rotation = if (rotation == 270f) 0f else rotation + 90f
        val item = items.getOrNull(position) ?: return
        lifecycleScope.launch {
            val rotated = withContext(Dispatchers.IO) { CastImages.rotate(this@CastMediaActivity, item.path, rotation) }
            castPhoto(rotated ?: item.path, item.title + rotation)
        }
    }

    // endregion

    // region video mode

    private fun setupVideoMode() = with(binding) {
        slPhoto.visibility = View.GONE
        slVideo.visibility = View.VISIBLE
        seekbarTime.isEnabled = TvConnect.isConnectWeb || TvConnect.castDevice?.hasCapability(MediaControl.Seek) == true
        seekbarTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = Unit
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (TvConnect.isConnectWeb) WebReceiver.send(7) { put("position", seekBar.progress / 1000) }
                mediaControl?.seek(seekBar.progress.toLong(), null)
            }
        })
        rlBackVideo.setOnClickListener { step(-1) }
        rlNextVideo.setOnClickListener { step(1) }
        rlPauseVideo.setOnClickListener { togglePlay() }
        rltPlayPauseMediaPlayer.setOnClickListener { togglePlay() }
        rltNextMediaPlayer.setOnClickListener { seekBy(true) }
        rltPrevMediaPlayer.setOnClickListener { seekBy(false) }
        llStopVideo.setOnClickListener {
            if (TvConnect.isConnectWeb) WebReceiver.send(3)
            onBackPressedDispatcher.onBackPressed()
        }
        llShowList.setOnClickListener { if (type == CastType.VIDEO_OFFLINE) showPlaylist() else finish() }
        imvRemote.setOnClickListener { open(com.casttotv.screen.mirroring.ui.activities.function.remote.RemoteActivity::class.java) }
    }

    private fun setupControls() = with(binding) {
        rltVolumeUpMediaPlayer.setOnClickListener { changeVolume(true) }
        rltVolumeDownMediaPlayer.setOnClickListener { changeVolume(false) }
        llMute.setOnClickListener { toggleMute() }
    }

    private fun togglePlay() {
        when {
            TvConnect.isConnectWeb -> {
                WebReceiver.send(if (playing) 2 else 1)
                setPlayIcons(!playing)
                playing = !playing
            }
            isRoku -> {
                setPlayIcons(!playing)
                RokuEcp.keypress(RokuEcp.Key.PLAY)
                playing = !playing
            }
            mediaControl == null -> Toast.makeText(this, R.string.not_support, Toast.LENGTH_SHORT).show()
            else -> {
                if (playing) mediaControl?.pause(null) else mediaControl?.play(null)
                setPlayIcons(!playing)
                playing = !playing
            }
        }
    }

    private fun setPlayIcons(playing: Boolean) {
        val icon = if (playing) R.drawable.ic_pause_media_player else R.drawable.ic_play_media_player
        binding.imgPlayPauseMediaPlayer.setImageResource(icon)
        binding.imvPauseVideo.setImageResource(icon)
    }

    /** `F(boolean)`: ±15 s; Roku uses Left/Right. */
    private fun seekBy(forward: Boolean) {
        if (TvConnect.isConnectWeb) return WebReceiver.send(if (forward) 5 else 6) { put("space", 10000) }
        if (isRoku) {
            RokuEcp.keypress(if (forward) RokuEcp.Key.RIGHT else RokuEcp.Key.LEFT)
            return
        }
        val control = mediaControl ?: return Toast.makeText(this, R.string.not_support, Toast.LENGTH_SHORT).show()
        val target = (binding.seekbarTime.progress + if (forward) 15000 else -15000).coerceAtLeast(0)
        control.seek(target.toLong(), null)
    }

    /** `G(boolean)`: volume steps of 1%; Roku uses VolumeUp/Down keys. */
    private fun changeVolume(up: Boolean) {
        if (!TvConnect.isConnected) return
        if (TvConnect.isConnectWeb) return WebReceiver.send(if (up) 12 else 13) { put("volume", 10) }
        if (isRoku) {
            RokuEcp.keypress(if (up) RokuEcp.Key.VOLUME_UP else RokuEcp.Key.VOLUME_DOWN)
            return
        }
        val volume = TvConnect.castDevice?.getCapability(VolumeControl::class.java) ?: return
        TvConnect.volume = if (up) minOf(TvConnect.volume + 0.01f, 1f) else maxOf(TvConnect.volume - 0.01f, 0f)
        volume.setVolume(TvConnect.volume, null)
    }

    private fun toggleMute() {
        muted = !muted
        if (TvConnect.isConnectWeb) WebReceiver.send(4) { put("volume", 0) }
        else if (isRoku) RokuEcp.keypress(RokuEcp.Key.VOLUME_MUTE)
        else TvConnect.castDevice?.getCapability(VolumeControl::class.java)?.setMute(muted, null)
    }

    /** Bottom sheet with the video queue: tap to cast, drag to reorder, X to remove (`ym1`). */
    @SuppressLint("NotifyDataSetChanged")
    private fun showPlaylist() {
        val sheet = BottomSheetDialog(this)
        sheet.setContentView(R.layout.bottom_sheet_layout)
        sheet.setOnShowListener {
            sheet.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(android.R.color.transparent)
        }
        val list = sheet.findViewById<RecyclerView>(R.id.rcv_quickly_list) ?: return
        val currentTitle = items.getOrNull(position)?.title
        lateinit var adapter: PlaylistAdapter
        adapter = PlaylistAdapter(items, { position }, onPlay = { index ->
            position = index
            castCurrent()
            sheet.dismiss()
        }, onRemove = { index ->
            items.removeAt(index)
            position = items.indexOfFirst { it.title == currentTitle }.coerceAtLeast(0)
            adapter.notifyDataSetChanged()
        })
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                items.add(to.bindingAdapterPosition, items.removeAt(from.bindingAdapterPosition))
                adapter.notifyItemMoved(from.bindingAdapterPosition, to.bindingAdapterPosition)
                position = items.indexOfFirst { it.title == currentTitle }.coerceAtLeast(0)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        }).attachToRecyclerView(list)
        if (position < items.size) list.scrollToPosition(position)
        sheet.show()
    }

    // endregion

    /** Previous/next item with wrap-around (`lb0` cases 1/2). Photos move the pager, which casts. */
    private fun step(delta: Int) {
        if (items.isEmpty()) return
        if (type == CastType.YOUTUBE_ONLINE || type == CastType.VIDEO_BROWSER) {
            Toast.makeText(this, "Not support", Toast.LENGTH_SHORT).show()
            return
        }
        position = (position + delta).mod(items.size)
        if (type.isPhoto) binding.vpImage.setCurrentItem(position, true) else castCurrent()
    }

    /** `A()` + `v()`: show the item and hand it to the TV. */
    private fun castCurrent() {
        val item = items.getOrNull(position) ?: return
        CastSession.position = position
        CastSession.title = item.title
        CastSession.path = item.path
        when (type) {
            CastType.PHOTO_OFFLINE -> lifecycleScope.launch {
                val prepared = withContext(Dispatchers.IO) { CastImages.prepare(this@CastMediaActivity, item.path) }
                castPhoto(prepared, item.title)
            }
            CastType.PHOTO_ONLINE, CastType.PHOTO_BROWSER, CastType.IMAGE_DRIVER -> castPhoto(item.path, item.title)
            CastType.AUDIO_OFFLINE -> castMedia(item, "audio/mp3")
            CastType.IPTV -> castMedia(item, "application/x-mpegurl")
            else -> {
                Glide.with(this).load(item.thumb).placeholder(R.drawable.ic_video_default).into(binding.imageView)
                binding.tvNameVideo.text = item.title
                castMedia(item, "video/mp4")
            }
        }
    }

    private fun castPhoto(path: String, title: String) {
        val url = FileServer.urlFor(this, path)
        if (isRoku) {
            RokuEcp.castMedia(url, video = false) { runOnUiThread { onCastSuccess() } }
            return
        }
        val player = player ?: return notConnected()
        player.displayImage(MediaInfo.Builder(url, "image/jpeg").setTitle(title).setDescription(url).build(), launchListener(isVideo = false))
    }

    private fun castMedia(item: MediaItem, mime: String) {
        val url = FileServer.urlFor(this, item.path)
        // `v()`: only video goes to a browser receiver.
        if (TvConnect.isConnectWeb && mime == "video/mp4") {
            WebReceiver.load(url)
            playing = true
            setPlayIcons(true)
            dismissCasting()
            return
        }
        if (isRoku && mime == "video/mp4") {
            RokuEcp.castMedia(url, video = true) { code ->
                runOnUiThread {
                    playing = code == 200
                    dismissCasting()
                }
            }
            return
        }
        val player = player ?: return notConnected()
        player.playMedia(MediaInfo.Builder(url, mime).setTitle(item.title).setDescription(url).build(), true, launchListener(isVideo = true))
    }

    private fun notConnected() {
        dismissCasting()
        Toast.makeText(this, "not connect smart", Toast.LENGTH_SHORT).show()
    }

    private fun launchListener(isVideo: Boolean) = object : MediaPlayer.LaunchListener {
        override fun onSuccess(result: MediaPlayer.MediaLaunchObject) {
            launchSession = result.launchSession
            if (!isVideo) {
                runOnUiThread(::onCastSuccess)
                return
            }
            runOnUiThread {
                dismissCasting()
                playing = true
                setPlayIcons(true)
            }
            mediaControl = result.mediaControl
            TvConnect.mediaControl = mediaControl
            progressTimer?.cancel()
            val device = TvConnect.castDevice
            if (device?.hasCapability(MediaControl.PlayState_Subscribe) == true) {
                mediaControl?.subscribePlayState(playStateListener)
            } else {
                mediaControl?.getDuration(durationListener)
                startProgressTimer()
            }
        }

        override fun onError(error: ServiceCommandError) {
            runOnUiThread { dismissCasting() }
        }
    }

    private fun onCastSuccess() = dismissCasting()

    private fun dismissCasting() {
        if (!isFinishing && castingDialog.isShowing) castingDialog.dismiss()
    }

    // region progress

    private val positionListener = object : MediaControl.PositionListener {
        override fun onSuccess(value: Long) = runOnUiThread {
            binding.textviewTime.text = formatTime(value)
            binding.seekbarTime.progress = value.toInt()
        }

        override fun onError(error: ServiceCommandError) = Unit
    }

    private val durationListener = object : MediaControl.DurationListener {
        override fun onSuccess(value: Long) = runOnUiThread {
            binding.textviewDuration.text = formatTime(value)
            binding.seekbarTime.max = value.toInt()
        }

        override fun onError(error: ServiceCommandError) = Unit
    }

    private val playStateListener = object : MediaControl.PlayStateListener {
        override fun onSuccess(state: MediaControl.PlayStateStatus) = runOnUiThread {
            when (state) {
                MediaControl.PlayStateStatus.Playing -> {
                    startProgressTimer()
                    if (TvConnect.castDevice?.hasCapability(MediaControl.Duration) == true) mediaControl?.getDuration(durationListener)
                    return@runOnUiThread
                }
                MediaControl.PlayStateStatus.Finished -> {
                    binding.textviewTime.text = "--:--"
                    binding.textviewDuration.text = "--:--"
                    binding.seekbarTime.progress = 0
                }
                else -> Unit
            }
            progressTimer?.cancel()
            progressTimer = null
        }

        override fun onError(error: ServiceCommandError) = Unit
    }

    /** `H()`: poll position and duration every second while playing. */
    private fun startProgressTimer() {
        progressTimer?.cancel()
        progressTimer = fixedRateTimer(period = 1000) {
            val device = TvConnect.castDevice ?: return@fixedRateTimer
            if (device.hasCapability(MediaControl.Position)) mediaControl?.getPosition(positionListener)
            if (device.hasCapability(MediaControl.Duration)) mediaControl?.getDuration(durationListener)
        }
    }

    private fun formatTime(ms: Long): String {
        val total = (ms / 1000).toInt()
        val h = total / 3600
        val m = total % 3600 / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
    }

    // endregion
}

/** `av2`: horizontal photo strip; the selected photo gets the main-color border. */
private class ThumbAdapter(private val items: List<MediaItem>, private val onClick: (Int) -> Unit) :
    RecyclerView.Adapter<ThumbAdapter.Holder>() {
    class Holder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("NotifyDataSetChanged")
    fun select(index: Int) {
        items.forEachIndexed { i, item -> item.isSelected = i == index }
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val context = holder.itemView.context
        holder.binding.imv.setBorderColor(
            ContextCompat.getColor(context, if (items[position].isSelected) R.color.color_main else R.color.transparent),
        )
        Glide.with(holder.itemView).load(items[position].path).into(holder.binding.imv)
        holder.itemView.setOnClickListener { onClick(holder.bindingAdapterPosition) }
    }
}

/** `em4`: big preview pager. */
private class SlideAdapter(private val items: List<MediaItem>) : PagerAdapter() {
    override fun getCount() = items.size
    override fun isViewFromObject(view: View, obj: Any) = view === obj

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val page = ItemSlideImageBinding.inflate(LayoutInflater.from(container.context), container, false)
        Glide.with(container).load(items[position].path).placeholder(R.drawable.ic_icon_default_tv).into(page.imageView)
        container.addView(page.root)
        return page.root
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) = container.removeView(obj as View)
}

/** `ym1`: video queue rows. */
private class PlaylistAdapter(
    private val items: List<MediaItem>,
    private val current: () -> Int,
    private val onPlay: (Int) -> Unit,
    private val onRemove: (Int) -> Unit,
) : RecyclerView.Adapter<PlaylistAdapter.Holder>() {
    class Holder(val binding: ItemVideoDragBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemVideoDragBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val context = holder.itemView.context
        with(holder.binding) {
            Glide.with(root).load(items[position].path).into(imgVideoOffline)
            tvName.text = items[position].title
            if (position == current()) rlItem.background = ContextCompat.getDrawable(context, R.drawable.bg_item_selected)
            else rlItem.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            imvClose.setOnClickListener { onRemove(holder.bindingAdapterPosition) }
            root.setOnClickListener { onPlay(holder.bindingAdapterPosition) }
        }
    }
}

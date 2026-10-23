package com.casttotv.screen.mirroring.ui.activities.function.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.MediaStoreRepo
import com.casttotv.screen.mirroring.databinding.ActivityListMediaBinding
import com.casttotv.screen.mirroring.databinding.FragmentMediaAllBinding
import com.casttotv.screen.mirroring.databinding.ItemAlbumPhotoBinding
import com.casttotv.screen.mirroring.databinding.LayoutItemVideoBinding
import com.casttotv.screen.mirroring.models.CastSession
import com.casttotv.screen.mirroring.models.CastType
import com.casttotv.screen.mirroring.models.MediaItem
import com.casttotv.screen.mirroring.models.PhotoAlbum
import com.casttotv.screen.mirroring.ui.activities.function.play.CastMediaActivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Port of `MediaActivity` for `media_type=Video`: "All video" and "Album video" tabs. */
class MediaActivity : BaseActivity() {

    private lateinit var binding: ActivityListMediaBinding

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindHeader(binding.include2, intent.getStringExtra(EXTRA_MEDIA_TYPE) ?: "Video")

        val tabs = listOf(getString(R.string.txt_all_video), getString(R.string.txt_album_video))
        binding.viewPager.adapter = TabsAdapter()
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, i -> tab.text = tabs[i] }.attach()
    }

    /** `um3`/`em3`: tapping a video casts the whole list starting there, connecting first if needed. */
    private fun castVideos(videos: List<MediaItem>, position: Int) = withConnection {
        CastSession.start(CastType.VIDEO_OFFLINE, videos, position)
        open(CastMediaActivity::class.java)
    }

    /** Two pages built from `fragment_media_all`: page 0 lists videos, page 1 lists albums (`sq3`). */
    private inner class TabsAdapter : RecyclerView.Adapter<TabsAdapter.Page>() {
        inner class Page(val binding: FragmentMediaAllBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemCount() = 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Page(FragmentMediaAllBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(page: Page, position: Int): Unit = with(page.binding) {
            pgLoading.isVisible = true
            rcvListMedia.layoutManager = GridLayoutManager(this@MediaActivity, 3)
            lifecycleScope.launch {
                if (position == 0) {
                    val videos = withContext(Dispatchers.IO) { MediaStoreRepo.allVideos(this@MediaActivity) }
                    clNoVideo.isVisible = videos.isEmpty()
                    rcvListMedia.adapter = VideoGridAdapter(videos, ::castVideos)
                } else {
                    val albums = withContext(Dispatchers.IO) { MediaStoreRepo.videoAlbums(this@MediaActivity) }
                    rcvListMedia.adapter = AlbumGridAdapter(albums) { album ->
                        DetailMediaActivity.album = album
                        open(DetailMediaActivity::class.java)
                    }
                }
                pgLoading.isVisible = false
            }
            Unit
        }
    }

    companion object {
        const val EXTRA_MEDIA_TYPE = "media_type"
    }
}

/** `rd0` mode 4 (`layout_item_video`), without the ad cells. */
class VideoGridAdapter(
    private val videos: List<MediaItem>,
    private val onClick: (List<MediaItem>, Int) -> Unit,
) : RecyclerView.Adapter<VideoGridAdapter.Holder>() {
    class Holder(val binding: LayoutItemVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = videos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int): Unit = with(holder.binding) {
        val video = videos[position]
        Glide.with(root).load(video.path).into(imgVideoOffline)
        tvName.text = video.title
        tvTime.text = formatDuration(video.durationMs)
        root.setOnClickListener { onClick(videos, holder.bindingAdapterPosition) }
    }
}

/** `ud0` mode 5 (`item_album_photo`): cover, name and item count. */
class AlbumGridAdapter(
    private val albums: List<PhotoAlbum>,
    private val onClick: (PhotoAlbum) -> Unit,
) : RecyclerView.Adapter<AlbumGridAdapter.Holder>() {
    class Holder(val binding: ItemAlbumPhotoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = albums.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAlbumPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int): Unit = with(holder.binding) {
        val album = albums[position]
        tvName.text = album.name
        tvSize.text = album.photos.size.toString()
        Glide.with(root).load(album.cover).into(imgPhoto)
        root.setOnClickListener { onClick(album) }
    }
}

/** `ak0.q`: m:ss or h:mm:ss. */
fun formatDuration(ms: Long): String {
    val total = (ms / 1000).toInt()
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

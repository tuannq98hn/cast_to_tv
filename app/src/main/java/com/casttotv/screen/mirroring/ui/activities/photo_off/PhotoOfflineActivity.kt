package com.casttotv.screen.mirroring.ui.activities.photo_off

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.MediaStoreRepo
import com.casttotv.screen.mirroring.databinding.ActivityPhotoOfflineBinding
import com.casttotv.screen.mirroring.databinding.FragmentMediaBinding
import com.casttotv.screen.mirroring.databinding.ItemAlbumPhoto2Binding
import com.casttotv.screen.mirroring.databinding.LayoutItemPhotosBinding
import com.casttotv.screen.mirroring.models.CastSession
import com.casttotv.screen.mirroring.models.CastType
import com.casttotv.screen.mirroring.models.MediaItem
import com.casttotv.screen.mirroring.models.PhotoAlbum
import com.casttotv.screen.mirroring.ui.activities.function.play.CastMediaActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Port of `PhotoOfflineActivity`: album chips on top, one photo grid per album in a pager. */
class PhotoOfflineActivity : BaseActivity() {

    private lateinit var binding: ActivityPhotoOfflineBinding
    private val albumAdapter = AlbumAdapter { position -> binding.vpgMedia.currentItem = position }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoOfflineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindMediaHeader(binding.header, getString(R.string.txt_photo))
        binding.rcvListAlbum.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvListAlbum.adapter = albumAdapter
        binding.vpgMedia.offscreenPageLimit = 3
        binding.vpgMedia.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                albumAdapter.select(position)
                binding.rcvListAlbum.smoothScrollToPosition(position)
            }
        })

        lifecycleScope.launch {
            val albums = withContext(Dispatchers.IO) { MediaStoreRepo.photoAlbums(this@PhotoOfflineActivity) }
            binding.clNoPhoto.isVisible = albums.isEmpty()
            albumAdapter.submit(albums)
            albumAdapter.select(0)
            binding.vpgMedia.adapter = AlbumPagerAdapter(albums)
            binding.progressBar.visibility = View.GONE
        }
    }

    /** `om3` / `mr4.a`: tapping a photo casts it, connecting first when needed. */
    private fun onPhotoClicked(photos: List<MediaItem>, position: Int) = withConnection {
        CastSession.start(CastType.PHOTO_OFFLINE, photos, position)
        open(CastMediaActivity::class.java)
    }

    private inner class AlbumPagerAdapter(private val albums: List<PhotoAlbum>) : PagerAdapter() {
        override fun getCount() = albums.size
        override fun isViewFromObject(view: View, obj: Any) = view === obj

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val page = FragmentMediaBinding.inflate(LayoutInflater.from(container.context), container, false)
            page.rcvList.layoutManager = GridLayoutManager(container.context, 2)
            page.rcvList.adapter = PhotoGridAdapter(albums[position].photos, ::onPhotoClicked)
            container.addView(page.root)
            return page.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) = container.removeView(obj as View)
    }
}

/** `ud0` mode 6: album name chips. */
private class AlbumAdapter(private val onClick: (Int) -> Unit) : RecyclerView.Adapter<AlbumAdapter.Holder>() {
    private val albums = mutableListOf<PhotoAlbum>()

    class Holder(val binding: ItemAlbumPhoto2Binding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("NotifyDataSetChanged")
    fun submit(list: List<PhotoAlbum>) {
        albums.clear()
        albums += list
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun select(position: Int) {
        albums.forEachIndexed { i, a -> a.isSelected = i == position }
        notifyDataSetChanged()
    }

    override fun getItemCount() = albums.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemAlbumPhoto2Binding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val album = albums[position]
        val context = holder.itemView.context
        with(holder.binding) {
            tvNameAlbum.text = album.name
            llBg.background = ContextCompat.getDrawable(
                context, if (album.isSelected) R.drawable.dc_bg_chip_active else R.drawable.dc_bg_chip,
            )
            tvNameAlbum.setTextColor(ContextCompat.getColor(context, if (album.isSelected) R.color.dc_text_primary else R.color.dc_text_secondary))
            tvCount.isVisible = album.isSelected
            tvCount.text = String.format(java.util.Locale.getDefault(), "%,d", album.photos.size)
            root.setOnClickListener { onClick(holder.bindingAdapterPosition) }
        }
    }
}

/** `qm3`: 2-column photo grid (the ad cell at index 6 is dropped with the ads). */
private class PhotoGridAdapter(
    private val photos: List<MediaItem>,
    private val onClick: (List<MediaItem>, Int) -> Unit,
) : RecyclerView.Adapter<PhotoGridAdapter.Holder>() {

    class Holder(val binding: LayoutItemPhotosBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = photos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutItemPhotosBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Glide.with(holder.itemView).load(photos[position].path).placeholder(R.drawable.ic_image_default).centerCrop()
            .into(holder.binding.imgPhoto)
        holder.binding.tvPhotoName.text = photos[position].title.ifEmpty { java.io.File(photos[position].path).nameWithoutExtension }
        holder.itemView.setOnClickListener { onClick(photos, holder.bindingAdapterPosition) }
    }
}

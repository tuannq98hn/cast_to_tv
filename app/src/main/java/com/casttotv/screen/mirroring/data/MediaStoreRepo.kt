package com.casttotv.screen.mirroring.data

import android.content.Context
import android.provider.MediaStore
import com.casttotv.screen.mirroring.models.MediaItem
import com.casttotv.screen.mirroring.models.PhotoAlbum

/** MediaStore queries used by the offline cast screens (`am4.l` + `jr4`). */
object MediaStoreRepo {

    /** Every image, newest first. */
    fun allPhotos(context: Context): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.TITLE, MediaStore.Images.Media.DATA),
            null, null, "${MediaStore.Images.Media.DATE_TAKEN} DESC",
        )?.use { c ->
            val title = c.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE)
            val data = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (c.moveToNext()) {
                val path = c.getString(data) ?: continue
                items += MediaItem(path, c.getString(title).orEmpty())
            }
        }
        return items
    }

    /** "All Photo" followed by one album per bucket, in MediaStore order. */
    fun photoAlbums(context: Context): List<PhotoAlbum> {
        val buckets = LinkedHashMap<String, MutableList<MediaItem>>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME, MediaStore.Images.Media.DATA),
            null, null, null,
        )?.use { c ->
            val bucket = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val data = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (c.moveToNext()) {
                val path = c.getString(data) ?: continue
                buckets.getOrPut(c.getString(bucket).orEmpty()) { mutableListOf() } += MediaItem(path, "")
            }
        }
        if (buckets.isEmpty()) return emptyList()
        val all = allPhotos(context)
        return listOf(PhotoAlbum("All Photo", all.firstOrNull()?.path.orEmpty(), all)) +
            buckets.map { (name, photos) -> PhotoAlbum(name, photos.first().path, photos) }
    }

    /** Every video, newest first (`MediaActivity`, media_type=Video). */
    fun allVideos(context: Context): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DURATION),
            null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val title = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val data = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val duration = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                val path = c.getString(data) ?: continue
                items += MediaItem(path, c.getString(title).orEmpty(), durationMs = c.getLong(duration))
            }
        }
        return items
    }

    /** Videos grouped by MediaStore bucket (`ak0.G`), for the "Album video" tab. */
    fun videoAlbums(context: Context): List<PhotoAlbum> {
        val buckets = LinkedHashMap<String, MutableList<MediaItem>>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media.DATA, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DURATION),
            null, null, null,
        )?.use { c ->
            val bucket = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val data = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val title = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val duration = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                val path = c.getString(data) ?: continue
                buckets.getOrPut(c.getString(bucket).orEmpty()) { mutableListOf() } +=
                    MediaItem(path, c.getString(title).orEmpty(), durationMs = c.getLong(duration))
            }
        }
        return buckets.map { (name, videos) -> PhotoAlbum(name, videos.first().path, videos) }
    }
}

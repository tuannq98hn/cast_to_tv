package com.casttotv.screen.mirroring.models

/** A local media file (`MediaModel`/`MediaObject` in the original, which carried the same fields twice). */
data class MediaItem(
    val path: String,
    val title: String,
    val thumb: String = path,
    val durationMs: Long = 0,
) {
    var isSelected = false
}

/** `PhotoAlbum`: a MediaStore bucket; the first album is the synthetic "All Photo". */
data class PhotoAlbum(val name: String, val cover: String, val photos: List<MediaItem>) {
    var isSelected = false
}

/** What `CastMediaActivity` is asked to cast (`ManagerDataPlay.typeCast`). */
enum class CastType {
    PHOTO_OFFLINE, VIDEO_OFFLINE, AUDIO_OFFLINE, PHOTO_ONLINE, PHOTO_BROWSER, VIDEO_BROWSER, YOUTUBE_ONLINE, IMAGE_DRIVER, IPTV;

    val isPhoto get() = this == PHOTO_OFFLINE || this == PHOTO_ONLINE || this == PHOTO_BROWSER || this == IMAGE_DRIVER
}

/** Port of `com.connectsdk.ManagerDataPlay`: hand-off from list screens to `CastMediaActivity`. */
object CastSession {
    var type = CastType.PHOTO_OFFLINE
    var title = ""
    var path = ""
    var thumb = ""
    var position = 0
    var items: List<MediaItem> = emptyList()

    fun start(type: CastType, items: List<MediaItem>, position: Int) {
        this.type = type
        this.items = items
        this.position = position
        items.getOrNull(position)?.let {
            title = it.title
            path = it.path
            thumb = it.thumb
        }
    }
}

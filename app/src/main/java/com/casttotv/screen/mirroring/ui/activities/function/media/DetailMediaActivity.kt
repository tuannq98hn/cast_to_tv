package com.casttotv.screen.mirroring.ui.activities.function.media

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.databinding.ActivityDetailMediaBinding
import com.casttotv.screen.mirroring.models.CastSession
import com.casttotv.screen.mirroring.models.CastType
import com.casttotv.screen.mirroring.models.PhotoAlbum
import com.casttotv.screen.mirroring.ui.activities.function.play.CastMediaActivity

/** Port of `DetailMediaActivity` (TYPE=1): the videos of one album. */
class DetailMediaActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDetailMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindHeader(binding.include3, "Video")
        val videos = album?.photos.orEmpty()
        binding.rcvListMedia.layoutManager = GridLayoutManager(this, 3)
        binding.rcvListMedia.adapter = VideoGridAdapter(videos) { list, position ->
            withConnection {
                CastSession.start(CastType.VIDEO_OFFLINE, list, position)
                open(CastMediaActivity::class.java)
            }
        }
    }

    companion object {
        /** Album handed over by [MediaActivity] (the original passed it through `ManagerDataPlay`). */
        var album: PhotoAlbum? = null
    }
}

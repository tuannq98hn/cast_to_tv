package com.casttotv.screen.mirroring.mirror

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.casttotv.screen.mirroring.R
import com.casttotv.screen.mirroring.tv.FileServer
import com.casttotv.screen.mirroring.ui.activities.function.screen_mirror.ScreenMirroringActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream

/**
 * Replacement for the bundled ScreenStream-style MJPEG module (`com.base.mjpeg.MjpegModuleService`): captures the
 * screen with MediaProjection and serves it as MJPEG on `http://<phone-ip>:8080/stream.mjpeg`.
 */
class MirrorService : Service() {

    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var thread: HandlerThread? = null
    private var server: MjpegServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val data: Intent = intent?.let {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= 33) it.getParcelableExtra(EXTRA_DATA, Intent::class.java) else it.getParcelableExtra(EXTRA_DATA)
        } ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundCompat()
        start(intent.getIntExtra(EXTRA_RESULT, Activity.RESULT_CANCELED), data)
        return START_NOT_STICKY
    }

    private fun start(resultCode: Int, data: Intent) {
        val manager = getSystemService(MediaProjectionManager::class.java)
        val projection = manager.getMediaProjection(resultCode, data) ?: return stopSelf()
        this.projection = projection
        val server = MjpegServer(this, PORT).also { it.startServer(); this.server = it }

        val metrics = resources.displayMetrics
        val scale = minOf(1f, MAX_WIDTH.toFloat() / metrics.widthPixels)
        val width = (metrics.widthPixels * scale).toInt() / 2 * 2
        val height = (metrics.heightPixels * scale).toInt() / 2 * 2
        val thread = HandlerThread("mirror-capture").also { it.start(); this.thread = it }
        val handler = Handler(thread.looper)
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        this.reader = reader
        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            image.use {
                val plane = it.planes[0]
                val rowPadding = plane.rowStride - plane.pixelStride * width
                val bitmap = Bitmap.createBitmap(width + rowPadding / plane.pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(plane.buffer)
                val cropped = if (rowPadding == 0) bitmap else Bitmap.createBitmap(bitmap, 0, 0, width, height)
                val out = ByteArrayOutputStream()
                cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                server.publish(out.toByteArray())
                if (cropped !== bitmap) cropped.recycle()
                bitmap.recycle()
            }
        }, handler)

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() = stopSelf()
        }, handler)
        display = projection.createVirtualDisplay(
            "CastToTV", width, height, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, handler,
        )
        _state.value = State.Streaming("http://${FileServer.phoneIp(this)}:$PORT")
    }

    override fun onDestroy() {
        display?.release()
        reader?.close()
        projection?.stop()
        server?.stop()
        thread?.quitSafely()
        _state.value = State.Idle
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.screen_mirroring), NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, ScreenMirroringActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MirrorService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.icon_app)
            .setContentTitle(getString(R.string.screen_mirroring))
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    sealed interface State {
        data object Idle : State

        /** [address] is `http://ip:port`; the raw stream is at `address/stream.mjpeg`. */
        data class Streaming(val address: String) : State {
            val streamUrl get() = "$address/stream.mjpeg"
        }
    }

    companion object {
        const val PORT = 8080
        private const val MAX_WIDTH = 1280
        private const val JPEG_QUALITY = 70
        private const val CHANNEL = "com.casttotv.screen.mirroring.NOTIFICATION_CHANNEL_STREAMING"
        private const val NOTIFICATION_ID = 101
        private const val EXTRA_RESULT = "result"
        private const val EXTRA_DATA = "data"
        private const val ACTION_STOP = "stop"

        private val _state = MutableStateFlow<State>(State.Idle)
        val state: StateFlow<State> = _state

        fun captureIntent(context: Context): Intent =
            context.getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent()

        fun start(context: Context, resultCode: Int, data: Intent) {
            ContextCompat.startForegroundService(
                context, Intent(context, MirrorService::class.java).putExtra(EXTRA_RESULT, resultCode).putExtra(EXTRA_DATA, data),
            )
        }

        fun stop(context: Context) = context.stopService(Intent(context, MirrorService::class.java))
    }
}

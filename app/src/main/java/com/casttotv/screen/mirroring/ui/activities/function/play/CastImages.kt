package com.casttotv.screen.mirroring.ui.activities.function.play

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.wifi.WifiManager
import java.io.File
import java.io.FileOutputStream

/** Image preparation before casting a local photo (`qr4`/`wr.y0`/`tr4`). */
internal object CastImages {

    /** On a strong Wi-Fi link the original re-encodes the photo (downsampled to ~20MP) before serving it. */
    @Suppress("DEPRECATION")
    fun prepare(context: Context, path: String): String {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val info = wifi?.connectionInfo
        val strong = info != null && info.rssi > -60 && info.linkSpeed >= 86
        return if (strong) resize(context, path, 96) ?: path else path
    }

    /** Rotates the photo by [degrees] into the cache and returns the new file (`tr4`). */
    fun rotate(context: Context, path: String, degrees: Float): String? = runCatching {
        val source = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize(path) })
            ?: return null
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate(degrees) }, true)
        val out = File(context.externalCacheDir ?: context.cacheDir, "rotate_${degrees.toInt()}_${File(path).name}")
        FileOutputStream(out).use { rotated.compress(Bitmap.CompressFormat.JPEG, 96, it) }
        out.path
    }.getOrNull()

    private fun resize(context: Context, path: String, quality: Int): String? = runCatching {
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize(path) })
            ?: return null
        val out = File(context.externalCacheDir ?: context.cacheDir, "resizeImage_" + File(path).name)
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        bitmap.recycle()
        out.path
    }.getOrElse { if (it is OutOfMemoryError && quality - 5 >= 50) resize(context, path, quality - 5) else null }

    /** `wr.R`: power-of-two sample size keeping ARGB_8888 pixels under 2e7 bytes. */
    private fun sampleSize(path: String): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val bytes = bounds.outWidth.toDouble() * bounds.outHeight * 4
        val factor = if (bytes > 2.0e7) bytes / 2.0e7 else 1.0
        val maxH = bounds.outHeight / factor
        val maxW = bounds.outWidth / factor
        var sample = 1
        if (bounds.outHeight > maxH || bounds.outWidth > maxW) {
            val halfH = bounds.outHeight / 2
            val halfW = bounds.outWidth / 2
            while (halfH / sample >= maxH && halfW / sample >= maxW) sample *= 2
        }
        return sample
    }
}

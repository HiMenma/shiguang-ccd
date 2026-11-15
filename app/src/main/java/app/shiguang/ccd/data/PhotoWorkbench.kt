package app.shiguang.ccd.data

import android.graphics.Bitmap
import android.net.Uri
import app.shiguang.ccd.AppContainer
import app.shiguang.ccd.render.LookRenderer
import app.shiguang.imaging.LookParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs the same GPU pipeline as the live preview but on a still image, off the
 * main thread.  Each call owns a short-lived EGL context.
 */
class PhotoWorkbench(private val container: AppContainer) {

    suspend fun render(bitmap: Bitmap, params: LookParams, mirror: Boolean = false): Bitmap =
        withContext(Dispatchers.Default) {
            val renderer = LookRenderer()
            try {
                renderer.init()
                renderer.renderBitmap(bitmap, params, mirror)
            } finally {
                renderer.release()
            }
        }

    suspend fun renderSource(uri: Uri, params: LookParams, maxSize: Int = 2048): Bitmap {
        val source = container.gallery.load(uri, maxSize)
        return render(source, params)
    }

    suspend fun finishAndSave(
        bitmap: Bitmap,
        params: LookParams,
        quality: Int = 92,
        name: String = "SG_${System.currentTimeMillis()}.jpg",
    ): Uri {
        val finished = PhotoFinisher.finish(bitmap, params, System.currentTimeMillis())
        return container.gallery.save(finished, quality, name)
    }
}

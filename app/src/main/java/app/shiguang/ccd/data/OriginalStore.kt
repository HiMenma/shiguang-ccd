package app.shiguang.ccd.data

import android.content.Context
import java.io.File

/**
 * Keeps the untouched camera JPEG for every shot so the editor can re-apply a
 * different look without stacking filters on an already processed image.
 */
class OriginalStore(context: Context) {

    private val dir = File(context.filesDir, "originals").apply { mkdirs() }

    fun save(key: String, source: File): Boolean = runCatching {
        source.copyTo(File(dir, "$key.jpg"), overwrite = true)
        true
    }.getOrDefault(false)

    fun file(key: String): File? = File(dir, "$key.jpg").takeIf { it.isFile }

    fun delete(key: String) {
        runCatching { File(dir, "$key.jpg").delete() }
    }
}

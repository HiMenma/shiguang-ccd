package app.shiguang.ccd

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.shiguang.ccd.data.GalleryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryRepositoryTest {

    private val repository: GalleryRepository
        get() = GalleryRepository(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun saveThenQueryAndLoad() = runBlocking {
        val bitmap = Bitmap.createBitmap(40, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(220, 40, 40))
        }
        val uri = repository.save(bitmap)

        val items = repository.recent(20)
        assertTrue("saved uri should be listed", items.any { it.uri == uri })

        val loaded = repository.load(uri, 256)
        assertEquals(40, loaded.width)
        assertEquals(24, loaded.height)
    }

    @Test
    fun recentReturnsAtMostLimit() = runBlocking {
        val items = repository.recent(1)
        assertTrue(items.size <= 1)
    }
}

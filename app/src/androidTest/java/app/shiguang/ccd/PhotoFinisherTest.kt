package app.shiguang.ccd

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.shiguang.ccd.data.PhotoFinisher
import app.shiguang.imaging.BorderStyle
import app.shiguang.imaging.LookParams
import app.shiguang.imaging.TimestampStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoFinisherTest {

    private fun solid(color: Int, size: Int = 200): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    @Test
    fun timestampDrawsPixels() {
        val params = LookParams(
            id = "t",
            name = "t",
            timestampStyle = TimestampStyle.ORANGE_LED,
            borderStyle = BorderStyle.NONE,
        )
        val out = PhotoFinisher.finish(solid(Color.BLACK), params, 1_600_000_000_000L)
        var painted = false
        var y = out.height * 3 / 4
        while (y < out.height && !painted) {
            var x = 0
            while (x < out.width) {
                if (out.getPixel(x, y) != Color.BLACK) {
                    painted = true
                    break
                }
                x += 3
            }
            y += 3
        }
        assertTrue("timestamp should paint pixels", painted)
    }

    @Test
    fun paperBorderGrowsTheBitmap() {
        val params = LookParams(
            id = "t",
            name = "t",
            borderStyle = BorderStyle.PAPER_WHITE,
            timestampStyle = TimestampStyle.NONE,
        )
        val source = solid(Color.rgb(10, 20, 30), 100)
        val out = PhotoFinisher.finish(source, params, 0L)
        assertTrue(out.width > source.width)
        assertTrue(out.height > source.height)
        assertEquals(Color.parseColor("#F7F1E3"), out.getPixel(2, 2))
    }

    @Test
    fun noDecorationKeepsSize() {
        val params = LookParams(id = "t", name = "t")
        val source = solid(Color.GRAY, 64)
        val out = PhotoFinisher.finish(source, params, 0L)
        assertEquals(64, out.width)
        assertEquals(64, out.height)
    }
}

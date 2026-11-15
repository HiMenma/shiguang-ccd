package app.shiguang.ccd

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.shiguang.ccd.render.LookRenderer
import app.shiguang.imaging.LookParams
import app.shiguang.imaging.PresetLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LookRendererTest {

    private fun solid(color: Int, size: Int = 24): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun render(source: Bitmap, params: LookParams): Bitmap {
        val renderer = LookRenderer()
        renderer.init()
        return try {
            renderer.renderBitmap(source, params)
        } finally {
            renderer.release()
        }
    }

    private val neutral = LookParams(
        id = "neutral",
        name = "neutral",
        shoulder = 0f,
        midGamma = 1f,
        toe = 0f,
        white = 1f,
        saturation = 1f,
        renderScale = 1f,
    )

    @Test
    fun identityPipelineKeepsColor() {
        val out = render(solid(Color.rgb(200, 120, 60)), neutral)
        val pixel = out.getPixel(out.width / 2, out.height / 2)
        assertTrue("red", kotlin.math.abs(200 - Color.red(pixel)) <= 12)
        assertTrue("green", kotlin.math.abs(120 - Color.green(pixel)) <= 12)
        assertTrue("blue", kotlin.math.abs(60 - Color.blue(pixel)) <= 12)
    }

    @Test
    fun monoPresetProducesGray() {
        val out = render(solid(Color.rgb(200, 90, 40)), PresetLibrary.MONO)
        val pixel = out.getPixel(out.width / 2, out.height / 2)
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        assertTrue("mono should be gray, got $r/$g/$b", kotlin.math.abs(r - g) < 12 && kotlin.math.abs(g - b) < 12)
    }

    @Test
    fun everyPresetRenders() {
        val source = solid(Color.rgb(120, 160, 200), 32)
        for (preset in PresetLibrary.builtIn) {
            val out = render(source, preset)
            assertEquals(32, out.width)
            assertEquals(32, out.height)
        }
    }

    @Test
    fun presetsProduceDifferentPixels() {
        val source = solid(Color.rgb(120, 160, 200), 32)
        val cream = render(source, PresetLibrary.CREAM).getPixel(16, 16)
        val blue = render(source, PresetLibrary.BLUE).getPixel(16, 16)
        assertTrue("presets should differ", cream != blue)
    }
}

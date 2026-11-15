package app.shiguang.imaging

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {

    @Test
    fun `5500K is neutral`() {
        val g = ColorMath.whiteBalanceGains(5500f, 0f)
        assertEquals(1f, g[0], 0.02f)
        assertEquals(1f, g[1], 0.02f)
        assertEquals(1f, g[2], 0.02f)
    }

    @Test
    fun `tungsten leans red, daylight leans blue`() {
        val warm = ColorMath.whiteBalanceGains(3000f, 0f)
        val cool = ColorMath.whiteBalanceGains(8500f, 0f)
        assertTrue("warm R should exceed B", warm[0] > warm[2])
        assertTrue("cool B should exceed R", cool[2] > cool[0])
    }

    @Test
    fun `gains preserve luminance`() {
        for (k in listOf(3000f, 4500f, 5500f, 7000f, 9000f)) {
            val g = ColorMath.whiteBalanceGains(k, 0f)
            val lum = ColorMath.luma(g[0], g[1], g[2])
            assertEquals(1f, lum, 0.001f)
        }
    }

    @Test
    fun `saturation matrix keeps luma and reaches zero at sat 0`() {
        val zero = ColorMath.saturationMatrix(0f)
        val out = ColorMath.applyMatrix(zero, 0.8f, 0.4f, 0.2f)
        assertEquals(out[0], out[1], 1e-4f)
        assertEquals(out[1], out[2], 1e-4f)
        assertEquals(ColorMath.luma(0.8f, 0.4f, 0.2f), out[0], 1e-3f)
    }

    @Test
    fun `identity matrix is identity`() {
        val out = ColorMath.applyMatrix(ColorMath.identityMatrix(), 0.3f, 0.6f, 0.9f)
        assertTrue(abs(out[0] - 0.3f) < 1e-6f)
        assertTrue(abs(out[1] - 0.6f) < 1e-6f)
        assertTrue(abs(out[2] - 0.9f) < 1e-6f)
    }

    @Test
    fun `srgb round trip`() {
        for (c in listOf(0f, 0.05f, 0.5f, 0.9f, 1f)) {
            val back = ColorMath.linearToSrgb(ColorMath.srgbToLinear(c))
            assertEquals(c, back, 1e-3f)
        }
    }
}

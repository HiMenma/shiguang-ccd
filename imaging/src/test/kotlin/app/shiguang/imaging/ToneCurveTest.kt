package app.shiguang.imaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneCurveTest {

    @Test
    fun `curve is monotonic for every built-in preset`() {
        for (preset in PresetLibrary.builtIn) {
            var previous = -1f
            var x = 0f
            while (x <= 1f) {
                val y = ToneCurve.apply(x, preset)
                assertTrue("${preset.name} not monotonic at $x: $previous -> $y", y >= previous - 1e-4f)
                previous = y
                x += 0.01f
            }
        }
    }

    @Test
    fun `output stays in range`() {
        for (preset in PresetLibrary.builtIn) {
            var x = 0f
            while (x <= 1f) {
                val y = ToneCurve.apply(x, preset)
                assertTrue("out of range for ${preset.name}: $y", y in 0f..1f)
                x += 0.02f
            }
        }
    }

    @Test
    fun `neutral params are close to identity`() {
        val p = LookParams(id = "test", name = "test", shoulder = 0f, midGamma = 1f, white = 1f)
        assertEquals(0f, ToneCurve.apply(0f, p), 1e-5f)
        assertEquals(0.5f, ToneCurve.apply(0.5f, p), 1e-4f)
        assertEquals(1f, ToneCurve.apply(1f, p), 1e-4f)
    }

    @Test
    fun `black lift raises the floor`() {
        val p = LookParams(id = "test", name = "test", shoulder = 0f, blackLift = 0.1f)
        assertEquals(0.1f, ToneCurve.apply(0f, p), 1e-4f)
    }

    @Test
    fun `exposure brightens`() {
        val base = LookParams(id = "a", name = "a", shoulder = 0f)
        val bright = base.copy(exposure = 0.5f)
        assertTrue(ToneCurve.apply(0.3f, bright) > ToneCurve.apply(0.3f, base))
    }
}

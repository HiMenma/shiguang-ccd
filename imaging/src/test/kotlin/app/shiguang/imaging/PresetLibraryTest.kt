package app.shiguang.imaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetLibraryTest {

    @Test
    fun `six built-in presets with unique ids`() {
        assertEquals(6, PresetLibrary.builtIn.size)
        assertEquals(6, PresetLibrary.builtIn.map { it.id }.distinct().size)
        assertEquals(6, PresetLibrary.builtIn.map { it.name }.distinct().size)
    }

    @Test
    fun `presets survive sanitizing unchanged`() {
        for (preset in PresetLibrary.builtIn) {
            assertEquals(preset, preset.sanitized())
        }
    }

    @Test
    fun `looks are actually different from each other`() {
        val looks = PresetLibrary.builtIn.map { it.sanitized() }
        for (i in looks.indices) {
            for (j in i + 1 until looks.size) {
                val a = looks[i]
                val b = looks[j]
                val distance = kotlin.math.abs(a.kelvin - b.kelvin) +
                    kotlin.math.abs(a.saturation - b.saturation) * 1000f +
                    kotlin.math.abs(a.renderScale - b.renderScale) * 1000f +
                    kotlin.math.abs(a.grainAmount - b.grainAmount) * 1000f
                assertTrue("${a.name} and ${b.name} are too similar", distance > 5f)
            }
        }
    }

    @Test
    fun `sanitize clamps wild values`() {
        val wild = LookParams(
            id = "x",
            name = "x",
            kelvin = 99999f,
            exposure = 12f,
            saturation = -4f,
            matrix = listOf(1f),
            renderScale = 3f,
        ).sanitized()
        assertTrue(wild.kelvin <= 10000f)
        assertEquals(2f, wild.exposure, 1e-4f)
        assertEquals(0f, wild.saturation, 1e-4f)
        assertEquals(9, wild.matrix.size)
        assertEquals(1f, wild.renderScale, 1e-4f)
    }

    @Test
    fun `serialization round trip`() {
        val original = PresetLibrary.PEEL
        val text = LookJson.encode(original)
        val back = LookJson.decode(text)
        assertNotNull(back)
        assertEquals(original, back)
    }

    @Test
    fun `list serialization round trip and corrupt input`() {
        val list = PresetLibrary.builtIn
        assertEquals(list, LookJson.decodeList(LookJson.encodeList(list)))
        assertTrue(LookJson.decodeList("not json").isEmpty())
        assertEquals(null, LookJson.decode("{}"))
    }
}

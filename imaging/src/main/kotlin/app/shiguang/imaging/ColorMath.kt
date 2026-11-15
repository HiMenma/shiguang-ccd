package app.shiguang.imaging

import kotlin.math.ln
import kotlin.math.pow

/**
 * Pure colour maths shared by the GLSL shader (as a reference implementation)
 * and the CPU fallback renderer.
 */
object ColorMath {

    const val LUMA_R = 0.2126f
    const val LUMA_G = 0.7152f
    const val LUMA_B = 0.0722f

    fun luma(r: Float, g: Float, b: Float): Float = LUMA_R * r + LUMA_G * g + LUMA_B * b

    fun applyMatrix(m: List<Float>, r: Float, g: Float, b: Float): FloatArray {
        require(m.size == 9) { "matrix must have 9 elements" }
        return floatArrayOf(
            m[0] * r + m[1] * g + m[2] * b,
            m[3] * r + m[4] * g + m[5] * b,
            m[6] * r + m[7] * g + m[8] * b,
        )
    }

    /** Tanner Helland's approximation, returns linear-ish sRGB in 0..1. */
    fun kelvinToRgb(kelvin: Float): FloatArray {
        val t = (kelvin.coerceIn(1000f, 40000f)) / 100f
        val r: Float
        val g: Float
        val b: Float
        if (t <= 66f) {
            r = 255f
            g = (99.4708025861f * ln(t.toDouble()) - 161.1195681661f).toFloat()
            b = if (t <= 19f) 0f else (138.5177312231f * ln((t - 10f).toDouble()) - 305.0447927307f).toFloat()
        } else {
            r = (329.698727446f * (t - 60f).pow(-0.1332047592f)).toFloat()
            g = (288.1221695283f * (t - 60f).pow(-0.0755148492f)).toFloat()
            b = 255f
        }
        return floatArrayOf(
            (r / 255f).coerceIn(0f, 1f),
            (g / 255f).coerceIn(0f, 1f),
            (b / 255f).coerceIn(0f, 1f),
        )
    }

    /**
     * Per-channel gains that turn a 5500K-referenced image into the requested
     * illuminant.  Normalised so overall luminance stays put (no accidental
     * exposure change when the user drags the temperature slider).
     */
    fun whiteBalanceGains(kelvin: Float, tint: Float): FloatArray {
        val ref = kelvinToRgb(5500f)
        val cur = kelvinToRgb(kelvin)
        var r = cur[0] / ref[0].coerceAtLeast(1e-4f)
        var g = cur[1] / ref[1].coerceAtLeast(1e-4f)
        var b = cur[2] / ref[2].coerceAtLeast(1e-4f)
        // Positive tint pushes green down => magenta cast.
        g *= (1f - tint.coerceIn(-1f, 1f) * 0.25f).coerceAtLeast(0.25f)
        val lum = luma(r, g, b).coerceAtLeast(1e-4f)
        r /= lum
        g /= lum
        b /= lum
        return floatArrayOf(r, g, b)
    }

    fun saturationMatrix(sat: Float): List<Float> {
        val s = sat.coerceIn(0f, 2f)
        val inv = 1f - s
        return listOf(
            LUMA_R * inv + s, LUMA_G * inv, LUMA_B * inv,
            LUMA_R * inv, LUMA_G * inv + s, LUMA_B * inv,
            LUMA_R * inv, LUMA_G * inv, LUMA_B * inv + s,
        )
    }

    fun monochromeMatrix(): List<Float> = listOf(
        0.50f, 0.36f, 0.14f,
        0.50f, 0.36f, 0.14f,
        0.50f, 0.36f, 0.14f,
    )

    fun identityMatrix(): List<Float> = LookParams.DEFAULT_MATRIX

    fun srgbToLinear(c: Float): Float =
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

    fun linearToSrgb(c: Float): Float =
        if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1f / 2.4f) - 0.055f
}

package app.shiguang.imaging

import kotlinx.serialization.Serializable

/**
 * Every knob the renderer understands.  Presets are just instances of this
 * class, and the parameter sheet edits the same fields, so a "look" is always
 * fully described by data - no hidden state.
 *
 * Values are deliberately stored as plain floats (not enums) so the whole
 * struct can be uploaded to a GLSL program without conversion.
 */
@Serializable
data class LookParams(
    val id: String,
    val name: String,
    // White balance.  Absolute illuminant temperature; 5500K is neutral.
    val kelvin: Float = 5500f,
    val tint: Float = 0f,
    // 3x3 row-major colour matrix applied after the white balance gain.
    val matrix: List<Float> = DEFAULT_MATRIX,
    // Tone shaping.
    val exposure: Float = 0f,
    val blackLift: Float = 0f,
    val toe: Float = 0f,
    val midGamma: Float = 1f,
    val shoulder: Float = 0.12f,
    val white: Float = 1f,
    // Colour shaping.
    val saturation: Float = 1f,
    val shadowTint: List<Float> = ZERO3,
    val highlightTint: List<Float> = ZERO3,
    // Optics / sensor character.
    val bloomStrength: Float = 0f,
    val bloomThreshold: Float = 0.75f,
    val chromaticAberration: Float = 0f,
    val vignette: Float = 0f,
    val grainAmount: Float = 0f,
    val grainSize: Float = 1f,
    val sharpen: Float = 0f,
    val renderScale: Float = 1f,
    // Presentation.
    val timestampStyle: TimestampStyle = TimestampStyle.NONE,
    val borderStyle: BorderStyle = BorderStyle.NONE,
    val mirror: Boolean = false,
) {

    fun sanitized(): LookParams {
        val m = if (matrix.size == 9 && matrix.all { it.isFinite() }) matrix else DEFAULT_MATRIX
        return copy(
            kelvin = kelvin.clamp(2500f, 10000f),
            tint = tint.clamp(-1f, 1f),
            matrix = m,
            exposure = exposure.clamp(-2f, 2f),
            blackLift = blackLift.clamp(0f, 0.3f),
            toe = toe.clamp(0f, 0.9f),
            midGamma = midGamma.clamp(0.5f, 2f),
            shoulder = shoulder.clamp(0f, 0.6f),
            white = white.clamp(0.7f, 1f),
            saturation = saturation.clamp(0f, 2f),
            shadowTint = tint3(shadowTint),
            highlightTint = tint3(highlightTint),
            bloomStrength = bloomStrength.clamp(0f, 0.6f),
            bloomThreshold = bloomThreshold.clamp(0f, 1f),
            chromaticAberration = chromaticAberration.clamp(0f, 1f),
            vignette = vignette.clamp(0f, 0.8f),
            grainAmount = grainAmount.clamp(0f, 1f),
            grainSize = grainSize.clamp(0.5f, 3f),
            sharpen = sharpen.clamp(0f, 1f),
            renderScale = renderScale.clamp(0.4f, 1f),
        )
    }

    fun withCameraProfile(kelvin: Float, tint: Float): LookParams =
        copy(kelvin = kelvin, tint = tint).sanitized()

    companion object {
        val DEFAULT_MATRIX: List<Float> = listOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f,
        )
        val ZERO3: List<Float> = listOf(0f, 0f, 0f)
    }
}

private fun tint3(v: List<Float>): List<Float> =
    if (v.size == 3 && v.all { it.isFinite() }) v.map { it.clamp(-0.25f, 0.25f) } else LookParams.ZERO3

internal fun Float.clamp(lo: Float, hi: Float): Float = when {
    !isFinite() -> lo
    this < lo -> lo
    this > hi -> hi
    else -> this
}

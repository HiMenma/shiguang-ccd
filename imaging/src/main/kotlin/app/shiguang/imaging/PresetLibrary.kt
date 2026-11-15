package app.shiguang.imaging

/**
 * Built-in looks.  Names and numbers are our own - nothing here is copied
 * from a commercial camera app; they are calibrated around the generic
 * "early digital compact" character described in docs/DESIGN.md.
 */
object PresetLibrary {

    val CREAM = LookParams(
        id = "cream",
        name = "奶油",
        kelvin = 5850f,
        tint = 0.08f,
        matrix = listOf(
            1.03f, 0.01f, 0.00f,
            0.01f, 1.00f, 0.00f,
            0.00f, 0.01f, 0.97f,
        ),
        exposure = 0.15f,
        blackLift = 0.035f,
        toe = 0.25f,
        midGamma = 0.92f,
        shoulder = 0.18f,
        white = 0.98f,
        saturation = 1.00f,
        shadowTint = listOf(0.020f, 0.000f, 0.010f),
        highlightTint = listOf(0.030f, 0.010f, 0.000f),
        bloomStrength = 0.12f,
        chromaticAberration = 0.05f,
        vignette = 0.12f,
        grainAmount = 0.20f,
        grainSize = 1.0f,
        sharpen = 0.15f,
        renderScale = 1.0f,
        timestampStyle = TimestampStyle.NONE,
    )

    val Y2K = LookParams(
        id = "y2k",
        name = "千禧",
        kelvin = 5600f,
        tint = -0.05f,
        matrix = listOf(
            1.00f, 0.00f, 0.02f,
            0.00f, 1.00f, 0.00f,
            0.02f, 0.00f, 1.02f,
        ),
        exposure = 0.0f,
        blackLift = 0.0f,
        toe = 0.0f,
        midGamma = 1.05f,
        shoulder = 0.12f,
        white = 1.0f,
        saturation = 1.10f,
        shadowTint = listOf(0.006f, 0.000f, 0.018f),
        highlightTint = listOf(0.028f, 0.000f, 0.048f),
        bloomStrength = 0.30f,
        bloomThreshold = 0.70f,
        chromaticAberration = 0.50f,
        vignette = 0.22f,
        grainAmount = 0.45f,
        grainSize = 1.3f,
        sharpen = 0.10f,
        renderScale = 0.62f,
        timestampStyle = TimestampStyle.ORANGE_LED,
    )

    val STREET = LookParams(
        id = "street",
        name = "街拍",
        kelvin = 5400f,
        tint = 0.0f,
        matrix = listOf(
            1.02f, 0.00f, -0.02f,
            0.00f, 1.00f, 0.00f,
            -0.02f, 0.01f, 1.04f,
        ),
        exposure = 0.05f,
        blackLift = 0.0f,
        toe = 0.0f,
        midGamma = 1.12f,
        shoulder = 0.10f,
        white = 1.0f,
        saturation = 1.20f,
        shadowTint = listOf(-0.020f, 0.010f, 0.030f),
        highlightTint = listOf(0.020f, 0.000f, -0.020f),
        bloomStrength = 0.10f,
        chromaticAberration = 0.10f,
        vignette = 0.18f,
        grainAmount = 0.32f,
        grainSize = 1.0f,
        sharpen = 0.35f,
        renderScale = 0.85f,
    )

    val BLUE = LookParams(
        id = "blue",
        name = "蓝调",
        kelvin = 4700f,
        tint = 0.02f,
        matrix = listOf(
            0.98f, 0.00f, 0.02f,
            0.00f, 1.00f, 0.01f,
            0.00f, 0.00f, 1.05f,
        ),
        exposure = 0.0f,
        blackLift = 0.01f,
        toe = 0.10f,
        midGamma = 1.0f,
        shoulder = 0.15f,
        white = 1.0f,
        saturation = 0.85f,
        shadowTint = listOf(-0.010f, 0.010f, 0.050f),
        highlightTint = listOf(0.000f, 0.010f, 0.030f),
        bloomStrength = 0.15f,
        chromaticAberration = 0.08f,
        vignette = 0.22f,
        grainAmount = 0.28f,
        grainSize = 1.0f,
        sharpen = 0.15f,
        renderScale = 0.90f,
    )

    val MONO = LookParams(
        id = "mono",
        name = "银盐",
        kelvin = 5500f,
        tint = 0.0f,
        matrix = ColorMath.monochromeMatrix(),
        exposure = 0.0f,
        blackLift = 0.0f,
        toe = 0.05f,
        midGamma = 1.10f,
        shoulder = 0.20f,
        white = 1.0f,
        saturation = 0.0f,
        bloomStrength = 0.12f,
        chromaticAberration = 0.0f,
        vignette = 0.28f,
        grainAmount = 0.65f,
        grainSize = 1.1f,
        sharpen = 0.25f,
        renderScale = 0.90f,
    )

    val PEEL = LookParams(
        id = "peel",
        name = "撕拉",
        kelvin = 6500f,
        tint = 0.05f,
        matrix = listOf(
            1.04f, 0.02f, 0.00f,
            0.02f, 1.00f, 0.00f,
            0.00f, 0.02f, 0.94f,
        ),
        exposure = 0.02f,
        blackLift = 0.06f,
        toe = 0.30f,
        midGamma = 0.95f,
        shoulder = 0.25f,
        white = 0.95f,
        saturation = 0.95f,
        shadowTint = listOf(0.030f, 0.020f, 0.000f),
        highlightTint = listOf(0.050f, 0.030f, 0.000f),
        bloomStrength = 0.20f,
        chromaticAberration = 0.12f,
        vignette = 0.30f,
        grainAmount = 0.40f,
        grainSize = 1.0f,
        sharpen = 0.10f,
        renderScale = 0.80f,
        borderStyle = BorderStyle.PAPER_WHITE,
        timestampStyle = TimestampStyle.WHITE_DIGITAL,
    )

    val builtIn: List<LookParams> = listOf(CREAM, Y2K, STREET, BLUE, MONO, PEEL)

    val default: LookParams get() = builtIn.first()

    fun byId(id: String): LookParams? = builtIn.firstOrNull { it.id == id }

    fun catalog(custom: List<LookParams> = emptyList()): List<LookParams> = builtIn + custom
}

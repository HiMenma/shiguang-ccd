package app.shiguang.ccd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF14120F)
val InkSoft = Color(0xFF1E1B17)
val Cream = Color(0xFFF3EDE2)
val CreamDim = Color(0xFF9E968A)
val Amber = Color(0xFFF2A03D)
val FilmRed = Color(0xFFD9533F)

private val DarkScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    secondary = Cream,
    onSecondary = Ink,
    background = Ink,
    onBackground = Cream,
    surface = InkSoft,
    onSurface = Cream,
    surfaceVariant = Color(0xFF2A2620),
    onSurfaceVariant = CreamDim,
    error = FilmRed,
)

private val LightScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    secondary = Ink,
    onSecondary = Cream,
    background = Ink,
    onBackground = Cream,
    surface = InkSoft,
    onSurface = Cream,
    surfaceVariant = Color(0xFF2A2620),
    onSurfaceVariant = CreamDim,
    error = FilmRed,
)

@Composable
fun ShiguangTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ShiguangTypography,
        content = content,
    )
}

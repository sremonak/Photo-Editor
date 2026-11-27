package studio.velox.atelier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val VeloxColorScheme = darkColorScheme(
    primary = LeicaVermilion,
    onPrimary = ChalkWhite,
    secondary = ChampagneBrass,
    onSecondary = ObsidianStage,
    tertiary = CobaltMask,
    background = ObsidianStage,
    onBackground = ChalkWhite,
    surface = TitaniumChassis,
    onSurface = ChalkWhite,
    surfaceVariant = GraphiteIsland,
    onSurfaceVariant = SilverLabel,
    outline = MachinedBevel
)

private val VeloxTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp
    )
)

@Composable
fun VeloxAtelierTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VeloxColorScheme,
        typography = VeloxTypography,
        content = content
    )
}

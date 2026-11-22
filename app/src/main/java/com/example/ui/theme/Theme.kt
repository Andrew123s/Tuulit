package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class TuulitExtendedColors(
    val periodClay: Color,
    val periodClaySoft: Color,
    val estimateOchre: Color,
    val estimateOchreSoft: Color,
    val bananaYellow: Color,
    val softYellow: Color,
    val warmTan: Color,
    val toastedSand: Color,
    val hairlineBorder: Color,
    val alertSoft: Color,
)

private val LightExtendedColors = TuulitExtendedColors(
    periodClay = TuulitPeriodClay,
    periodClaySoft = TuulitPeriodClaySoft,
    estimateOchre = TuulitEstimateOchre,
    estimateOchreSoft = TuulitEstimateOchreSoft,
    bananaYellow = TuulitBananaYellow,
    softYellow = TuulitSoftYellow,
    warmTan = TuulitWarmTan,
    toastedSand = TuulitToastedSand,
    hairlineBorder = TuulitTaupeBorder,
    alertSoft = TuulitAlertCrimsonSoft,
)

private val DarkExtendedColors = TuulitExtendedColors(
    periodClay = TuulitDarkClay,
    periodClaySoft = TuulitDarkClaySoft,
    estimateOchre = TuulitDarkOchre,
    estimateOchreSoft = TuulitDarkOchreSoft,
    bananaYellow = TuulitDarkYellow,
    softYellow = Color(0xFF423618),
    warmTan = TuulitDarkTan,
    toastedSand = TuulitDarkSand,
    hairlineBorder = TuulitDarkBorder,
    alertSoft = TuulitDarkAlertSoft,
)

val LocalTuulitColors = staticCompositionLocalOf { LightExtendedColors }

private val TuulitLightColorScheme = lightColorScheme(
    primary = TuulitCharcoal,
    onPrimary = TuulitParchment,
    primaryContainer = TuulitWarmTan,
    onPrimaryContainer = TuulitCharcoal,
    secondary = TuulitToastedSand,
    onSecondary = TuulitCharcoal,
    secondaryContainer = TuulitWarmTan,
    onSecondaryContainer = TuulitCharcoal,
    tertiary = TuulitBananaYellow,
    onTertiary = TuulitCharcoal,
    tertiaryContainer = TuulitSoftYellow,
    onTertiaryContainer = TuulitCharcoal,
    background = TuulitCream,
    onBackground = TuulitCharcoal,
    surface = TuulitParchment,
    onSurface = TuulitCharcoal,
    surfaceVariant = TuulitWarmTan,
    onSurfaceVariant = TuulitEspresso,
    outline = TuulitMutedInk,
    outlineVariant = TuulitTaupeBorder,
    error = TuulitAlertCrimson,
    onError = TuulitParchment,
    errorContainer = TuulitAlertCrimsonSoft,
    onErrorContainer = TuulitAlertCrimson,
)

private val TuulitDarkColorScheme = darkColorScheme(
    primary = TuulitDarkInk,
    onPrimary = TuulitDarkBg,
    primaryContainer = TuulitDarkTan,
    onPrimaryContainer = TuulitDarkInk,
    secondary = TuulitDarkSand,
    onSecondary = TuulitDarkInk,
    secondaryContainer = TuulitDarkTan,
    onSecondaryContainer = TuulitDarkInk,
    tertiary = TuulitDarkYellow,
    onTertiary = TuulitCharcoal,
    tertiaryContainer = Color(0xFF423618),
    onTertiaryContainer = TuulitDarkYellow,
    background = TuulitDarkBg,
    onBackground = TuulitDarkInk,
    surface = TuulitDarkSurface,
    onSurface = TuulitDarkInk,
    surfaceVariant = TuulitDarkTan,
    onSurfaceVariant = TuulitDarkMutedInk,
    outline = TuulitDarkMutedInk,
    outlineVariant = TuulitDarkBorder,
    error = TuulitDarkAlert,
    onError = TuulitDarkBg,
    errorContainer = TuulitDarkAlertSoft,
    onErrorContainer = TuulitDarkAlert,
)

val TuulitShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object TuulitThemeTokens {
    val extended: TuulitExtendedColors
        @Composable get() = LocalTuulitColors.current
}

@Composable
fun TuulitTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TuulitDarkColorScheme else TuulitLightColorScheme
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalTuulitColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = TuulitShapes,
            content = content
        )
    }
}

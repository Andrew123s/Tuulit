package app.tuulit.ui.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tuulit.R
import app.tuulit.data.local.ThemeMode

/**
 * Colour tokens, v2 (light-first, warm and feminine). Old names are kept so every screen keeps
 * working: cream = ivory page, oat = raised surface, sand = vanilla fill, tan = peachy tan,
 * period = soft coral, fertile = muted lilac, toffee = cocoa accent.
 */
@Immutable
data class TuulitColors(
    val cream: Color, val oat: Color, val sand: Color, val tan: Color, val hairline: Color,
    val ink: Color, val inkMuted: Color, val inkFaint: Color, val onInk: Color,
    val toffee: Color, val banana: Color, val onBanana: Color, val bananaSoft: Color,
    val period: Color, val periodSoft: Color, val periodDeep: Color, val onPeriod: Color,
    val fertile: Color, val fertileSoft: Color, val lilacDeep: Color,
    val peach: Color, val peachSoft: Color, val rose: Color, val roseSoft: Color,
    val alert: Color, val alertSoft: Color, val focus: Color, val scrim: Color, val isDark: Boolean,
)

val LightColors = TuulitColors(
    cream = Color(0xFFFFF8F0), oat = Color(0xFFFFFDF9), sand = Color(0xFFFBEEDD), tan = Color(0xFFF2D9BE),
    hairline = Color(0xFFF0E2D0), ink = Color(0xFF2B2320), inkMuted = Color(0xFF6B5A50), inkFaint = Color(0xFF7D6857),
    onInk = Color(0xFFFFF8F0), toffee = Color(0xFF9A5B3C), banana = Color(0xFFF7C948), onBanana = Color(0xFF2B2320),
    bananaSoft = Color(0xFFFFF0C2), period = Color(0xFFE2725B), periodSoft = Color(0xFFFDDDD3), periodDeep = Color(0xFFB5482F),
    onPeriod = Color(0xFF2B2320), fertile = Color(0xFF9C88C9), fertileSoft = Color(0xFFECE5F7), lilacDeep = Color(0xFF6F5AA8),
    peach = Color(0xFFFFC9AE), peachSoft = Color(0xFFFFE7DA), rose = Color(0xFFE7A6B0), roseSoft = Color(0xFFF9E1E4),
    alert = Color(0xFFB3261E), alertSoft = Color(0xFFFCE3DF), focus = Color(0xFF2B2320),
    scrim = Color(0x522B2320), isDark = false,
)

val DarkColors = TuulitColors(
    cream = Color(0xFF1C1517), oat = Color(0xFF261D20), sand = Color(0xFF33272A), tan = Color(0xFF4B3A37),
    hairline = Color(0xFF3D2F31), ink = Color(0xFFF8EEE6), inkMuted = Color(0xFFCDBAB0), inkFaint = Color(0xFFA8958B),
    onInk = Color(0xFF1C1517), toffee = Color(0xFFF0B48C), banana = Color(0xFFF7CF5C), onBanana = Color(0xFF2B2320),
    bananaSoft = Color(0xFF3E3320), period = Color(0xFFF08A73), periodSoft = Color(0xFF4A2A26), periodDeep = Color(0xFFF5A08C),
    onPeriod = Color(0xFF2B2320), fertile = Color(0xFFB7A6E0), fertileSoft = Color(0xFF332C45), lilacDeep = Color(0xFFC9BCEB),
    peach = Color(0xFFF4B69A), peachSoft = Color(0xFF45302A), rose = Color(0xFFE7A6B0), roseSoft = Color(0xFF452E34),
    alert = Color(0xFFF2998F), alertSoft = Color(0xFF4A2522), focus = Color(0xFFF7CF5C),
    scrim = Color(0x99000000), isDark = true,
)

/** Mascot art colours are fixed in both themes; the mark is a picture, not UI. */
object MascotColors {
    val Banana = Color(0xFFF7C948)
    val Tan = Color(0xFFF2CFA8)
    val Flesh = Color(0xFFFFFBF3)
    val Outline = Color(0xFF3A2A24)
    val Prop = Color(0xFFE2725B)
    val Blush = Color(0x66F49A8C)
}

@OptIn(ExperimentalTextApi::class)
private fun fraunces(weight: Int, opsz: Float) = Font(
    R.font.fraunces_variable, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", opsz), FontVariation.Setting("SOFT", 50f)),
)

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans_variable, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun style(family: FontFamily, size: TextUnit, line: TextUnit, weight: Int, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = family, fontSize = size, lineHeight = line, fontWeight = FontWeight(weight), letterSpacing = tracking,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

/** Type styles from the design system. Each Fraunces style has its own optical size. */
@Immutable
data class TuulitType(
    val numeral: TextStyle, val display: TextStyle, val headline: TextStyle, val title: TextStyle,
    val subtitle: TextStyle, val body: TextStyle, val bodySm: TextStyle, val label: TextStyle,
    val overline: TextStyle, val caption: TextStyle, val tab: TextStyle,
)

private val Sans = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700))

val DefaultType = TuulitType(
    numeral = style(FontFamily(fraunces(380, 96f)), 64.sp, 64.sp, 380, (-1.5).sp),
    display = style(FontFamily(fraunces(400, 72f)), 34.sp, 38.sp, 400, (-0.6).sp),
    headline = style(FontFamily(fraunces(450, 48f)), 26.sp, 31.sp, 450, (-0.3).sp),
    title = style(FontFamily(fraunces(500, 24f)), 20.sp, 26.sp, 500, (-0.1).sp),
    subtitle = style(Sans, 17.sp, 24.sp, 600),
    body = style(Sans, 16.sp, 24.sp, 400),
    bodySm = style(Sans, 14.sp, 20.sp, 400),
    label = style(Sans, 14.sp, 18.sp, 600, 0.1.sp),
    overline = style(Sans, 12.sp, 16.sp, 600, 0.8.sp),
    caption = style(Sans, 12.sp, 16.sp, 500),
    tab = style(Sans, 11.sp, 14.sp, 600),
)

/** Spacing and radius tokens. */
object Space {
    val s1: Dp = 4.dp; val s2: Dp = 8.dp; val s3: Dp = 12.dp; val s4: Dp = 16.dp; val s5: Dp = 20.dp
    val s6: Dp = 24.dp; val s8: Dp = 32.dp; val s10: Dp = 40.dp; val s14: Dp = 56.dp
}

object Radius {
    val sm: Dp = 10.dp; val md: Dp = 16.dp; val lg: Dp = 28.dp; val pill: Dp = 999.dp
}

val LocalTuulitColors = staticCompositionLocalOf { LightColors }
val LocalTuulitType = staticCompositionLocalOf { DefaultType }
val LocalReducedMotion = staticCompositionLocalOf { false }

object Tuulit {
    val colors: TuulitColors @Composable @ReadOnlyComposable get() = LocalTuulitColors.current
    val type: TuulitType @Composable @ReadOnlyComposable get() = LocalTuulitType.current
    val reducedMotion: Boolean @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}

@Composable
fun TuulitTheme(mode: ThemeMode = ThemeMode.SYSTEM, reducedMotion: Boolean = false, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(primary = c.ink, onPrimary = c.onInk, background = c.cream, onBackground = c.ink, surface = c.oat,
            onSurface = c.ink, surfaceVariant = c.sand, onSurfaceVariant = c.inkMuted, outline = c.inkFaint, error = c.alert,
            secondary = c.toffee, tertiary = c.banana, surfaceContainerLow = c.oat, surfaceContainer = c.oat)
    } else {
        lightColorScheme(primary = c.ink, onPrimary = c.onInk, background = c.cream, onBackground = c.ink, surface = c.oat,
            onSurface = c.ink, surfaceVariant = c.sand, onSurfaceVariant = c.inkMuted, outline = c.inkFaint, error = c.alert,
            secondary = c.toffee, tertiary = c.banana, surfaceContainerLow = c.oat, surfaceContainer = c.oat)
    }
    CompositionLocalProvider(LocalTuulitColors provides c, LocalTuulitType provides DefaultType, LocalReducedMotion provides reducedMotion) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography.copy(bodyLarge = DefaultType.body), content = content)
    }
}

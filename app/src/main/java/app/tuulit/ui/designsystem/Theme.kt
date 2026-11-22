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

/** Colour tokens from the Tuulit Design System (tokens.json). Names match the design exactly. */
@Immutable
data class TuulitColors(
    val cream: Color, val oat: Color, val sand: Color, val tan: Color, val hairline: Color,
    val ink: Color, val inkMuted: Color, val inkFaint: Color, val onInk: Color,
    val toffee: Color, val banana: Color, val onBanana: Color, val bananaSoft: Color,
    val period: Color, val periodSoft: Color, val fertile: Color, val fertileSoft: Color,
    val alert: Color, val alertSoft: Color, val focus: Color, val scrim: Color, val isDark: Boolean,
)

val LightColors = TuulitColors(
    cream = Color(0xFFF6F0E4), oat = Color(0xFFFBF7EF), sand = Color(0xFFECE2D0), tan = Color(0xFFD6BE98),
    hairline = Color(0xFFDDD0BA), ink = Color(0xFF221E1A), inkMuted = Color(0xFF5E554A), inkFaint = Color(0xFF736859),
    onInk = Color(0xFFFBF7EF), toffee = Color(0xFF8A5A2B), banana = Color(0xFFF2C230), onBanana = Color(0xFF221E1A),
    bananaSoft = Color(0xFFF7E3A3), period = Color(0xFFA64A33), periodSoft = Color(0xFFF2D9D0), fertile = Color(0xFF4F6577),
    fertileSoft = Color(0xFFDDE3E6), alert = Color(0xFF8E2A2A), alertSoft = Color(0xFFF3DAD6), focus = Color(0xFF221E1A),
    scrim = Color(0x52221E1A), isDark = false,
)

val DarkColors = TuulitColors(
    cream = Color(0xFF171411), oat = Color(0xFF201C18), sand = Color(0xFF2B2620), tan = Color(0xFF4A3F33),
    hairline = Color(0xFF3A332B), ink = Color(0xFFF3ECE1), inkMuted = Color(0xFFBDB2A3), inkFaint = Color(0xFF968B7C),
    onInk = Color(0xFF171411), toffee = Color(0xFFD9A66E), banana = Color(0xFFF2C94A), onBanana = Color(0xFF221E1A),
    bananaSoft = Color(0xFF3A3016), period = Color(0xFFE0806F), periodSoft = Color(0xFF3D231E), fertile = Color(0xFF9DB2C4),
    fertileSoft = Color(0xFF222A31), alert = Color(0xFFEF8A80), alertSoft = Color(0xFF3D2221), focus = Color(0xFFF2C94A),
    scrim = Color(0x99000000), isDark = true,
)

/** Mascot art colours are fixed in both themes; the mark is a picture, not UI. */
object MascotColors {
    val Banana = Color(0xFFF2C230)
    val Tan = Color(0xFFD6BE98)
    val Flesh = Color(0xFFFBF7EF)
    val Outline = Color(0xFF221E1A)
    val Prop = Color(0xFFA64A33)
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

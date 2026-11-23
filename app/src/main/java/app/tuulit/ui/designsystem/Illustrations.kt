package app.tuulit.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tuulit.domain.CyclePhase
import app.tuulit.domain.CyclePrediction
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Tuulit illustration system: flat, rounded shapes from the palette, no outlines (except the
 * mascot), at most three colours per picture. Every illustration is drawn in code so it scales,
 * themes and never needs image assets.
 */

/** Phase colours: menstrual coral, follicular peach, ovulation lilac, luteal dusty rose. */
data class PhaseStyle(val fill: Color, val soft: Color, val label: String)

@Composable
fun phaseStyle(phase: CyclePhase): PhaseStyle {
    val c = Tuulit.colors
    return when (phase) {
        CyclePhase.MENSTRUAL -> PhaseStyle(c.period, c.periodSoft, "Period")
        CyclePhase.FOLLICULAR -> PhaseStyle(c.peach, c.peachSoft, "Follicular")
        CyclePhase.ESTIMATED_OVULATION -> PhaseStyle(c.fertile, c.fertileSoft, "Ovulation")
        CyclePhase.LUTEAL -> PhaseStyle(c.rose, c.roseSoft, "Luteal")
        CyclePhase.UNKNOWN -> PhaseStyle(c.banana, c.bananaSoft, "Getting started")
    }
}

/** A soft organic blob, used behind heroes and illustrations. */
@Composable
fun Blob(color: Color, modifier: Modifier, seed: Int = 0) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val wob = listOf(0.06f, -0.04f, 0.05f, -0.06f, 0.03f, -0.05f).let { l -> l.drop(seed % l.size) + l.take(seed % l.size) }
        val p = Path()
        val n = 6
        val pts = (0 until n).map { i ->
            val a = (i.toFloat() / n) * 2f * PI.toFloat()
            val r = 0.5f + wob[i]
            Offset(w / 2 + cos(a) * w * r, h / 2 + sin(a) * h * r)
        }
        p.moveTo((pts[0].x + pts[n - 1].x) / 2, (pts[0].y + pts[n - 1].y) / 2)
        for (i in 0 until n) {
            val cur = pts[i]; val nxt = pts[(i + 1) % n]
            p.quadraticTo(cur.x, cur.y, (cur.x + nxt.x) / 2, (cur.y + nxt.y) / 2)
        }
        p.close()
        drawPath(p, color)
    }
}

/**
 * The cycle ring: one arc per day of the cycle, tinted by phase. Logged period days are solid
 * coral, predicted ones a soft coral, the fertile estimate lilac, and today a banana knob.
 */
@Composable
fun CycleRing(
    prediction: CyclePrediction,
    cycleStart: Long?,
    today: Long,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Tuulit.colors
    val length = prediction.averageCycleLength.coerceIn(21, 45)
    val start = cycleStart ?: (today - ((prediction.currentCycleDay ?: 1) - 1))
    val track = c.sand
    val desc = "Cycle day ${prediction.currentCycleDay ?: "unknown"} of about $length"
    Box(modifier.size(size).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.085f
            val radius = this.size.minDimension / 2 - stroke
            val gap = 1.6f
            val sweep = 360f / length
            for (i in 0 until length) {
                val day = start + i
                val color = when {
                    day in prediction.loggedPeriodDays -> c.period
                    day in prediction.predictedPeriodDays -> c.periodSoft
                    day == prediction.estimatedOvulationDay -> c.fertile
                    day in prediction.estimatedFertileDays -> c.fertileSoft
                    day < today -> c.tan
                    else -> track
                }
                drawArc(color, -90f + i * sweep + gap / 2, sweep - gap, false,
                    topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Butt))
            }
            val idx = (today - start).toInt().coerceIn(0, length - 1)
            val ang = Math.toRadians((-90.0 + (idx + 0.5) * sweep)).toFloat()
            val knob = Offset(center.x + cos(ang) * radius, center.y + sin(ang) * radius)
            drawCircle(c.oat, stroke * 0.95f, knob)
            drawCircle(c.banana, stroke * 0.72f, knob)
        }
        content()
    }
}

/** Five pastel mood blobs with drawn faces, rough to great. */
@Composable
fun moodColor(score: Int): Color {
    val c = Tuulit.colors
    return when (score) { 1 -> c.fertileSoft; 2 -> c.roseSoft; 3 -> c.sand; 4 -> c.peachSoft; else -> c.bananaSoft }
}

@Composable
fun moodColorStrong(score: Int): Color {
    val c = Tuulit.colors
    return when (score) { 1 -> c.fertile; 2 -> c.rose; 3 -> c.tan; 4 -> c.peach; else -> c.banana }
}

// ------------------------------------------------------------------ Care illustrations

enum class Illo { HEAT, BREATH, STRETCH, MOON, HEART, SUN, LEAF_CUP, SPARK }

fun illoFor(icon: String): Illo = when (icon) {
    "heat" -> Illo.HEAT; "breath" -> Illo.BREATH; "stretch" -> Illo.STRETCH; "moon" -> Illo.MOON
    "care" -> Illo.HEART; "food" -> Illo.LEAF_CUP; "spark" -> Illo.SPARK; else -> Illo.SUN
}

/** Small flat illustrations for Care. [bg] is the blob behind the picture. */
@Composable
fun Illustration(kind: Illo, size: Dp, modifier: Modifier = Modifier, bg: Color? = null) {
    val c = Tuulit.colors
    val back = bg ?: c.peachSoft
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawCircle(back, s * 0.46f, center)
        when (kind) {
            Illo.HEAT -> {
                drawRoundRect(c.period, Offset(s * 0.30f, s * 0.30f), Size(s * 0.40f, s * 0.46f), CornerRadius(s * 0.14f))
                drawRoundRect(c.periodDeep, Offset(s * 0.42f, s * 0.20f), Size(s * 0.16f, s * 0.12f), CornerRadius(s * 0.04f))
                wave(this, s * 0.36f, s * 0.48f, s * 0.28f, c.periodSoft, s)
                wave(this, s * 0.36f, s * 0.60f, s * 0.28f, c.periodSoft, s)
            }
            Illo.BREATH -> {
                drawCircle(c.fertileSoft, s * 0.30f, center)
                drawCircle(c.fertile, s * 0.20f, center)
                drawCircle(c.oat, s * 0.09f, center)
            }
            Illo.STRETCH -> {
                // a soft lotus: three petals and a sun drop
                rotate(-35f, center) { drawOval(c.peach, Offset(center.x - s * 0.09f, center.y - s * 0.30f), Size(s * 0.18f, s * 0.36f)) }
                rotate(35f, center) { drawOval(c.peach, Offset(center.x - s * 0.09f, center.y - s * 0.30f), Size(s * 0.18f, s * 0.36f)) }
                drawOval(c.rose, Offset(center.x - s * 0.10f, center.y - s * 0.32f), Size(s * 0.20f, s * 0.40f))
                drawRoundRect(c.toffee.copy(alpha = 0.35f), Offset(s * 0.24f, s * 0.70f), Size(s * 0.52f, s * 0.05f), CornerRadius(s * 0.03f))
                drawCircle(c.banana, s * 0.05f, Offset(center.x, s * 0.18f))
            }
            Illo.MOON -> {
                drawCircle(c.banana, s * 0.24f, center)
                drawCircle(back, s * 0.20f, Offset(center.x + s * 0.11f, center.y - s * 0.08f))
                drawCircle(c.oat, s * 0.025f, Offset(s * 0.70f, s * 0.30f))
                drawCircle(c.oat, s * 0.018f, Offset(s * 0.30f, s * 0.70f))
            }
            Illo.HEART -> heart(this, center, s * 0.26f, c.rose)
            Illo.SUN -> {
                for (i in 0 until 8) {
                    rotate(i * 45f) { drawRoundRect(c.banana, Offset(center.x - s * 0.025f, s * 0.12f), Size(s * 0.05f, s * 0.12f), CornerRadius(s * 0.03f)) }
                }
                drawCircle(c.banana, s * 0.18f, center)
            }
            Illo.LEAF_CUP -> {
                drawRoundRect(c.peach, Offset(s * 0.28f, s * 0.42f), Size(s * 0.40f, s * 0.30f), CornerRadius(s * 0.12f))
                drawArc(c.peach, -90f, 180f, false, Offset(s * 0.60f, s * 0.46f), Size(s * 0.16f, s * 0.16f), style = Stroke(s * 0.05f))
                wave(this, s * 0.38f, s * 0.30f, s * 0.14f, c.rose, s)
            }
            Illo.SPARK -> spark(this, center, s * 0.26f, c.banana)
        }
    }
}

private fun wave(d: DrawScope, x: Float, y: Float, w: Float, color: Color, s: Float) {
    val p = Path().apply {
        moveTo(x, y)
        quadraticTo(x + w * 0.25f, y - s * 0.04f, x + w * 0.5f, y)
        quadraticTo(x + w * 0.75f, y + s * 0.04f, x + w, y)
    }
    d.drawPath(p, color, style = Stroke(s * 0.035f, cap = StrokeCap.Round))
}

fun heart(d: DrawScope, c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y + r * 0.9f)
        cubicTo(c.x - r * 1.4f, c.y + r * 0.1f, c.x - r * 0.9f, c.y - r * 1.1f, c.x, c.y - r * 0.4f)
        cubicTo(c.x + r * 0.9f, c.y - r * 1.1f, c.x + r * 1.4f, c.y + r * 0.1f, c.x, c.y + r * 0.9f)
        close()
    }
    d.drawPath(p, color)
}

fun spark(d: DrawScope, c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x + r * 0.12f, c.y - r * 0.12f, c.x + r, c.y)
        quadraticTo(c.x + r * 0.12f, c.y + r * 0.12f, c.x, c.y + r)
        quadraticTo(c.x - r * 0.12f, c.y + r * 0.12f, c.x - r, c.y)
        quadraticTo(c.x - r * 0.12f, c.y - r * 0.12f, c.x, c.y - r)
        close()
    }
    d.drawPath(p, color)
}

// ------------------------------------------------------------------ Game artwork

/** Each game has its own background colour and artwork, so Play never looks like a grid of identical tiles. */
data class GameLook(val bg: Color, val ink: Color)

@Composable
fun gameLook(id: String): GameLook {
    val c = Tuulit.colors
    return when (id) {
        "banana_catch" -> GameLook(c.banana, c.onBanana)
        "mood_match" -> GameLook(c.peach, c.ink)
        "memory_flip" -> GameLook(c.fertile, c.ink)
        "brain_break" -> GameLook(c.bananaSoft, c.ink)
        "word_puzzle" -> GameLook(c.peachSoft, c.ink)
        "calm_tap" -> GameLook(c.fertileSoft, c.ink)
        "focus_game" -> GameLook(c.roseSoft, c.ink)
        "stress_smash" -> GameLook(c.period, c.onPeriod)
        else -> GameLook(c.sand, c.ink)
    }
}

@Composable
fun GameArt(id: String, modifier: Modifier) {
    val c = Tuulit.colors
    val faces = remember { listOf(5, 1, 4, 2) }
    Canvas(modifier) {
        val w = size.width; val h = size.height; val s = minOf(w, h)
        when (id) {
            "banana_catch" -> {
                // falling bananas and a basket
                fun banana(x: Float, y: Float, rot: Float, sc: Float) = rotate(rot, Offset(x, y)) {
                    val p = Path().apply {
                        moveTo(x - s * 0.13f * sc, y - s * 0.06f * sc)
                        quadraticTo(x - s * 0.02f * sc, y + s * 0.18f * sc, x + s * 0.15f * sc, y - s * 0.03f * sc)
                        quadraticTo(x - s * 0.01f * sc, y + s * 0.04f * sc, x - s * 0.13f * sc, y - s * 0.06f * sc)
                        close()
                    }
                    drawPath(p, Color(0xFFFFE27A)); drawPath(p, c.ink.copy(alpha = 0.8f), style = Stroke(s * 0.011f))
                    drawCircle(c.toffee, s * 0.014f * sc, Offset(x + s * 0.15f * sc, y - s * 0.03f * sc))
                }
                banana(w * 0.28f, h * 0.30f, -20f, 1.1f); banana(w * 0.66f, h * 0.22f, 25f, 0.9f); banana(w * 0.52f, h * 0.52f, 10f, 1f)
                // a woven basket: a half bowl with a rim
                drawArc(c.toffee, 0f, 180f, true, Offset(w * 0.30f, h * 0.62f), Size(w * 0.40f, h * 0.30f))
                drawRoundRect(c.periodDeep, Offset(w * 0.28f, h * 0.74f), Size(w * 0.44f, h * 0.05f), CornerRadius(h * 0.025f))
            }
            "mood_match" -> {
                faces.forEachIndexed { i, f ->
                    val x = w * (0.22f + (i % 2) * 0.32f) + (i / 2) * w * 0.12f; val y = h * (0.30f + (i / 2) * 0.38f)
                    drawRoundRect(if (i % 2 == 0) c.oat else c.peachSoft, Offset(x - s * 0.13f, y - s * 0.16f), Size(s * 0.26f, s * 0.32f), CornerRadius(s * 0.05f))
                    face(this, Offset(x, y), s * 0.08f, f, c.ink)
                }
            }
            "memory_flip" -> {
                for (i in 0 until 3) for (j in 0 until 3) {
                    val lit = (i + j) % 3 == 0
                    drawRoundRect(if (lit) c.banana else c.fertileSoft, Offset(w * 0.18f + i * w * 0.23f, h * 0.16f + j * h * 0.24f),
                        Size(w * 0.19f, h * 0.20f), CornerRadius(s * 0.04f))
                }
            }
            "brain_break" -> {
                drawCircle(c.banana, s * 0.30f, Offset(w * 0.5f, h * 0.5f))
                drawLine(c.ink, Offset(w * 0.5f - s * 0.12f, h * 0.5f), Offset(w * 0.5f + s * 0.12f, h * 0.5f), s * 0.035f, StrokeCap.Round)
                drawLine(c.ink, Offset(w * 0.5f, h * 0.5f - s * 0.12f), Offset(w * 0.5f, h * 0.5f + s * 0.12f), s * 0.035f, StrokeCap.Round)
                drawCircle(c.peach, s * 0.08f, Offset(w * 0.20f, h * 0.22f)); drawCircle(c.rose, s * 0.06f, Offset(w * 0.82f, h * 0.78f))
            }
            "word_puzzle" -> {
                listOf(c.peach, c.banana, c.fertileSoft, c.roseSoft).forEachIndexed { i, col ->
                    drawRoundRect(col, Offset(w * 0.12f + i * w * 0.2f, h * 0.36f + (if (i % 2 == 0) -h * 0.05f else h * 0.05f)),
                        Size(w * 0.17f, w * 0.17f), CornerRadius(s * 0.04f))
                }
            }
            "calm_tap" -> {
                drawCircle(c.oat, s * 0.38f, Offset(w / 2, h / 2), style = Stroke(s * 0.02f))
                drawCircle(c.fertile.copy(alpha = 0.45f), s * 0.30f, Offset(w / 2, h / 2))
                drawCircle(c.fertile, s * 0.18f, Offset(w / 2, h / 2))
            }
            "focus_game" -> {
                drawCircle(c.rose, s * 0.08f, Offset(w * 0.22f, h * 0.28f)); drawCircle(c.peach, s * 0.07f, Offset(w * 0.78f, h * 0.70f))
                drawRoundRect(c.fertile, Offset(w * 0.68f, h * 0.18f), Size(s * 0.14f, s * 0.14f), CornerRadius(s * 0.03f))
                drawCircle(c.oat, s * 0.20f, Offset(w * 0.46f, h * 0.52f)); drawCircle(c.banana, s * 0.12f, Offset(w * 0.46f, h * 0.52f))
            }
            "stress_smash" -> {
                for (i in 0 until 3) for (j in 0 until 2) {
                    val crushed = (i + j) % 2 == 1
                    drawRoundRect(if (crushed) c.periodSoft else c.oat, Offset(w * 0.16f + i * w * 0.24f, h * 0.24f + j * h * 0.30f),
                        Size(w * 0.20f, h * 0.24f), CornerRadius(s * 0.05f))
                }
                spark(this, Offset(w * 0.78f, h * 0.18f), s * 0.09f, c.banana)
            }
        }
    }
}

/** A simple filled face used by Mood Match art and mood blobs. */
fun face(d: DrawScope, c: Offset, r: Float, score: Int, ink: Color) {
    d.drawCircle(ink, r * 0.14f, Offset(c.x - r * 0.38f, c.y - r * 0.18f))
    d.drawCircle(ink, r * 0.14f, Offset(c.x + r * 0.38f, c.y - r * 0.18f))
    val curve = when (score) { 1 -> -0.30f; 2 -> -0.14f; 3 -> 0f; 4 -> 0.18f; else -> 0.34f }
    val p = Path().apply {
        moveTo(c.x - r * 0.42f, c.y + r * 0.30f)
        quadraticTo(c.x, c.y + r * (0.30f + curve * 1.6f), c.x + r * 0.42f, c.y + r * 0.30f)
    }
    d.drawPath(p, ink, style = Stroke(r * 0.13f, cap = StrokeCap.Round))
    if (score == 5) {
        d.drawCircle(Color(0x55F49A8C), r * 0.16f, Offset(c.x - r * 0.62f, c.y + r * 0.10f))
        d.drawCircle(Color(0x55F49A8C), r * 0.16f, Offset(c.x + r * 0.62f, c.y + r * 0.10f))
    }
}

/** Big mood blob for check-ins: pastel circle with a filled face. */
@Composable
fun MoodBlob(score: Int, selected: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val c = Tuulit.colors
    val fill = if (selected) moodColorStrong(score) else moodColor(score)
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(fill, r * (if (selected) 1f else 0.86f))
        if (selected) drawCircle(c.ink, r * 0.98f, style = Stroke(r * 0.06f))
        face(this, center, r * 0.55f, score, c.ink)
    }
}

package app.tuulit.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tuulit.domain.BananaModePersona

/** The seven Banana Mode moods. The silhouette never changes; only the face and one prop do. */
enum class MascotMood { WARM, CHILL, CRAMPS, SLEEPY, DRAMA, GYM, HAPPY }

fun BananaModePersona.mood(): MascotMood = when (this) {
    BananaModePersona.NEUTRAL -> MascotMood.WARM
    BananaModePersona.CHILL -> MascotMood.CHILL
    BananaModePersona.CRAMPS -> MascotMood.CRAMPS
    BananaModePersona.SLEEPY -> MascotMood.SLEEPY
    BananaModePersona.DRAMA -> MascotMood.DRAMA
    BananaModePersona.GYM -> MascotMood.GYM
    BananaModePersona.HAPPY -> MascotMood.HAPPY
}

private fun p(d: String): Path = PathParser().parsePathString(d).toPath()

/** Body paths in a 120×120 viewBox, identical to the design system's vector mascot. */
private object Body {
    val backLeft = p("M54 63C40 55 28 63 26 86c7 0 12-12 26-15z")
    val backRight = p("M66 63c12-9 24-6 27 7-7 0-12-4-25 1z")
    val flesh = p("M51 66C47 44 50 22 62 13c6-4 10 0 8 9-2 14-4 30-4 44z")
    val body = p("M49 65c-1 22 13 38 37 41l4-7c-17-6-24-18-24-34z")
    val stem = p("M86 99l5 1.5-2 7-5-1.5z")
    val peelLeft = p("M57 64c-14 0-22 14-17 32 7 1 9-13 18-22z")
    val crease1 = p("M45 76c-1 5-1 9 0 13")
    val peelRight = p("M62 64c12-6 25 0 26 16-7 1-9-8-23-8z")
    val crease2 = p("M72 66c4 0 8 2 10 6")
    val headband = p("M51.5 28.5l20-3.6.8 5-20 3.6z")
}

@Composable
fun Mascot(mood: MascotMood, size: Dp, modifier: Modifier = Modifier) {
    val face = remember(mood) { faceFor(mood) }
    Canvas(modifier.size(size).semantics { contentDescription = "Tuulit banana, ${mood.name.lowercase()} mood" }) {
        val s = this.size.minDimension / 120f
        scale(s, s, pivot = Offset.Zero) {
            val outline = Stroke(width = 2.6f, join = StrokeJoin.Round, cap = StrokeCap.Round)
            fun shape(path: Path, fill: Color) { drawPath(path, fill); drawPath(path, MascotColors.Outline, style = outline) }
            shape(Body.backLeft, MascotColors.Tan)
            shape(Body.backRight, MascotColors.Tan)
            shape(Body.flesh, MascotColors.Flesh)
            drawCircle(MascotColors.Outline, 3f, Offset(66.5f, 13.5f))
            shape(Body.body, MascotColors.Banana)
            shape(Body.stem, MascotColors.Outline)
            shape(Body.peelLeft, MascotColors.Banana)
            drawPath(Body.crease1, MascotColors.Outline, style = Stroke(1.6f, cap = StrokeCap.Round))
            shape(Body.peelRight, MascotColors.Banana)
            drawPath(Body.crease2, MascotColors.Outline, style = Stroke(1.6f, cap = StrokeCap.Round))
            if (mood == MascotMood.GYM) shape(Body.headband, MascotColors.Prop)
            if (mood == MascotMood.CRAMPS) {
                val wrap = p("M51 58h18a5 5 0 0 1 0 10h-18a5 5 0 0 1 0-10z")
                shape(wrap, MascotColors.Prop)
            }
            face(this)
        }
    }
}

private fun faceFor(mood: MascotMood): DrawScope.() -> Unit {
    val k = MascotColors.Outline
    val line = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun DrawScope.stroke(d: String) = drawPath(p(d), k, style = line)
    return when (mood) {
        MascotMood.WARM, MascotMood.GYM -> {
            val dy = if (mood == MascotMood.GYM) 1f else 0f
            ({
                drawCircle(k, 2.1f, Offset(57f, 37f + dy)); drawCircle(k, 2.1f, Offset(64.5f, 36f + dy))
                stroke(if (mood == MascotMood.GYM) "M57.5 43q3.8 3.6 7.4-.6" else "M57.5 42.5q3.8 3.4 7.4-.6")
            })
        }
        MascotMood.CHILL -> ({ stroke("M54.6 37q2.4 2.4 4.8 0M62 36q2.4 2.4 4.8 0"); stroke("M58.5 43q2.5 1.8 5 0") })
        MascotMood.CRAMPS -> ({ stroke("M55 38l4-2M62.5 36l4 2"); stroke("M57 44q1.7-2 3.4 0t3.4 0t3 0") })
        MascotMood.SLEEPY -> ({
            stroke("M54.8 37.5h4.4M62.2 36.5h4.4")
            drawCircle(k, 1.8f, Offset(61f, 43.5f), style = Stroke(1.8f))
            stroke("M80 14h8l-8 9h8M90 5h5l-5 6h5")
        })
        MascotMood.DRAMA -> ({
            drawCircle(k, 2.2f, Offset(57f, 37.5f)); drawCircle(k, 2.2f, Offset(64.5f, 36.5f))
            stroke("M62 31.5l6-1.6"); stroke("M57.5 43.5q4.5 1 7.5-2.6")
        })
        MascotMood.HAPPY -> ({
            stroke("M54.6 38q2.4-3 4.8 0M62 37q2.4-3 4.8 0")
            val mouth = p("M56.5 42q4.5 5 9 0")
            drawPath(mouth, MascotColors.Prop); drawPath(mouth, k, style = line)
            drawPath(p("M26 18v10M21 23h10"), MascotColors.Banana, style = Stroke(2.6f, cap = StrokeCap.Round))
        })
    }
}

/** Five line-drawn faces for the check-in, 1 = rough ... 5 = great. */
@Composable
fun MoodFace(score: Int, color: Color, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    val mouth = when (score.coerceIn(1, 5)) {
        1 -> "M9 17q3-3 6 0"; 2 -> "M9 16.5q3-1.5 6 0"; 3 -> "M9 16h6"; 4 -> "M9 15.5q3 1.8 6 0"; else -> "M8.5 15q3.5 3.5 7 0"
    }
    val mouthPath = remember(score) { p(mouth) }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            val st = Stroke(1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawCircle(color, 8.5f, Offset(12f, 12f), style = st)
            drawCircle(color, 0.9f, Offset(9.3f, 10.2f)); drawCircle(color, 0.9f, Offset(14.7f, 10.2f))
            drawPath(mouthPath, color, style = st)
        }
    }
}

val moodFaceLabels = listOf("Rough", "Low", "Okay", "Good", "Great")

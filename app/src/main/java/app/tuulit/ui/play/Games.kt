package app.tuulit.ui.play

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tuulit.domain.PlayContent
import app.tuulit.ui.FixedScreen
import app.tuulit.ui.TuulitSheet
import app.tuulit.ui.VSpace
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.IconBtn
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotColors
import app.tuulit.ui.designsystem.MascotMood
import app.tuulit.ui.designsystem.MoodFace
import app.tuulit.ui.designsystem.Overline
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/** Ends a round: saves the score and shows the result sheet. */
class RoundState {
    var finished by mutableStateOf(false)
    var score by mutableIntStateOf(0)
    var newBest by mutableStateOf(false)
    var line by mutableStateOf("")
    var round by mutableIntStateOf(0)
}

@Composable
fun GameScreen(id: String, onBack: () -> Unit) {
    val info = PlayContent.games.firstOrNull { it.id == id } ?: run { onBack(); return }
    val c = container()
    val scope = rememberCoroutineScope()
    val round = remember { RoundState() }
    var best by remember { mutableIntStateOf(0) }
    var liveScore by remember { mutableIntStateOf(0) }
    LaunchedEffect(round.round) { best = c.carePlay.bestScore(id); liveScore = 0 }
    val finish: (Int, String) -> Unit = { score, line ->
        if (!round.finished) {
            round.score = score; round.line = line; round.finished = true
            scope.launch { round.newBest = c.carePlay.recordGame(id, score) }
        }
    }
    FixedScreen {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(TuulitIcons.Close, "Close game", onBack)
            T(info.title, Tuulit.type.label, modifier = Modifier.weight(1f), align = TextAlign.Center)
            T("$liveScore", Tuulit.type.headline, modifier = Modifier.semantics { contentDescription = "Score $liveScore" })
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            T(info.blurb, Tuulit.type.caption, Tuulit.colors.inkMuted, Modifier.weight(1f))
            if (best > 0) T("Best $best", Tuulit.type.caption, Tuulit.colors.inkMuted)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(round.round) {
                when (id) {
                    "banana_catch" -> BananaCatch(onScore = { liveScore = it }, onEnd = finish)
                    "mood_match" -> MoodMatch(onScore = { liveScore = it }, onEnd = finish)
                    "memory_flip" -> MemoryFlip(onScore = { liveScore = it }, onEnd = finish)
                    "brain_break" -> BrainBreak(onScore = { liveScore = it }, onEnd = finish)
                    "word_puzzle" -> WordPuzzle(onScore = { liveScore = it }, onEnd = finish)
                    "calm_tap" -> CalmTap(onScore = { liveScore = it }, onEnd = finish)
                    "focus_game" -> FocusGame(onScore = { liveScore = it }, onEnd = finish)
                    "stress_smash" -> StressSmash(onScore = { liveScore = it }, onEnd = finish)
                }
            }
        }
    }
    if (round.finished) {
        TuulitSheet(onDismiss = onBack) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(MascotMood.HAPPY, 96.dp)
                if (round.newBest) Overline("New personal best", Tuulit.colors.toffee, Modifier.padding(top = 6.dp))
                T("${round.score}", Tuulit.type.numeral, modifier = Modifier.padding(top = 4.dp))
                T(round.line, Tuulit.type.body, Tuulit.colors.inkMuted, Modifier.padding(top = 4.dp, bottom = 20.dp), align = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TButton("Back to Play", onBack, Modifier.weight(1f), kind = ButtonKind.QUIET)
                    TButton("Play again", { round.finished = false; round.newBest = false; round.round++ }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun key(k: Int, content: @Composable () -> Unit) = androidx.compose.runtime.key(k) { content() }

// ---------------------------------------------------------------- Banana Catch

private data class Faller(val id: Long, var x: Float, var y: Float, val rock: Boolean, val speed: Float)

@Composable
private fun BananaCatch(onScore: (Int) -> Unit, onEnd: (Int, String) -> Unit) {
    val col = Tuulit.colors
    val view = LocalView.current
    val items = remember { mutableStateListOf<Faller>() }
    var basketX by remember { mutableFloatStateOf(0.5f) }
    var lives by remember { mutableIntStateOf(3) }
    var score by remember { mutableIntStateOf(0) }
    var caught by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        var last = 0L; var spawn = 0f; var nextId = 0L; var elapsed = 0f
        while (isActive && lives > 0) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now; elapsed += dt; spawn -= dt
                val difficulty = 1f + elapsed / 45f
                if (spawn <= 0f) {
                    items.add(Faller(nextId++, Random.nextFloat() * 0.84f + 0.08f, -0.05f, Random.nextFloat() < 0.28f, (0.22f + Random.nextFloat() * 0.1f) * difficulty))
                    spawn = (0.9f / difficulty).coerceAtLeast(0.35f)
                }
                val it = items.iterator()
                while (it.hasNext()) {
                    val f = it.next()
                    f.y += f.speed * dt
                    if (f.y >= 0.88f && f.y <= 0.96f && abs(f.x - basketX) < 0.12f) {
                        it.remove()
                        if (f.rock) { lives--; view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
                        else { score += 10; caught++; onScore(score) }
                    } else if (f.y > 1.05f) it.remove()
                }
                tick++
            }
        }
        onEnd(score, "You caught $caught ${if (caught == 1) "banana" else "bananas"}. That's a nice two minutes off.")
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(bottom = 8.dp).semantics { contentDescription = "$lives lives left" }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i -> Icon(TuulitIcons.Drop, null, Modifier.size(18.dp), tint = if (i < lives) col.period else col.tan) }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(col.oat)
            .pointerInput(Unit) { detectDragGestures { change, _ -> basketX = (change.position.x / size.width).coerceIn(0.08f, 0.92f) } }
            .pointerInput(Unit) { detectTapGestures { pos -> basketX = (pos.x / size.width).coerceIn(0.08f, 0.92f) } }) {
            val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
            Canvas(Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") tick
                items.forEach { f ->
                    val cx = f.x * w; val cy = f.y * h
                    if (f.rock) {
                        drawOval(col.tan, Offset(cx - 16f * density, cy - 12f * density), Size(32f * density, 24f * density))
                        drawOval(col.ink, Offset(cx - 16f * density, cy - 12f * density), Size(32f * density, 24f * density), style = Stroke(2f * density))
                    } else {
                        drawArc(MascotColors.Banana, 20f, 140f, false, Offset(cx - 18f * density, cy - 22f * density), Size(36f * density, 36f * density),
                            style = Stroke(9f * density, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                }
                val bw = 96f * density
                drawRoundRect(col.ink, Offset(basketX * w - bw / 2, h * 0.92f), Size(bw, 30f * density), CornerRadius(22f * density))
            }
            T("Drag anywhere to move", Tuulit.type.caption, col.inkFaint, Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
        }
    }
}

// ---------------------------------------------------------------- Mood Match

private val matchSymbols: List<ImageVector> = listOf(TuulitIcons.Heat, TuulitIcons.Moon, TuulitIcons.Music, TuulitIcons.Care,
    TuulitIcons.Breath, TuulitIcons.Book, TuulitIcons.Food, TuulitIcons.Spark)

@Composable
private fun MoodMatch(onScore: (Int) -> Unit, onEnd: (Int, String) -> Unit) {
    val deck = remember { (matchSymbols.indices + matchSymbols.indices).shuffled() }
    val up = remember { mutableStateListOf<Int>() }
    val matched = remember { mutableStateListOf<Int>() }
    var moves by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf("Find the pairs. Take your time.") }
    LaunchedEffect(up.size) {
        if (up.size == 2) {
            moves++; onScore(moves)
            val (a, b) = up[0] to up[1]
            if (deck[a] == deck[b]) { matched += a; matched += b; up.clear(); hint = "A pair." }
            else { hint = "Not a pair. They flip back in a moment."; delay(800); up.clear() }
            if (matched.size == deck.size) onEnd((120 - moves * 4).coerceAtLeast(20), "All pairs found in $moves moves.")
        }
    }
    Column(Modifier.fillMaxSize()) {
        T("${matched.size / 2} of ${deck.size / 2} pairs", Tuulit.type.caption, Tuulit.colors.inkMuted, Modifier.padding(bottom = 14.dp))
        deck.indices.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { i ->
                    val c = Tuulit.colors
                    val isUp = i in up; val isMatched = i in matched
                    Box(Modifier.weight(1f).aspectRatio(0.75f).clip(RoundedCornerShape(10.dp))
                        .background(if (isMatched) c.ink else if (isUp) c.oat else c.tan)
                        .then(if (isUp) Modifier.border(2.dp, c.ink, RoundedCornerShape(10.dp)) else Modifier)
                        .clickable(enabled = !isUp && !isMatched && up.size < 2, role = Role.Button) { up += i }
                        .semantics { contentDescription = if (isMatched || isUp) "Card ${deck[i] + 1}" else "Face-down card" },
                        contentAlignment = Alignment.Center) {
                        if (isUp || isMatched) Icon(matchSymbols[deck[i]], null, Modifier.size(28.dp), tint = if (isMatched) c.onInk else c.ink)
                    }
                }
            }
        }
        Box(Modifier.weight(1f))
        T(hint, Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------- Memory Flip (sequence memory)

@Composable
private fun MemoryFlip(onScore: (Int) -> Unit, onEnd: (Int, String) -> Unit) {
    val col = Tuulit.colors
    val sequence = remember { mutableStateListOf(Random.nextInt(9)) }
    var showing by remember { mutableStateOf(true) }
    var lit by remember { mutableIntStateOf(-1) }
    var input by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Watch the order.") }
    LaunchedEffect(sequence.size, showing) {
        if (!showing) return@LaunchedEffect
        delay(700)
        for (t in sequence) { lit = t; delay(550); lit = -1; delay(250) }
        showing = false; input = 0; message = "Your turn: repeat the order."
    }
    Column(Modifier.fillMaxSize()) {
        T("Round ${sequence.size}", Tuulit.type.caption, col.inkMuted, Modifier.padding(bottom = 14.dp))
        (0 until 9).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { i ->
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(if (lit == i) col.banana else col.sand)
                        .clickable(enabled = !showing, role = Role.Button) {
                            lit = i
                            if (sequence[input] == i) {
                                input++
                                if (input == sequence.size) {
                                    onScore(sequence.size * 10); message = "Yes. One more this time."
                                    sequence += Random.nextInt(9); showing = true
                                }
                            } else {
                                onEnd((sequence.size - 1) * 10, "You remembered a sequence of ${sequence.size - 1}.")
                            }
                        }.semantics { contentDescription = "Tile ${i + 1}" })
                }
            }
        }
        LaunchedEffect(lit, showing) { if (!showing && lit >= 0) { delay(200); lit = -1 } }
        Box(Modifier.weight(1f))
        T(message, Tuulit.type.bodySm, col.inkMuted, Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------- Brain Break (gentle sums)

private data class Sum(val text: String, val answer: Int, val options: List<Int>)

private fun newSum(level: Int): Sum {
    val max = 10 + level * 3
    val a = Random.nextInt(2, max); val b = Random.nextInt(1, max)
    val plus = Random.nextBoolean() || a < b
    val ans = if (plus) a + b else a - b
    val opts = (setOf(ans) + generateSequence { ans + Random.nextInt(-5, 6) }.filter { it != ans && it >= 0 }.take(3)).shuffled()
    return Sum(if (plus) "$a + $b" else "$a − $b", ans, opts)
}

@Composable
private fun BrainBreak(onScore: (Int) -> Unit, onEnd: (Int, String) -> Unit) {
    var level by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf(newSum(0)) }
    var score by remember { mutableIntStateOf(0) }
    var right by remember { mutableIntStateOf(0) }
    var asked by remember { mutableIntStateOf(1) }
    var seconds by remember { mutableIntStateOf(60) }
    var feedback by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (isActive && seconds > 0) { delay(1000); seconds-- }
        onEnd(score, "$right right out of ${asked - 1}. Your brain had a little workout.")
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        T("$seconds s", Tuulit.type.caption, Tuulit.colors.inkMuted, Modifier.align(Alignment.Start))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { T(q.text, Tuulit.type.numeral) }
        T(feedback, Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(bottom = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
        q.options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { o ->
                    TButton("$o", {
                        if (o == q.answer) { score += 10; right++; onScore(score); feedback = "Right."; if (right % 4 == 0) level++ }
                        else feedback = "It was ${q.answer}. Next one."
                        asked++; q = newSum(level)
                    }, Modifier.weight(1f).height(64.dp), kind = ButtonKind.QUIET)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Word Puzzle

@Composable
private fun WordPuzzle(onScore: (Int) -> Unit, onEnd: (Int, String) -> Unit) {
    val words = remember { PlayContent.words.shuffled().take(5) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var solved by remember { mutableIntStateOf(0) }
    val (word, hint) = words[index]
    val letters = remember(index) { word.toList().shuffled().let { if (it.joinToString("") == word) it.reversed() else it } }
    val picked = remember(index) { mutableStateListOf<Int>() }
    var hintUsed by remember(index) { mutableStateOf(false) }
    var feedback by remember(index) { mutableStateOf("") }
    fun next() { if (index < words.lastIndex) index++ else onEnd(score, "You solved $solved of ${words.size} words.") }
    val guess = picked.joinToString("") { letters[it].toString() }
    LaunchedEffect(guess) {
        if (guess.length == word.length) {
            if (guess == word) {
                score += if (hintUsed) 15 else 20; solved++; onScore(score); feedback = "Yes, $word."; delay(900); next()
            } else { feedback = "Not quite. Try another order."; delay(700); picked.clear() }
        }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        T("Word ${index + 1} of ${words.size}", Tuulit.type.caption, Tuulit.colors.inkMuted, Modifier.align(Alignment.Start))
        T(hint, Tuulit.type.title, modifier = Modifier.padding(top = 24.dp), align = TextAlign.Center)
        Row(Modifier.padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            word.indices.forEach { i ->
                val ch = guess.getOrNull(i)?.toString() ?: if (hintUsed && i == 0) word.first().toString() else ""
                Box(Modifier.size(38.dp, 48.dp).clip(RoundedCornerShape(10.dp)).background(Tuulit.colors.oat).border(1.5.dp, Tuulit.colors.inkFaint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) { T(ch, Tuulit.type.title, if (guess.getOrNull(i) == null) Tuulit.colors.inkFaint else Tuulit.colors.ink) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            letters.forEachIndexed { i, ch ->
                val used = i in picked
                Box(Modifier.size(44.dp).clip(CircleShape).background(if (used) Tuulit.colors.cream else Tuulit.colors.sand)
                    .clickable(enabled = !used, role = Role.Button) { picked += i }.semantics { contentDescription = "Letter $ch" },
                    contentAlignment = Alignment.Center) { if (!used) T(ch.toString(), Tuulit.type.label) }
            }
        }
        T(feedback, Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 16.dp).semantics { liveRegion = LiveRegionMode.Polite })
        Box(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TButton("Undo", { if (picked.isNotEmpty()) picked.removeAt(picked.lastIndex) }, Modifier.weight(1f), kind = ButtonKind.QUIET)
            TButton("Hint", { hintUsed = true }, Modifier.weight(1f), kind = ButtonKind.QUIET, enabled = !hintUsed)
            TButton("Skip", { next() }, Modifier.weight(1f), kind = ButtonKind.QUIET)
        }
    }
}

// ---------------------------------------------------------------- Calm Tap

@Composable
private fun CalmTap(onScore: (Int) -> Unit, onEnd: (Int, String) -> Unit) {
    val col = Tuulit.colors
    val grow = remember { Animatable(0.3f) }
    var score by remember { mutableIntStateOf(0) }
    var taps by remember { mutableIntStateOf(0) }
    var smooth by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("Tap as the circle reaches the ring. There's no rush.") }
    var tappedThisBreath by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) {
            tappedThisBreath = false
            grow.snapTo(0.3f)
            grow.animateTo(1.05f, tween(4000, easing = LinearEasing))
            delay(400)
        }
    }
    Column(Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures {
            if (tappedThisBreath) return@detectTapGestures
            tappedThisBreath = true; taps++
            val v = grow.value
            if (v in 0.88f..1.05f) { score += 15; smooth++; onScore(score); feedback = "Smooth rhythm." }
            else if (v < 0.88f) feedback = "A little early. Wait for the ring."
            if (taps >= 10) onEnd(score, "$smooth of 10 taps in rhythm. Notice your breathing now.")
        }
    }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(260.dp).border(2.dp, col.ink, CircleShape))
            Box(Modifier.size(232.dp).border(1.5.dp, col.inkFaint, CircleShape))
            Box(Modifier.size(240.dp).scale(grow.value).clip(CircleShape).background(col.banana), contentAlignment = Alignment.Center) {
                T("Tap", Tuulit.type.label, col.onBanana)
            }
        }
        T(feedback, Tuulit.type.title, modifier = Modifier.padding(top = 28.dp).semantics { liveRegion = LiveRegionMode.Polite }, align = TextAlign.Center)
        T("${10 - taps} taps left · tap anywhere", Tuulit.type.caption, col.inkFaint, Modifier.padding(top = 8.dp))
    }
}

// ---------------------------------------------------------------- Focus Game

private val distractors = listOf(TuulitIcons.Moon, TuulitIcons.Care, TuulitIcons.Spark, TuulitIcons.Drop, TuulitIcons.Music, TuulitIcons.Book)

@Composable
private fun FocusGame(onScore: (Int) -> Unit, onEnd: (Int, String) -> Unit) {
    val col = Tuulit.colors
    var grid by remember { mutableStateOf(List(12) { -1 }) }
    var bananaAt by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(45) }
    var feedback by remember { mutableStateOf("Tap only the banana.") }
    LaunchedEffect(Unit) {
        var elapsedMs = 0L
        while (isActive && seconds > 0) {
            val showBanana = Random.nextFloat() < 0.7f
            bananaAt = if (showBanana) Random.nextInt(12) else -1
            grid = List(12) { i -> if (i == bananaAt) 99 else if (Random.nextFloat() < 0.6f) Random.nextInt(distractors.size) else -1 }
            val window = (1300 - hits * 25).coerceAtLeast(650).toLong()
            delay(window); elapsedMs += window
            seconds = 45 - (elapsedMs / 1000).toInt()
        }
        onEnd(score, "$hits bananas spotted, $misses mis-taps. Nicely focused.")
    }
    Column(Modifier.fillMaxSize()) {
        T("${seconds.coerceAtLeast(0)} s", Tuulit.type.caption, col.inkMuted, Modifier.padding(bottom = 14.dp))
        grid.indices.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { i ->
                    val v = grid[i]
                    Box(Modifier.weight(1f).aspectRatio(1.2f).clip(RoundedCornerShape(16.dp)).background(col.sand)
                        .clickable(role = Role.Button) {
                            if (v == 99) { score += 10; hits++; onScore(score); feedback = "Got it."; grid = grid.toMutableList().also { it[i] = -1 }; bananaAt = -1 }
                            else if (v >= 0) { misses++; feedback = "That one's not a banana." }
                        }.semantics { contentDescription = if (v == 99) "Banana" else if (v >= 0) "Shape" else "Empty" },
                        contentAlignment = Alignment.Center) {
                        when {
                            v == 99 -> Mascot(MascotMood.WARM, 52.dp)
                            v >= 0 -> Icon(distractors[v], null, Modifier.size(30.dp), tint = col.inkMuted)
                        }
                    }
                }
            }
        }
        Box(Modifier.weight(1f))
        T(feedback, Tuulit.type.bodySm, col.inkMuted, Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, align = TextAlign.Center)
    }
}

// ---------------------------------------------------------------- Stress Smash

@Composable
private fun StressSmash(onScore: (Int) -> Unit, onEnd: (Int, String) -> Unit) {
    val col = Tuulit.colors
    val view = LocalView.current
    var crushed by remember { mutableStateOf(setOf<Int>()) }
    var total by remember { mutableIntStateOf(0) }
    var hit by remember { mutableIntStateOf(-1) }
    LaunchedEffect(hit) { if (hit >= 0) { delay(160); hit = -1 } }
    Column(Modifier.fillMaxSize()) {
        T("Tap to crush. Keep going as long as it helps.", Tuulit.type.bodySm, col.inkMuted, Modifier.padding(bottom = 14.dp))
        (0 until 30).chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { i ->
                    val isCrushed = i in crushed
                    Box(Modifier.weight(1f).aspectRatio(1f).scale(if (hit == i) 0.9f else 1f).clip(RoundedCornerShape(12.dp))
                        .background(if (hit == i) col.banana else if (isCrushed) col.cream else col.tan)
                        .then(if (isCrushed) Modifier.border(1.5.dp, col.hairline, RoundedCornerShape(12.dp)) else Modifier)
                        .clickable(enabled = !isCrushed, role = Role.Button) {
                            crushed = crushed + i; total++; hit = i; onScore(total)
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }.semantics { contentDescription = if (isCrushed) "Crushed" else "Tile" },
                        contentAlignment = Alignment.Center) {
                        if (isCrushed) Icon(TuulitIcons.Check, null, Modifier.size(16.dp), tint = col.inkFaint)
                    }
                }
            }
        }
        Box(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TButton("Fresh board", { crushed = emptySet() }, Modifier.weight(1f), kind = ButtonKind.QUIET, icon = TuulitIcons.Refresh)
            TButton("I feel better", { onEnd(total, "$total tiles crushed. Glad that helped.") }, Modifier.weight(1f))
        }
    }
}

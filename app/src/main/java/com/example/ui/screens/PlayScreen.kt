package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.BananaModePersona
import com.example.ui.TuulitUiState
import com.example.ui.components.BananaMascot
import com.example.ui.components.TuulitBadge
import com.example.ui.components.TuulitCard
import com.example.ui.components.TuulitPrimaryButton
import com.example.ui.components.TuulitSecondaryButton
import com.example.ui.theme.TuulitCharcoal
import com.example.ui.theme.TuulitThemeTokens
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private enum class PlayHubTab(val label: String) {
    GAMES("Games"),
    QUIZZES("Quizzes"),
    REWARDS("Milestones")
}

private enum class ActiveMiniGame(val id: String, val title: String) {
    BANANA_CATCH("banana_catch", "Banana Catch"),
    MEMORY_FLIP("memory_flip", "Mood Match"),
    CALM_TAP("calm_tap", "Calm Tap"),
    STRESS_SMASH("stress_smash", "Stress Smash")
}

private data class QuizQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int, // -1 for personality/reflective quizzes
    val explanation: String
)

private data class TuulitQuiz(
    val id: String,
    val title: String,
    val subtitle: String,
    val questions: List<QuizQuestion>
)

private val tuulitQuizzes = listOf(
    TuulitQuiz(
        id = "myths_facts",
        title = "Period Myths vs. Facts",
        subtitle = "Test common misconceptions with zero judgment.",
        questions = listOf(
            QuizQuestion(
                "Everyone with a healthy menstrual cycle has a 28-day cycle.",
                listOf("Fact", "Myth"),
                1,
                "Myth. Healthy adult cycles commonly range between 21 and 35 days and naturally shift by a few days."
            ),
            QuizQuestion(
                "Severe period pain that stops you from working or sleeping should always be accepted as normal.",
                listOf("Fact", "Myth"),
                1,
                "Myth. Disruptive pain deserves evaluation by a healthcare clinician—never let severe pain be dismissed."
            ),
            QuizQuestion(
                "Gentle movement or stretching is unsafe during your period.",
                listOf("Fact", "Myth"),
                1,
                "Myth. Gentle movement is safe if it feels comfortable, and rest is equally valid whenever you need it."
            ),
            QuizQuestion(
                "Calendar fertile-window estimates are reliable contraception.",
                listOf("Fact", "Myth"),
                1,
                "Myth. Ovulation timing shifts easily, so calendar estimates should never be used as birth control."
            )
        )
    ),
    TuulitQuiz(
        id = "cycle_knowledge",
        title = "How Well Do You Know Your Cycle?",
        subtitle = "A quick 3-question body literacy check.",
        questions = listOf(
            QuizQuestion(
                "Which natural chemical messengers trigger uterine contractions (cramps) during menstruation?",
                listOf("Prostaglandins", "Melatonin", "Adrenaline"),
                0,
                "Prostaglandins signal the uterus to contract to shed its lining; higher levels often mean stronger cramps."
            ),
            QuizQuestion(
                "Which phase typically occurs between ovulation and the start of the next period?",
                listOf("Follicular phase", "Luteal phase", "Menstrual phase"),
                1,
                "The luteal phase follows ovulation and lasts roughly 11–16 days for most people."
            ),
            QuizQuestion(
                "Can stress, travel, or illness temporarily shift when your period arrives?",
                listOf("Yes, they can", "No, cycles never respond to life events"),
                0,
                "Yes—the brain-ovary hormone loop responds sensitively to stress, illness, and sleep changes."
            )
        )
    ),
    TuulitQuiz(
        id = "self_care_style",
        title = "What Self-Care Do You Need Today?",
        subtitle = "A playful check-in to match your energy.",
        questions = listOf(
            QuizQuestion(
                "How does your social battery feel right now?",
                listOf("Quiet blanket mode", "Cozy 1-on-1 chat", "Up for a distraction"),
                -1,
                "Honor your real capacity today—there is no right answer."
            ),
            QuizQuestion(
                "Which comfort sounds best in the next 10 minutes?",
                listOf("Warm drink & breathing", "A quick mini-game", "Putting my phone down to rest"),
                -1,
                "Try adding your favorite pick to your Period Survival Kit in Tuulit Care."
            )
        )
    ),
    TuulitQuiz(
        id = "couple_support",
        title = "How Good Are You at Period Support?",
        subtitle = "Couple Mode quiz for partners and trusted friends.",
        questions = listOf(
            QuizQuestion(
                "Your person says their cramps are rough today. What is the best first move?",
                listOf("Ask what would feel helpful right now", "Give unsolicited medical advice", "Tell them it can't be that bad"),
                0,
                "Asking rather than assuming respects their comfort and builds real trust."
            ),
            QuizQuestion(
                "Are two people's menstrual cycles and symptoms basically identical?",
                listOf("Yes, mostly the same", "No—every person and cycle varies widely"),
                1,
                "Symptoms, flow, energy, and cycle lengths differ enormously from person to person."
            ),
            QuizQuestion(
                "If your partner feels frustrated during their luteal phase, should you immediately blame hormones?",
                listOf("No—listen to the actual concern first", "Yes, always bring up their cycle"),
                0,
                "Dismissing real emotions as 'just hormonal' feels invalidating. Listen with empathy first."
            )
        )
    )
)

@Composable
fun PlayScreen(
    uiState: TuulitUiState,
    onRecordGameScore: (String, Int) -> Unit,
    onQuizCompleted: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(PlayHubTab.GAMES) }
    var activeGame by remember { mutableStateOf<ActiveMiniGame?>(null) }
    var activeQuiz by remember { mutableStateOf<TuulitQuiz?>(null) }

    if (activeGame != null) {
        BackHandler { activeGame = null }
        when (activeGame!!) {
            ActiveMiniGame.BANANA_CATCH -> BananaCatchGame(
                highScore = uiState.gameScores["banana_catch"]?.highScore ?: 0,
                onFinishScore = { onRecordGameScore("banana_catch", it) },
                onBack = { activeGame = null }
            )
            ActiveMiniGame.MEMORY_FLIP -> MemoryFlipGame(
                bestMoves = uiState.gameScores["memory_flip"]?.highScore ?: 0,
                onFinishScore = { score -> onRecordGameScore("memory_flip", score) },
                onBack = { activeGame = null }
            )
            ActiveMiniGame.CALM_TAP -> CalmTapGame(
                highScore = uiState.gameScores["calm_tap"]?.highScore ?: 0,
                onFinishScore = { onRecordGameScore("calm_tap", it) },
                onBack = { activeGame = null }
            )
            ActiveMiniGame.STRESS_SMASH -> StressSmashGame(
                highScore = uiState.gameScores["stress_smash"]?.highScore ?: 0,
                onFinishScore = { onRecordGameScore("stress_smash", it) },
                onBack = { activeGame = null }
            )
        }
        return
    }

    if (activeQuiz != null) {
        BackHandler { activeQuiz = null }
        QuizRunnerView(
            quiz = activeQuiz!!,
            onCompleted = { onQuizCompleted() },
            onBack = { activeQuiz = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tuulit Play", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Lighthearted distractions, quizzes, and gentle milestones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BananaMascot(persona = BananaModePersona.DRAMA, size = 54.dp)
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            edgePadding = 16.dp,
            divider = {}
        ) {
            PlayHubTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("play_tab_${tab.name.lowercase()}")
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    PlayHubTab.GAMES -> {
                        val gameCards = listOf(
                            Triple(ActiveMiniGame.BANANA_CATCH, "Catch falling bananas while dodging alarm blocks.", Icons.Outlined.Games),
                            Triple(ActiveMiniGame.MEMORY_FLIP, "Match cozy wellness vector symbols in pairs.", Icons.Outlined.FavoriteBorder),
                            Triple(ActiveMiniGame.CALM_TAP, "Tap softly when the expanding yellow ring meets the rim.", Icons.Outlined.TouchApp),
                            Triple(ActiveMiniGame.STRESS_SMASH, "Pop and crush abstract clay tiles to unwind.", Icons.Outlined.Bolt)
                        )
                        items(gameCards.size) { i ->
                            val (game, desc, icon) = gameCards[i]
                            val best = uiState.gameScores[game.id]?.highScore ?: 0
                            TuulitCard(
                                onClick = { activeGame = game },
                                modifier = Modifier.testTag("play_game_${game.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(TuulitThemeTokens.extended.softYellow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, contentDescription = null, tint = TuulitCharcoal)
                                        }
                                        Column {
                                            Text(game.title, style = MaterialTheme.typography.titleMedium)
                                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (best > 0) {
                                        TuulitBadge(text = "Best $best")
                                    }
                                }
                            }
                        }
                    }

                    PlayHubTab.QUIZZES -> {
                        items(tuulitQuizzes.size) { i ->
                            val quiz = tuulitQuizzes[i]
                            TuulitCard(
                                onClick = { activeQuiz = quiz },
                                modifier = Modifier.testTag("play_quiz_${quiz.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(TuulitThemeTokens.extended.warmTan),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Quiz, contentDescription = null)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(quiz.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${quiz.questions.size} questions • ${quiz.subtitle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PlayHubTab.REWARDS -> {
                        item {
                            TuulitCard(containerColor = TuulitThemeTokens.extended.warmTan) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    BananaMascot(persona = BananaModePersona.HAPPY, size = 56.dp, showContainer = false)
                                    Column {
                                        Text("Streaks Without Pressure", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Earn milestones at your own pace. Tuulit never penalizes missed days or sends streak-loss guilt.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (uiState.achievements.isEmpty()) {
                            item {
                                TuulitCard {
                                    Text("Your milestones will appear here", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Log a cycle, check in with your mood, finish a breathing timer, or play a game to unlock your first badge.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(uiState.achievements.size) { i ->
                                val badge = uiState.achievements[i]
                                TuulitCard {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(TuulitThemeTokens.extended.softYellow),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = TuulitCharcoal)
                                        }
                                        Column {
                                            Text(badge.title, style = MaterialTheme.typography.titleSmall)
                                            Text(badge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- GAME 1: BANANA CATCH (Interactive Canvas) ---
private data class FallingItem(val x: Float, val y: Float, val speed: Float, val isBanana: Boolean)

@Composable
private fun BananaCatchGame(
    highScore: Int,
    onFinishScore: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val extended = TuulitThemeTokens.extended
    var basketX by remember { mutableFloatStateOf(0.5f) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var items by remember { mutableStateOf(listOf<FallingItem>()) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning) {
        var tick = 0
        while (isRunning) {
            delay(32L)
            tick++
            val updated = items.map { it.copy(y = it.y + it.speed) }.toMutableList()
            if (tick % 20 == 0) {
                updated.add(
                    FallingItem(
                        x = Random.nextFloat().coerceIn(0.1f, 0.9f),
                        y = 0f,
                        speed = 0.015f + Random.nextFloat() * 0.012f,
                        isBanana = Random.nextFloat() < 0.72f
                    )
                )
            }
            val surviving = mutableListOf<FallingItem>()
            updated.forEach { item ->
                val caught = item.y in 0.80f..0.92f && abs(item.x - basketX) < 0.14f
                if (caught) {
                    if (item.isBanana) {
                        score += 10
                    } else {
                        lives--
                    }
                } else if (item.y < 1.02f) {
                    surviving.add(item)
                }
            }
            items = surviving
            if (lives <= 0) {
                isRunning = false
                onFinishScore(score)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (score > 0) onFinishScore(score)
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Play")
            }
            Text("Banana Catch", style = MaterialTheme.typography.titleLarge)
            TuulitBadge(text = "Score $score • Best ${maxOf(score, highScore)}")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Drag below to catch bananas, dodge clay rocks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Lives: $lives", style = MaterialTheme.typography.labelLarge, color = extended.periodClay)
        }

        val bg = MaterialTheme.colorScheme.surface
        val yellow = extended.bananaYellow
        val clay = extended.periodClay
        val tan = extended.warmTan
        val ink = TuulitCharcoal

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(bg)
                .border(1.dp, extended.hairlineBorder, RoundedCornerShape(22.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        basketX = (change.position.x / size.width).coerceIn(0.12f, 0.88f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Falling objects
                items.forEach { item ->
                    val cx = item.x * w
                    val cy = item.y * h
                    if (item.isBanana) {
                        val arc = Path().apply {
                            moveTo(cx - 18f, cy + 8f)
                            quadraticTo(cx, cy - 22f, cx + 22f, cy - 4f)
                            quadraticTo(cx + 4f, cy + 16f, cx - 18f, cy + 8f)
                            close()
                        }
                        drawPath(arc, color = yellow)
                        drawPath(arc, color = ink, style = Stroke(width = 3f, cap = StrokeCap.Round))
                    } else {
                        drawRoundRect(
                            color = clay,
                            topLeft = Offset(cx - 16f, cy - 16f),
                            size = Size(32f, 32f),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            color = ink,
                            topLeft = Offset(cx - 16f, cy - 16f),
                            size = Size(32f, 32f),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(width = 2.5f)
                        )
                    }
                }

                // Basket
                val bw = w * 0.24f
                val bh = 28f
                val bx = (basketX * w) - bw / 2f
                val by = h * 0.86f
                drawRoundRect(
                    color = tan,
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(14f, 14f),
                    style = Stroke(width = 3.5f)
                )
            }

            if (!isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BananaMascot(persona = BananaModePersona.HAPPY, size = 72.dp)
                        Text("Round Complete", style = MaterialTheme.typography.headlineMedium)
                        Text("You caught ${score / 10} bananas ($score pts)", style = MaterialTheme.typography.bodyMedium)
                        TuulitPrimaryButton(
                            text = "Play Again",
                            onClick = {
                                score = 0
                                lives = 3
                                items = emptyList()
                                isRunning = true
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- GAME 2: MEMORY FLIP / MOOD MATCH (12 Vector Cards) ---
private data class MemoryCard(val id: Int, val symbolId: Int, val icon: ImageVector, val matched: Boolean = false)

@Composable
private fun MemoryFlipGame(
    bestMoves: Int,
    onFinishScore: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val extended = TuulitThemeTokens.extended
    val icons = listOf(
        Icons.Outlined.Spa,
        Icons.Outlined.LocalCafe,
        Icons.Outlined.Bedtime,
        Icons.Outlined.WbSunny,
        Icons.Outlined.MusicNote,
        Icons.Outlined.LocalFlorist
    )

    fun createDeck(): List<MemoryCard> {
        val pairs = icons.flatMapIndexed { idx, ic ->
            listOf(MemoryCard(idx * 2, idx, ic), MemoryCard(idx * 2 + 1, idx, ic))
        }
        return pairs.shuffled()
    }

    var cards by remember { mutableStateOf(createDeck()) }
    var flippedIndices by remember { mutableStateOf(listOf<Int>()) }
    var moves by remember { mutableIntStateOf(0) }
    var won by remember { mutableStateOf(false) }

    LaunchedEffect(flippedIndices) {
        if (flippedIndices.size == 2) {
            moves++
            val (a, b) = flippedIndices
            if (cards[a].symbolId == cards[b].symbolId) {
                cards = cards.mapIndexed { idx, c ->
                    if (idx == a || idx == b) c.copy(matched = true) else c
                }
                flippedIndices = emptyList()
                if (cards.all { it.matched }) {
                    won = true
                    val score = (120 - moves * 4).coerceAtLeast(20)
                    onFinishScore(score)
                }
            } else {
                delay(650L)
                flippedIndices = emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Play")
            }
            Text("Mood Match", style = MaterialTheme.typography.titleLarge)
            TuulitBadge(text = "Moves: $moves")
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (r in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (c in 0 until 3) {
                        val idx = r * 3 + c
                        val card = cards[idx]
                        val revealed = card.matched || flippedIndices.contains(idx)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !revealed && flippedIndices.size < 2) {
                                    flippedIndices = flippedIndices + idx
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                card.matched -> extended.softYellow
                                revealed -> MaterialTheme.colorScheme.surface
                                else -> extended.warmTan
                            },
                            border = BorderStroke(1.dp, if (card.matched) extended.bananaYellow else extended.hairlineBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (revealed) {
                                    Icon(card.icon, contentDescription = "Matched symbol", modifier = Modifier.size(30.dp), tint = TuulitCharcoal)
                                } else {
                                    Icon(Icons.Outlined.StarOutline, contentDescription = "Hidden card", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (won) {
            TuulitCard(containerColor = extended.softYellow) {
                Text("All pairs matched in $moves moves!", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                TuulitPrimaryButton(
                    text = "Play Again",
                    onClick = {
                        cards = createDeck()
                        flippedIndices = emptyList()
                        moves = 0
                        won = false
                    }
                )
            }
        }
    }
}

// --- GAME 3: CALM TAP (Rhythmic Ring Focus) ---
@Composable
private fun CalmTapGame(
    highScore: Int,
    onFinishScore: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val extended = TuulitThemeTokens.extended
    var radiusFraction by remember { mutableFloatStateOf(0.25f) }
    var growing by remember { mutableStateOf(true) }
    var score by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("Tap when the yellow circle nears the outer ring") }

    LaunchedEffect(Unit) {
        while (true) {
            delay(28L)
            if (growing) {
                radiusFraction += 0.014f
                if (radiusFraction >= 0.96f) growing = false
            } else {
                radiusFraction -= 0.014f
                if (radiusFraction <= 0.22f) growing = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (score > 0) onFinishScore(score)
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Play")
            }
            Text("Calm Tap", style = MaterialTheme.typography.titleLarge)
            TuulitBadge(text = "Score $score • Best ${maxOf(score, highScore)}")
        }

        Text(feedback, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val outerColor = MaterialTheme.colorScheme.primary
        val innerFill = extended.bananaYellow
        val softTan = extended.warmTan

        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .clickable {
                    val distance = abs(radiusFraction - 0.82f)
                    if (distance < 0.14f) {
                        score += 15
                        feedback = "Smooth cadence (+15)"
                        onFinishScore(score)
                    } else {
                        feedback = "Gentle rhythm—aim near the dark ring"
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = size.minDimension / 2f
                drawCircle(color = softTan, radius = maxR * 0.94f)
                drawCircle(color = innerFill, radius = maxR * radiusFraction)
                drawCircle(color = outerColor, radius = maxR * 0.82f, style = Stroke(width = 4f))
            }
            Text("TAP", style = MaterialTheme.typography.titleMedium, color = TuulitCharcoal)
        }
    }
}

// --- GAME 4: STRESS SMASH (Popping Geometric Clay Tiles) ---
@Composable
private fun StressSmashGame(
    highScore: Int,
    onFinishScore: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val extended = TuulitThemeTokens.extended
    var popped by remember { mutableStateOf(List(16) { false }) }
    var totalPopped by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (totalPopped > 0) onFinishScore(totalPopped)
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Play")
            }
            Text("Stress Smash", style = MaterialTheme.typography.titleLarge)
            TuulitBadge(text = "Popped $totalPopped • Best ${maxOf(totalPopped, highScore)}")
        }

        Text(
            "Tap any tile to crush it. Reset the board as many times as you like.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (r in 0 until 4) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in 0 until 4) {
                        val idx = r * 4 + c
                        val isPopped = popped[idx]
                        val tileBg = when {
                            isPopped -> extended.warmTan.copy(alpha = 0.4f)
                            (r + c) % 3 == 0 -> extended.bananaYellow
                            (r + c) % 2 == 0 -> extended.periodClay
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(if (isPopped) 24.dp else 14.dp))
                                .background(tileBg)
                                .border(1.dp, extended.hairlineBorder, RoundedCornerShape(if (isPopped) 24.dp else 14.dp))
                                .clickable {
                                    if (!isPopped) {
                                        popped = popped.toMutableList().also { it[idx] = true }
                                        totalPopped++
                                        if (popped.all { it }) {
                                            onFinishScore(totalPopped)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPopped) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = "Crushed tile", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        TuulitPrimaryButton(
            text = "Fresh Board",
            onClick = { popped = List(16) { false } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- INTERACTIVE QUIZ RUNNER ---
@Composable
private fun QuizRunnerView(
    quiz: TuulitQuiz,
    onCompleted: () -> Unit,
    onBack: () -> Unit,
) {
    val extended = TuulitThemeTokens.extended
    var qIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to quizzes")
            }
            Text(quiz.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }

        if (finished) {
            TuulitCard(containerColor = extended.softYellow) {
                BananaMascot(persona = BananaModePersona.HAPPY, size = 68.dp)
                Spacer(Modifier.height(12.dp))
                Text("Quiz Complete", style = MaterialTheme.typography.headlineMedium)
                val gradedCount = quiz.questions.count { it.correctIndex >= 0 }
                if (gradedCount > 0) {
                    Text("You got $correctCount of $gradedCount right.", style = MaterialTheme.typography.titleSmall)
                } else {
                    Text("Thanks for checking in with yourself.", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(14.dp))
                TuulitPrimaryButton(text = "Back to Tuulit Play", onClick = onBack)
            }
        } else {
            val q = quiz.questions[qIndex]
            TuulitBadge(text = "Question ${qIndex + 1} of ${quiz.questions.size}")
            TuulitCard {
                Text(q.prompt, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(14.dp))
                q.options.forEachIndexed { idx, opt ->
                    val chosen = selectedAnswer == idx
                    val isRight = q.correctIndex == idx
                    val bg = when {
                        selectedAnswer == null -> extended.warmTan
                        chosen && (q.correctIndex < 0 || isRight) -> extended.softYellow
                        chosen && !isRight -> extended.periodClaySoft
                        isRight -> extended.softYellow
                        else -> MaterialTheme.colorScheme.surface
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = selectedAnswer == null) {
                                selectedAnswer = idx
                                if (q.correctIndex == idx) correctCount++
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = bg,
                        border = BorderStroke(1.dp, extended.hairlineBorder)
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (selectedAnswer != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(q.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    TuulitPrimaryButton(
                        text = if (qIndex == quiz.questions.lastIndex) "Finish Quiz" else "Next Question",
                        onClick = {
                            if (qIndex == quiz.questions.lastIndex) {
                                finished = true
                                onCompleted()
                            } else {
                                qIndex++
                                selectedAnswer = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

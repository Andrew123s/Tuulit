package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.EmergencyChecklistItemEntity
import com.example.domain.BananaModePersona
import com.example.ui.TuulitUiState
import com.example.ui.components.BananaMascot
import com.example.ui.components.TuulitBadge
import com.example.ui.components.TuulitCard
import com.example.ui.components.TuulitNoticeBanner
import com.example.ui.components.TuulitPrimaryButton
import com.example.ui.components.TuulitSecondaryButton
import com.example.ui.theme.TuulitThemeTokens
import kotlinx.coroutines.delay

private enum class CareTab(val label: String) {
    CRAMPS("Cramp Relief"),
    RELAX("Relaxation"),
    MOOD("Mood Reset"),
    SLEEP("Sleep"),
    KIT("Survival Kit"),
    FOOD("Food & Checklist"),
}

private enum class DiscomfortLevel(val label: String) {
    MILD("Mild"),
    MODERATE("Moderate"),
    SEVERE("Severe")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareScreen(
    uiState: TuulitUiState,
    onRelaxationCompleted: () -> Unit,
    onSaveSleepNote: (String) -> Unit,
    onAddSurvivalItem: (String, String, String) -> Unit,
    onDeleteSurvivalItem: (Long) -> Unit,
    onToggleChecklistItem: (EmergencyChecklistItemEntity) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onDeleteChecklistItem: (Long) -> Unit,
    onOpenPlay: () -> Unit,
    onOpenPainJournal: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(CareTab.CRAMPS) }
    val extended = TuulitThemeTokens.extended

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
            Text("Tuulit Care", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Practical comfort, breathing, rest, and personalized survival routines.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            edgePadding = 16.dp,
            divider = {}
        ) {
            CareTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("care_tab_${tab.name.lowercase()}")
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    CareTab.CRAMPS -> item {
                        CrampReliefSection(
                            onStartBreathing = { selectedTab = CareTab.RELAX },
                            onOpenPainJournal = onOpenPainJournal
                        )
                    }
                    CareTab.RELAX -> item {
                        RelaxationStudioSection(
                            reducedMotion = uiState.prefs.reducedMotionEnabled,
                            onCompleted = onRelaxationCompleted
                        )
                    }
                    CareTab.MOOD -> item {
                        MoodResetSection(
                            onGoBreathing = { selectedTab = CareTab.RELAX },
                            onGoPlay = onOpenPlay
                        )
                    }
                    CareTab.SLEEP -> item {
                        SleepSupportSection(
                            savedNote = uiState.prefs.sleepRoutineNote,
                            onSaveNote = onSaveSleepNote,
                            onStartBreathing = { selectedTab = CareTab.RELAX }
                        )
                    }
                    CareTab.KIT -> item {
                        SurvivalKitSection(
                            uiState = uiState,
                            onAddItem = onAddSurvivalItem,
                            onDeleteItem = onDeleteSurvivalItem
                        )
                    }
                    CareTab.FOOD -> item {
                        FoodAndChecklistSection(
                            uiState = uiState,
                            onToggleChecklist = onToggleChecklistItem,
                            onAddChecklist = onAddChecklistItem,
                            onDeleteChecklist = onDeleteChecklistItem
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrampReliefSection(
    onStartBreathing: () -> Unit,
    onOpenPainJournal: () -> Unit,
) {
    val extended = TuulitThemeTokens.extended
    var level by remember { mutableStateOf(DiscomfortLevel.MILD) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TuulitCard(containerColor = extended.warmTan) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BananaMascot(persona = BananaModePersona.CRAMPS, size = 60.dp, showContainer = false)
                Column {
                    Text("How does your discomfort feel?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Wellness options are comfort support—not guaranteed medical treatments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiscomfortLevel.entries.forEach { item ->
                    FilterChip(
                        selected = level == item,
                        onClick = { level = item },
                        label = { Text(item.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (item == DiscomfortLevel.SEVERE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("cramp_level_${item.name.lowercase()}")
                    )
                }
            }
        }

        when (level) {
            DiscomfortLevel.MILD -> {
                TuulitCard {
                    Text("Mild Discomfort Support", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    BulletLine("Gentle warmth: Place a warm compress or hot water bottle (wrapped in a cloth) over your lower abdomen for 15–20 minutes.")
                    BulletLine("Paced breathing: Try 1–3 minutes of slow exhales to soften abdominal muscle tension.")
                    BulletLine("Light movement: A slow walk, gentle hip circles, or reclining with knees elevated can feel soothing.")
                    BulletLine("Hydration: Sipping warm water or caffeine-free herbal tea may feel comforting.")
                    Spacer(Modifier.height(12.dp))
                    TuulitPrimaryButton(text = "Start 3-min Breathing", icon = Icons.Outlined.Air, onClick = onStartBreathing)
                }
            }
            DiscomfortLevel.MODERATE -> {
                TuulitCard {
                    Text("Moderate Discomfort Support", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    BulletLine("Supportive body positions: Try lying on your side with knees drawn toward your chest, or child's pose with a pillow under your torso.")
                    BulletLine("Heat therapy: Sustained low-level warmth helps relax uterine smooth muscle for many people. Avoid placing high heat directly on bare skin.")
                    BulletLine("Pain management info: Over-the-counter options (if safe for you) are something to verify with a pharmacist or clinician, especially if you take other medications.")
                    BulletLine("Track duration & impact: Recording pain in your Pain Journal helps you spot multi-cycle trends.")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TuulitPrimaryButton(text = "Guided Relaxation", onClick = onStartBreathing, modifier = Modifier.weight(1f))
                        TuulitSecondaryButton(text = "Log in Pain Journal", onClick = onOpenPainJournal, modifier = Modifier.weight(1f))
                    }
                }
            }
            DiscomfortLevel.SEVERE -> {
                TuulitNoticeBanner(
                    title = "Severe pain deserves real medical care",
                    message = "Pain that stops you from working, studying, walking, or sleeping is not something you should simply endure. Wellness exercises are not a substitute for clinical evaluation.",
                    icon = Icons.Outlined.WarningAmber,
                    containerColor = extended.alertSoft,
                    accentColor = MaterialTheme.colorScheme.error
                )
                TuulitCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Warning signs needing urgent care", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    BulletLine("Sudden, sharp, or one-sided pelvic pain that comes on rapidly.")
                    BulletLine("Soaking through one or more pads or tampons every hour for consecutive hours.")
                    BulletLine("Pain accompanied by fever, chills, dizziness, fainting, or persistent vomiting.")
                    BulletLine("Severe pain if there is any possibility of pregnancy.")
                    Spacer(Modifier.height(12.dp))
                    TuulitPrimaryButton(
                        text = "Record in Pain Journal for Doctor",
                        icon = Icons.Outlined.Healing,
                        onClick = onOpenPainJournal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun RelaxationStudioSection(
    reducedMotion: Boolean,
    onCompleted: () -> Unit,
) {
    val extended = TuulitThemeTokens.extended
    val durations = listOf(1, 3, 5, 10)
    var selectedMinutes by remember { mutableIntStateOf(3) }
    var isRunning by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(selectedMinutes * 60) }
    var cycleSecond by remember { mutableIntStateOf(0) } // 14-second cycle: 4 inhale, 4 hold, 6 exhale
    var completedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMinutes) {
        if (!isRunning) {
            secondsRemaining = selectedMinutes * 60
            cycleSecond = 0
        }
    }

    LaunchedEffect(isRunning) {
        while (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
            cycleSecond = (cycleSecond + 1) % 14
        }
        if (isRunning && secondsRemaining == 0) {
            isRunning = false
            completedOnce = true
            onCompleted()
        }
    }

    val phaseText = when {
        !isRunning -> "Ready when you are"
        cycleSecond < 4 -> "Inhale slowly (4s)"
        cycleSecond < 8 -> "Hold gently (4s)"
        else -> "Exhale long & soft (6s)"
    }

    val targetRadiusFraction = when {
        !isRunning -> 0.55f
        reducedMotion -> 0.65f
        cycleSecond < 4 -> 0.88f
        cycleSecond < 8 -> 0.88f
        else -> 0.45f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetRadiusFraction,
        animationSpec = tween(durationMillis = if (reducedMotion) 100 else 900),
        label = "breathing_circle"
    )

    val mins = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val timerStr = String.format("%02d:%02d", mins, secs)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TuulitCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Relaxation Studio", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "4–4–6 paced breathing & body relaxation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BananaMascot(persona = BananaModePersona.CHILL, size = 48.dp, showContainer = false)
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEach { m ->
                    FilterChip(
                        selected = selectedMinutes == m,
                        onClick = {
                            selectedMinutes = m
                            isRunning = false
                            secondsRemaining = m * 60
                            completedOnce = false
                        },
                        label = { Text("$m min") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            val circleFill = extended.warmTan
            val ringAccent = extended.bananaYellow
            val borderStroke = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(190.dp)) {
                    val maxR = size.minDimension / 2f
                    drawCircle(color = circleFill, radius = maxR * 0.94f)
                    drawCircle(
                        color = ringAccent,
                        radius = maxR * animatedFraction
                    )
                    drawCircle(
                        color = borderStroke,
                        radius = maxR * animatedFraction,
                        style = Stroke(width = 3f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(timerStr, style = MaterialTheme.typography.headlineLarge)
                    Text(phaseText, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TuulitPrimaryButton(
                    text = if (isRunning) "Pause" else "Start Session",
                    onClick = {
                        if (secondsRemaining == 0) secondsRemaining = selectedMinutes * 60
                        isRunning = !isRunning
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("breathing_toggle_button")
                )
                TuulitSecondaryButton(
                    text = "Reset",
                    onClick = {
                        isRunning = false
                        secondsRemaining = selectedMinutes * 60
                        cycleSecond = 0
                    }
                )
            }

            if (completedOnce) {
                Spacer(Modifier.height(10.dp))
                TuulitBadge(
                    text = "Session completed — Calm Moment unlocked",
                    icon = Icons.Outlined.Spa,
                    containerColor = extended.softYellow
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodResetSection(
    onGoBreathing: () -> Unit,
    onGoPlay: () -> Unit,
) {
    val states = listOf(
        Triple("Overwhelmed", "Take one thing off your plate right now. Start a 3-minute paced exhale to downshift your nervous system.", "Start breathing"),
        Triple("Irritated", "Irritation is a valid signal—not a flaw. Try stepping away from noisy screens or venting energy in Stress Smash.", "Open Play"),
        Triple("Sad", "You don't need to force cheerfulness. Wrap up comfortably, lower expectations for the hour, and jot down one honest sentence if you'd like.", "Start breathing"),
        Triple("Exhausted", "Skip productivity pressure. Hydrate, rest your eyes for 5 minutes, or switch on 'Today Is Not the Day' mode on Home.", "Start breathing"),
        Triple("Bored", "Give your brain a light spark with Banana Catch, Memory Flip, or a 2-minute period myths quiz.", "Open Play"),
        Triple("Anxious", "Try 5-4-3-2-1 grounding: notice 5 things you see, 4 you can touch, 3 you hear, 2 you smell, and 1 steady breath.", "Start breathing")
    )
    var chosen by remember { mutableStateOf(states.first()) }

    TuulitCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Mood, contentDescription = null)
            Text("Mood Reset", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            "Select how you feel—no toxic positivity and no automatic assumption that every mood is hormonal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            states.forEach { item ->
                FilterChip(
                    selected = chosen.first == item.first,
                    onClick = { chosen = item },
                    label = { Text(item.first) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        TuulitCard(containerColor = TuulitThemeTokens.extended.warmTan) {
            Text("Feeling ${chosen.first.lowercase()}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(chosen.second, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            TuulitPrimaryButton(
                text = chosen.third,
                onClick = { if (chosen.third == "Open Play") onGoPlay() else onGoBreathing() }
            )
        }
    }
}

@Composable
private fun SleepSupportSection(
    savedNote: String,
    onSaveNote: (String) -> Unit,
    onStartBreathing: () -> Unit,
) {
    var note by remember(savedNote) { mutableStateOf(savedNote) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TuulitCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BananaMascot(persona = BananaModePersona.SLEEPY, size = 56.dp, showContainer = false)
                Column {
                    Text("Sleep Support", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Gentle wind-down cues for restless nights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            BulletLine("Cool room + warm feet: A slightly cool bedroom with cozy socks or a warm compress can ease luteal-phase warmth.")
            BulletLine("Low-light 15 minutes: Dim screens and let your jaw and shoulders drop before lying down.")
            BulletLine("4–4–6 Wind-Down: Two minutes of longer exhales signals safety to your resting heart rate.")
            Spacer(Modifier.height(12.dp))
            TuulitSecondaryButton(text = "Open Breathing Timer", icon = Icons.Outlined.Bedtime, onClick = onStartBreathing)
        }

        TuulitCard {
            Text("Personal Sleep Notes & Routine", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(500) },
                placeholder = { Text("What helps you sleep best? (e.g., chamomile tea, side pillow, rain audio)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            TuulitPrimaryButton(text = "Save Sleep Note", onClick = { onSaveNote(note) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SurvivalKitSection(
    uiState: TuulitUiState,
    onAddItem: (String, String, String) -> Unit,
    onDeleteItem: (Long) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    val categories = listOf("Comfort", "Activity", "Food", "Game", "Routine", "Note")
    var category by remember { mutableStateOf("Comfort") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TuulitCard {
            Text("My Period Survival Kit", style = MaterialTheme.typography.titleLarge)
            Text(
                "Save your personal go-to comfort items, snacks, routines, and reminders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            uiState.survivalKit.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TuulitThemeTokens.extended.warmTan)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (item.detail.isNotBlank()) "${item.category} • ${item.detail}" else item.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteItem(item.id) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove survival kit item")
                    }
                }
            }
        }

        TuulitCard {
            Text("Add to Survival Kit", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Item or routine name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it.take(100) },
                label = { Text("Optional note (e.g., playlist or reminder)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            TuulitPrimaryButton(
                text = "Add Item",
                icon = Icons.Outlined.Add,
                onClick = {
                    if (title.isNotBlank()) {
                        onAddItem(title, category, detail)
                        title = ""
                        detail = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun FoodAndChecklistSection(
    uiState: TuulitUiState,
    onToggleChecklist: (EmergencyChecklistItemEntity) -> Unit,
    onAddChecklist: (String) -> Unit,
    onDeleteChecklist: (Long) -> Unit,
) {
    var newCheckText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TuulitCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Restaurant, contentDescription = null)
                Text("Cycle-Aware Comfort Food Ideas", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "Gentle nourishment ideas based on comfort—no claims that foods cure hormonal conditions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            BulletLine("During heavier flow: Iron-rich comfort meals like lentil soup, oatmeal with pumpkin seeds, or warm bean stew paired with citrus.")
            BulletLine("When bloated: Ginger or peppermint tea, bananas, rice bowls, and steady water intake.")
            BulletLine("When craving something sweet: Dark chocolate with roasted almonds or banana slices with peanut butter.")
        }

        TuulitCard {
            Text("Period Emergency Checklist", style = MaterialTheme.typography.titleLarge)
            Text(
                "Keep your bag stocked for unexpected starts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            uiState.checklist.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleChecklist(item) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.packed) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = if (item.packed) "Packed" else "Not packed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDeleteChecklist(item.id) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete checklist item", modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newCheckText,
                    onValueChange = { newCheckText = it.take(50) },
                    placeholder = { Text("Add emergency bag item") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newCheckText.isNotBlank()) {
                            onAddChecklist(newCheckText)
                            newCheckText = ""
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add checklist item")
                }
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(TuulitThemeTokens.extended.bananaYellow)
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

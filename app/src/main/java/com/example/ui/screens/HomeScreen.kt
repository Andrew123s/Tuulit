package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.BananaModePersona
import com.example.domain.CyclePhase
import com.example.domain.DailyLog
import com.example.domain.FlowIntensity
import com.example.domain.TrackingCatalog
import com.example.ui.TuulitUiState
import com.example.ui.components.BananaMascot
import com.example.ui.components.TuulitBadge
import com.example.ui.components.TuulitCard
import com.example.ui.components.TuulitPrimaryButton
import com.example.ui.components.TuulitSecondaryButton
import com.example.ui.components.TuulitSectionHeader
import com.example.ui.theme.TuulitThemeTokens

private data class NeedOption(
    val id: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val recommendation: String,
    val actionLabel: String,
)

private val needOptions = listOf(
    NeedOption(
        id = "pain",
        label = "I'm in pain",
        subtitle = "Cramps, aches, or heavy discomfort",
        icon = Icons.Outlined.Healing,
        recommendation = "Let's check your discomfort level with gentle warmth suggestions, resting positions, or medical warning guidance.",
        actionLabel = "Open Cramp Relief"
    ),
    NeedOption(
        id = "irritated",
        label = "I'm irritated",
        subtitle = "Overstimulated or on edge",
        icon = Icons.Outlined.SelfImprovement,
        recommendation = "No pressure to smile through it. Try a 3-minute paced exhale exercise or Smash some shapes to let off steam.",
        actionLabel = "Start Calming Exercise"
    ),
    NeedOption(
        id = "sad",
        label = "I'm sad",
        subtitle = "Heavy, tender, or emotional",
        icon = Icons.Outlined.EditNote,
        recommendation = "You don't need to fix how you feel right away. A quiet private note or a gentle chat can give your thoughts space.",
        actionLabel = "Open Private Journal"
    ),
    NeedOption(
        id = "exhausted",
        label = "I'm exhausted",
        subtitle = "Running on empty today",
        icon = Icons.Outlined.Bedtime,
        recommendation = "Switch into 'Today Is Not the Day' minimal mode so Tuulit stays calm, quiet, and simple.",
        actionLabel = "Switch to Minimal Mode"
    ),
    NeedOption(
        id = "bored",
        label = "I'm bored",
        subtitle = "Need a lighthearted distraction",
        icon = Icons.Outlined.Games,
        recommendation = "Jump into a quick round of Banana Catch, Memory Flip, or a Period Myths quiz.",
        actionLabel = "Go to Tuulit Play"
    ),
    NeedOption(
        id = "sleep",
        label = "I can't sleep",
        subtitle = "Restless mind or body at night",
        icon = Icons.Outlined.NightlightRound,
        recommendation = "Try a slow 4-4-6 breathing rhythm and your Sleep Support routine.",
        actionLabel = "Open Sleep Support"
    ),
    NeedOption(
        id = "someone",
        label = "I need someone",
        subtitle = "Talk to Tuulit AI or reach a trusted person",
        icon = Icons.Outlined.Group,
        recommendation = "Send a 1-tap Tuulit SOS message without typing, or start a calm conversation with Tuulit AI.",
        actionLabel = "Open Tuulit AI"
    ),
    NeedOption(
        id = "unsure",
        label = "I don't know",
        subtitle = "Just feeling off and unsure why",
        icon = Icons.Outlined.Mood,
        recommendation = "That is completely okay. Let's do a gentle 10-second mood and energy check-in to see what sounds easiest right now.",
        actionLabel = "Quick Check-In"
    )
)

private val sosPresets = listOf(
    "Today is a rough one.",
    "I need a hug.",
    "I need food or a warm drink.",
    "Check on me later.",
    "I need a little space right now.",
    "Could you help me with something?",
    "Let's watch something cozy.",
    "Call me when you can.",
    "I'm not feeling well today."
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: TuulitUiState,
    onLogPeriodStart: (Long) -> Unit,
    onLogPeriodEnd: (Long) -> Unit,
    onQuickSaveTodayLog: (DailyLog) -> Unit,
    onSetBananaMode: (BananaModePersona) -> Unit,
    onSetTodayNotTheDay: (Boolean) -> Unit,
    onOpenAi: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenCare: () -> Unit,
    onOpenPlay: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenPainJournal: () -> Unit,
    onOpenConnect: () -> Unit,
) {
    val context = LocalContext.current
    val extended = TuulitThemeTokens.extended

    var showNeedSomethingSheet by remember { mutableStateOf(false) }
    var selectedNeed by remember { mutableStateOf<NeedOption?>(null) }
    var showBananaModeSheet by remember { mutableStateOf(false) }
    var showSosSheet by remember { mutableStateOf(false) }

    // Minimal "Today Is Not the Day" Mode
    if (uiState.prefs.todayIsNotTheDayMode) {
        TodayIsNotTheDayView(
            persona = uiState.prefs.bananaMode,
            onExitMinimal = { onSetTodayNotTheDay(false) },
            onLogFeeling = onOpenCalendar,
            onDistract = onOpenPlay,
            onRelax = onOpenCare,
            onAskTuulit = onOpenAi,
            onTellMyPerson = { showSosSheet = true }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Greeting + Mascot + Ask Tuulit quick pill
                item {
                    val namePart = uiState.prefs.displayName.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hey$namePart, how are you feeling today?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${uiState.prefs.bananaMode.title}  •  ${uiState.prefs.bananaMode.focusHint}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showBananaModeSheet = true }
                                    .padding(vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        BananaMascot(
                            persona = uiState.prefs.bananaMode,
                            size = 64.dp,
                            modifier = Modifier
                                .clickable { showBananaModeSheet = true }
                                .testTag("home_banana_mascot")
                        )
                    }
                }

                // Hero Cycle Overview Card (flat cream/tan layering, zero gradients)
                item {
                    val pred = uiState.prediction
                    val badgeColor = when {
                        pred.isCurrentlyMenstruating -> extended.periodClaySoft
                        pred.currentPhase == CyclePhase.ESTIMATED_OVULATION -> extended.estimateOchreSoft
                        else -> extended.warmTan
                    }
                    val badgeInk = when {
                        pred.isCurrentlyMenstruating -> extended.periodClay
                        pred.currentPhase == CyclePhase.ESTIMATED_OVULATION -> extended.estimateOchre
                        else -> MaterialTheme.colorScheme.onBackground
                    }

                    TuulitCard(
                        containerColor = MaterialTheme.colorScheme.surface,
                        borderColor = extended.hairlineBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TuulitBadge(
                                text = pred.currentPhase.title,
                                icon = if (pred.isCurrentlyMenstruating) Icons.Outlined.WaterDrop else Icons.Outlined.CalendarMonth,
                                containerColor = badgeColor,
                                contentColor = badgeInk
                            )
                            if (pred.currentCycleDay != null) {
                                Text(
                                    text = "DAY ${pred.currentCycleDay}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = pred.estimateSummary,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = pred.currentPhase.shortSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (pred.isCurrentlyMenstruating) {
                                TuulitPrimaryButton(
                                    text = "Log period end",
                                    icon = Icons.Outlined.WaterDrop,
                                    onClick = { onLogPeriodEnd(uiState.todayEpochDay) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_period_end_button")
                                )
                            } else {
                                TuulitPrimaryButton(
                                    text = "Period started",
                                    icon = Icons.Outlined.WaterDrop,
                                    onClick = { onLogPeriodStart(uiState.todayEpochDay) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_period_start_button")
                                )
                            }
                            TuulitSecondaryButton(
                                text = "Calendar",
                                icon = Icons.Outlined.CalendarMonth,
                                onClick = onOpenCalendar,
                                modifier = Modifier.testTag("home_calendar_shortcut")
                            )
                        }
                    }
                }

                // Signature Innovative Actions: "I Need Something" & "Today Is Not the Day"
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TuulitCard(
                            containerColor = extended.softYellow,
                            borderColor = extended.bananaYellow,
                            contentPadding = PaddingValues(16.dp),
                            onClick = {
                                selectedNeed = null
                                showNeedSomethingSheet = true
                            },
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("home_i_need_something_card")
                        ) {
                            Icon(
                                Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "I Need Something",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Pain, mood, boredom, or rest triage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        TuulitCard(
                            containerColor = extended.warmTan,
                            borderColor = extended.hairlineBorder,
                            contentPadding = PaddingValues(16.dp),
                            onClick = { onSetTodayNotTheDay(true) },
                            modifier = Modifier
                                .weight(0.95f)
                                .testTag("home_not_the_day_card")
                        ) {
                            Icon(
                                Icons.Outlined.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Today Is Not the Day",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Calm minimal mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Today's Quick Check-In (Mood + Energy + Flow)
                item {
                    val todayLog = uiState.todayLog ?: DailyLog(epochDay = uiState.todayEpochDay)
                    TuulitCard {
                        TuulitSectionHeader(
                            title = "Today's check-in",
                            subtitle = "Tap to log mood or energy quickly",
                            actionLabel = "Full log",
                            onActionClick = onOpenCalendar
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Mood", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        val quickMoods = listOf("Calm", "Happy", "Tired", "Irritated", "Anxious", "Emotional")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickMoods.forEach { mood ->
                                val selected = todayLog.moods.contains(mood)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val updated = if (selected) todayLog.moods - mood else todayLog.moods + mood
                                        onQuickSaveTodayLog(todayLog.copy(moods = updated))
                                    },
                                    label = { Text(mood) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Energy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TrackingCatalog.energyLabels.forEach { (level, label) ->
                                val selected = todayLog.energyLevel == level
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else extended.warmTan)
                                        .clickable { onQuickSaveTodayLog(todayLog.copy(energyLevel = level)) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$level",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                        if (todayLog.symptoms.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Logged symptoms: ${todayLog.symptoms.keys.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Ask Tuulit AI Banner Card
                item {
                    TuulitCard(
                        containerColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.primary,
                        onClick = onOpenAi,
                        modifier = Modifier.testTag("home_ask_tuulit_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = extended.bananaYellow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "ASK TUULIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extended.bananaYellow
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Have a question about your body or symptoms?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Explain My Body, Myth Checker, and Appointment Prep",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(extended.bananaYellow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.SupportAgent,
                                    contentDescription = "Open Tuulit AI",
                                    tint = MaterialTheme.colorScheme.onTertiary
                                )
                            }
                        }
                    }
                }

                // Quick Explore Grid: Care, Play, Pain Journal, Insights, Tuulit SOS
                item {
                    TuulitSectionHeader(title = "Support & insights")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeMiniTile(
                                title = "Pain Journal",
                                subtitle = "${uiState.painLogs.size} entries",
                                icon = Icons.Outlined.LocalFireDepartment,
                                onClick = onOpenPainJournal,
                                modifier = Modifier.weight(1f).testTag("home_pain_journal_tile")
                            )
                            HomeMiniTile(
                                title = "Insights & Map",
                                subtitle = "${uiState.dailyLogs.size} days logged",
                                icon = Icons.Outlined.Insights,
                                onClick = onOpenInsights,
                                modifier = Modifier.weight(1f).testTag("home_insights_tile")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeMiniTile(
                                title = "Tuulit SOS",
                                subtitle = "1-tap support card",
                                icon = Icons.Outlined.Send,
                                onClick = { showSosSheet = true },
                                modifier = Modifier.weight(1f).testTag("home_sos_tile")
                            )
                            HomeMiniTile(
                                title = "Tuulit Connect",
                                subtitle = if (uiState.trustedContacts.isEmpty()) "Optional support" else "${uiState.trustedContacts.size} trusted",
                                icon = Icons.Outlined.Group,
                                onClick = onOpenConnect,
                                modifier = Modifier.weight(1f).testTag("home_connect_tile")
                            )
                        }
                    }
                }

                // Gentle contextual suggestion based on current phase or mood
                item {
                    val suggestionTitle = when {
                        uiState.prediction.isCurrentlyMenstruating -> "Gentle comfort suggestion"
                        uiState.todayLog?.energyLevel in 1..2 -> "Low-energy idea"
                        else -> "Take a quiet moment"
                    }
                    val suggestionBody = when {
                        uiState.prediction.isCurrentlyMenstruating ->
                            "Warm compresses, slow breathing, or a calm round of Banana Catch are ready in Care and Play whenever you want them."
                        uiState.todayLog?.energyLevel in 1..2 ->
                            "You logged lower energy today. Try a 3-minute relaxation session or keep things light."
                        else ->
                            "Customize your Period Survival Kit in Care so your favorite routines are one tap away when you need them."
                    }
                    TuulitCard(containerColor = extended.warmTan) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Spa, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(suggestionTitle, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = suggestionBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TuulitSecondaryButton(text = "Open Care", onClick = onOpenCare)
                            TuulitSecondaryButton(text = "Open Play", onClick = onOpenPlay)
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet: "I Need Something"
    if (showNeedSomethingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNeedSomethingSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("What do you need right now?", style = MaterialTheme.typography.headlineSmall)
                if (selectedNeed == null) {
                    Text(
                        "Pick whatever fits closest. Tuulit will guide you to a gentle next step.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    needOptions.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { option ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { selectedNeed = option },
                                    shape = RoundedCornerShape(16.dp),
                                    color = extended.warmTan,
                                    border = BorderStroke(1.dp, extended.hairlineBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Icon(option.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text(option.label, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            option.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val chosen = selectedNeed!!
                    TuulitCard(containerColor = extended.warmTan) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(chosen.icon, contentDescription = null)
                            Text(chosen.label, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(chosen.recommendation, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TuulitSecondaryButton(
                            text = "Back",
                            onClick = { selectedNeed = null },
                            modifier = Modifier.weight(0.8f)
                        )
                        TuulitPrimaryButton(
                            text = chosen.actionLabel,
                            onClick = {
                                showNeedSomethingSheet = false
                                when (chosen.id) {
                                    "pain", "irritated", "sleep" -> onOpenCare()
                                    "sad", "unsure" -> onOpenCalendar()
                                    "exhausted" -> onSetTodayNotTheDay(true)
                                    "bored" -> onOpenPlay()
                                    "someone" -> onOpenAi()
                                }
                            },
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Modal Sheet: Banana Mode Selector
    if (showBananaModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBananaModeSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Banana Mode", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Choose the expression and vibe you want on your dashboard today.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BananaModePersona.entries.forEach { persona ->
                    val isSelected = uiState.prefs.bananaMode == persona
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) extended.softYellow else extended.warmTan)
                            .border(
                                1.dp,
                                if (isSelected) extended.bananaYellow else extended.hairlineBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSetBananaMode(persona)
                                showBananaModeSheet = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BananaMascot(persona = persona, size = 48.dp, showContainer = false)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(persona.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                persona.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Modal Sheet: Tuulit SOS (1-Tap Share to Trusted Person)
    if (showSosSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSosSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tuulit SOS", style = MaterialTheme.typography.headlineSmall)
                val targetName = uiState.trustedContacts.firstOrNull()?.name
                Text(
                    if (targetName != null) "Tap a message to share with $targetName via your preferred messaging app—no typing required."
                    else "Tap any message below to send to a trusted person without having to type.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sosPresets.forEach { msg ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val shareText = if (targetName != null) {
                                        "Hi $targetName — $msg (sent via Tuulit)"
                                    } else {
                                        "$msg (sent via Tuulit)"
                                    }
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Send Tuulit SOS"))
                                    showSosSheet = false
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = extended.warmTan,
                            border = BorderStroke(1.dp, extended.hairlineBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(msg, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HomeMiniTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TuulitCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayIsNotTheDayView(
    persona: BananaModePersona,
    onExitMinimal: () -> Unit,
    onLogFeeling: () -> Unit,
    onDistract: () -> Unit,
    onRelax: () -> Unit,
    onAskTuulit: () -> Unit,
    onTellMyPerson: () -> Unit,
) {
    val extended = TuulitThemeTokens.extended
    var restingMessage by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                BananaMascot(persona = BananaModePersona.SLEEPY, size = 96.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Let's keep things simple today.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "No charts, no checklists, no pressure. Pick just one thing—or close your eyes and rest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (restingMessage) {
                item {
                    TuulitCard(containerColor = extended.warmTan) {
                        Text("Permission to rest", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Put your phone down whenever you're ready. Tuulit will be right here later.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val actions = listOf(
                Triple("Log how I'm feeling", Icons.Outlined.EditNote, onLogFeeling),
                Triple("Find something to distract me", Icons.Outlined.Games, onDistract),
                Triple("Relax for five minutes", Icons.Outlined.SelfImprovement, onRelax),
                Triple("Ask Tuulit something", Icons.Outlined.AutoAwesome, onAskTuulit),
                Triple("Tell my person", Icons.Outlined.Send, onTellMyPerson),
                Triple("Rest", Icons.Outlined.Bedtime) { restingMessage = true }
            )

            items(actions.size) { index ->
                val (label, icon, action) = actions[index]
                TuulitCard(
                    onClick = action,
                    contentPadding = PaddingValues(18.dp),
                    modifier = Modifier.testTag("minimal_mode_action_$index")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(icon, contentDescription = null)
                        Text(label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onExitMinimal,
                    modifier = Modifier.testTag("exit_not_the_day_button")
                ) {
                    Text("Return to full Tuulit dashboard")
                }
            }
        }
    }
}

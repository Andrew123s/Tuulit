package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.TuulitRepository
import com.example.domain.DailyLog
import com.example.domain.FlowIntensity
import com.example.domain.SymptomSeverity
import com.example.domain.TrackingCatalog
import com.example.ui.TuulitUiState
import com.example.ui.components.TuulitCard
import com.example.ui.components.TuulitNoticeBanner
import com.example.ui.components.TuulitPrimaryButton
import com.example.ui.components.TuulitSecondaryButton
import com.example.ui.theme.TuulitThemeTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    uiState: TuulitUiState,
    onLogPeriodStart: (Long) -> Unit,
    onLogPeriodEnd: (Long) -> Unit,
    onSaveDailyLog: (DailyLog) -> Unit,
    onDeleteDailyLog: (Long) -> Unit,
    onAddCustomMood: (String) -> Unit,
    onAddCustomSymptom: (String) -> Unit,
) {
    val extended = TuulitThemeTokens.extended
    val today = uiState.todayEpochDay

    // Track displayed month via Calendar in UTC
    val initialCal = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = today * 86_400_000L
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    var displayYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0..11
    var selectedEpochDay by remember { mutableLongStateOf(today) }
    var showDayEditorSheet by remember { mutableStateOf(false) }

    val monthCal = remember(displayYear, displayMonth) {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(displayYear, displayMonth, 1, 0, 0, 0)
        }
    }
    val monthTitle = remember(displayYear, displayMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(monthCal.time)
    }
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeekOffset = monthCal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0
    val monthStartEpochDay = monthCal.timeInMillis / 86_400_000L

    val logsByDay = remember(uiState.dailyLogs) { uiState.dailyLogs.associateBy { it.epochDay } }
    val selectedLog = logsByDay[selectedEpochDay]

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Cycle Calendar", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Recorded dates and calculated estimates are clearly separated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Month Navigation & Grid Card
            item {
                TuulitCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayMonth == 0) {
                                    displayMonth = 11
                                    displayYear--
                                } else displayMonth--
                            },
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        Text(monthTitle, style = MaterialTheme.typography.titleLarge)
                        IconButton(
                            onClick = {
                                if (displayMonth == 11) {
                                    displayMonth = 0
                                    displayYear++
                                } else displayMonth++
                            },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Weekday Headers
                    val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdays.forEach { d ->
                            Text(
                                text = d,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val totalCells = firstDayOfWeekOffset + daysInMonth
                    val rows = (totalCells + 6) / 7
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val cellIdx = row * 7 + col
                                    val dayNum = cellIdx - firstDayOfWeekOffset + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val epoch = monthStartEpochDay + (dayNum - 1)
                                        val isLoggedPeriod = uiState.prediction.loggedPeriodDays.contains(epoch)
                                        val isPredictedPeriod = uiState.prediction.predictedPeriodDays.contains(epoch)
                                        val isFertile = uiState.prediction.estimatedFertileDays.contains(epoch)
                                        val isOvulation = uiState.prediction.estimatedOvulationDay == epoch
                                        val hasLog = logsByDay.containsKey(epoch)
                                        val isSelected = epoch == selectedEpochDay
                                        val isToday = epoch == today

                                        val cellBg = when {
                                            isLoggedPeriod -> extended.periodClay
                                            isPredictedPeriod -> extended.periodClaySoft
                                            isFertile || isOvulation -> extended.estimateOchreSoft
                                            isSelected -> extended.warmTan
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                        val cellInk = when {
                                            isLoggedPeriod -> MaterialTheme.colorScheme.onError
                                            else -> MaterialTheme.colorScheme.onBackground
                                        }
                                        val border = when {
                                            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                            isPredictedPeriod -> BorderStroke(1.5.dp, extended.periodClay)
                                            isOvulation -> BorderStroke(1.5.dp, extended.estimateOchre)
                                            isToday -> BorderStroke(1.5.dp, extended.bananaYellow)
                                            else -> BorderStroke(0.5.dp, extended.hairlineBorder.copy(alpha = 0.6f))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(cellBg)
                                                .border(border, RoundedCornerShape(12.dp))
                                                .clickable { selectedEpochDay = epoch },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = cellInk
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (isLoggedPeriod) {
                                                        Icon(
                                                            Icons.Outlined.WaterDrop,
                                                            contentDescription = "Logged period",
                                                            tint = cellInk,
                                                            modifier = Modifier.size(9.dp)
                                                        )
                                                    } else if (isOvulation) {
                                                        Icon(
                                                            Icons.Outlined.LocalFlorist,
                                                            contentDescription = "Estimated ovulation",
                                                            tint = extended.estimateOchre,
                                                            modifier = Modifier.size(9.dp)
                                                        )
                                                    }
                                                    if (hasLog) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isLoggedPeriod) extended.bananaYellow else MaterialTheme.colorScheme.primary)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Accessible non-color-only legend
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LegendItem("Logged period", extended.periodClay, true, Icons.Outlined.WaterDrop)
                        LegendItem("Estimated period", extended.periodClaySoft, false, null, extended.periodClay)
                        LegendItem("Est. fertile / ovulation", extended.estimateOchreSoft, false, Icons.Outlined.LocalFlorist, extended.estimateOchre)
                        LegendItem("Has daily log", MaterialTheme.colorScheme.primary, true, null)
                    }
                }
            }

            // Selected Day Summary Card
            item {
                TuulitCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = TuulitRepository.epochDayToReadable(selectedEpochDay),
                                style = MaterialTheme.typography.titleLarge
                            )
                            val statusText = when {
                                uiState.prediction.loggedPeriodDays.contains(selectedEpochDay) -> "Recorded period day"
                                uiState.prediction.predictedPeriodDays.contains(selectedEpochDay) -> "Estimated upcoming period day"
                                uiState.prediction.estimatedOvulationDay == selectedEpochDay -> "Estimated ovulation day (not contraception)"
                                uiState.prediction.estimatedFertileDays.contains(selectedEpochDay) -> "Estimated fertile window (not contraception)"
                                else -> "Regular tracking day"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedLog != null) {
                            IconButton(onClick = { onDeleteDailyLog(selectedEpochDay) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear day entry")
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (selectedLog == null) {
                        Text(
                            "No symptoms, mood, or notes logged for this date yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (selectedLog.flow != FlowIntensity.NONE) {
                                Text("Flow: ${selectedLog.flow.label}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (selectedLog.moods.isNotEmpty()) {
                                Text("Moods: ${selectedLog.moods.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (selectedLog.energyLevel in 1..5) {
                                val eLabel = TrackingCatalog.energyLabels.firstOrNull { it.first == selectedLog.energyLevel }?.second ?: ""
                                Text("Energy: ${selectedLog.energyLevel}/5 ($eLabel)", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (selectedLog.symptoms.isNotEmpty()) {
                                val symStr = selectedLog.symptoms.entries.joinToString(", ") { "${it.key} (${it.value.label})" }
                                Text("Symptoms: $symStr", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (selectedLog.sleepQuality in 1..5 || selectedLog.waterGlasses > 0 || selectedLog.medicationNote.isNotBlank()) {
                                val lifestyle = buildList {
                                    if (selectedLog.sleepQuality in 1..5) add("Sleep ${selectedLog.sleepQuality}/5")
                                    if (selectedLog.waterGlasses > 0) add("Water ${selectedLog.waterGlasses} glasses")
                                    if (selectedLog.medicationNote.isNotBlank()) add("Meds: ${selectedLog.medicationNote}")
                                }.joinToString(" • ")
                                Text(lifestyle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (selectedLog.journalText.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Journal: ${selectedLog.journalText}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TuulitPrimaryButton(
                            text = if (selectedLog == null) "Add / Log Day" else "Edit Day Log",
                            onClick = { showDayEditorSheet = true },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("calendar_edit_day_button")
                        )
                        TuulitSecondaryButton(
                            text = "Mark Start",
                            onClick = { onLogPeriodStart(selectedEpochDay) },
                            modifier = Modifier.weight(0.9f)
                        )
                        TuulitSecondaryButton(
                            text = "Mark End",
                            onClick = { onLogPeriodEnd(selectedEpochDay) },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            }

            // Mandatory non-contraceptive estimate notice
            item {
                TuulitNoticeBanner(
                    title = "Estimates only — not contraception",
                    message = "Predicted period dates, fertile windows, and ovulation markers are mathematical estimates from your logs and must not be relied upon to prevent or plan pregnancy.",
                    icon = Icons.Outlined.Info
                )
            }
        }
    }

    if (showDayEditorSheet) {
        DailyLogEditorSheet(
            epochDay = selectedEpochDay,
            initialLog = selectedLog ?: DailyLog(epochDay = selectedEpochDay),
            customMoods = uiState.prefs.customMoods,
            customSymptoms = uiState.prefs.customSymptoms,
            onDismiss = { showDayEditorSheet = false },
            onSave = { updated ->
                onSaveDailyLog(updated)
                showDayEditorSheet = false
            },
            onAddCustomMood = onAddCustomMood,
            onAddCustomSymptom = onAddCustomSymptom
        )
    }
}

@Composable
private fun LegendItem(
    label: String,
    fill: androidx.compose.ui.graphics.Color,
    filled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    borderColor: androidx.compose.ui.graphics.Color = fill,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(fill)
                .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (filled) MaterialTheme.colorScheme.onError else borderColor,
                    modifier = Modifier.size(9.dp)
                )
            }
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DailyLogEditorSheet(
    epochDay: Long,
    initialLog: DailyLog,
    customMoods: List<String>,
    customSymptoms: List<String>,
    onDismiss: () -> Unit,
    onSave: (DailyLog) -> Unit,
    onAddCustomMood: (String) -> Unit,
    onAddCustomSymptom: (String) -> Unit,
) {
    var flow by remember { mutableStateOf(initialLog.flow) }
    var symptoms by remember { mutableStateOf(initialLog.symptoms) }
    var moods by remember { mutableStateOf(initialLog.moods) }
    var energy by remember { mutableIntStateOf(initialLog.energyLevel) }
    var sleep by remember { mutableIntStateOf(initialLog.sleepQuality) }
    var water by remember { mutableIntStateOf(initialLog.waterGlasses) }
    var medication by remember { mutableStateOf(initialLog.medicationNote) }
    var exercise by remember { mutableStateOf(initialLog.exerciseNote) }
    var journal by remember { mutableStateOf(initialLog.journalText) }

    var newMoodText by remember { mutableStateOf("") }
    var newSymptomText by remember { mutableStateOf("") }

    val allMoods = remember(customMoods) { (TrackingCatalog.defaultMoods + customMoods).distinct() }
    val allSymptoms = remember(customSymptoms) { (TrackingCatalog.defaultSymptoms + customSymptoms).distinct() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Log for ${TuulitRepository.epochDayToReadable(epochDay)}",
                style = MaterialTheme.typography.headlineSmall
            )

            // Flow intensity
            Text("Flow intensity", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowIntensity.entries.forEach { item ->
                    FilterChip(
                        selected = flow == item,
                        onClick = { flow = item },
                        label = { Text(item.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TuulitThemeTokens.extended.periodClay,
                            selectedLabelColor = MaterialTheme.colorScheme.onError
                        )
                    )
                }
            }

            // Symptoms + tap to cycle severity (Off -> Mild -> Moderate -> Severe -> Off)
            Text("Symptoms (tap to cycle Mild / Moderate / Severe)", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                allSymptoms.forEach { symptom ->
                    val sev = symptoms[symptom]
                    FilterChip(
                        selected = sev != null,
                        onClick = {
                            val next = when (sev) {
                                null -> SymptomSeverity.MILD
                                SymptomSeverity.MILD -> SymptomSeverity.MODERATE
                                SymptomSeverity.MODERATE -> SymptomSeverity.SEVERE
                                SymptomSeverity.SEVERE -> null
                            }
                            symptoms = if (next == null) symptoms - symptom else symptoms + (symptom to next)
                        },
                        label = {
                            Text(if (sev != null) "$symptom • ${sev.label}" else symptom)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newSymptomText,
                    onValueChange = { newSymptomText = it.take(28) },
                    placeholder = { Text("Add custom symptom") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newSymptomText.isNotBlank()) {
                            onAddCustomSymptom(newSymptomText)
                            symptoms = symptoms + (newSymptomText.trim() to SymptomSeverity.MILD)
                            newSymptomText = ""
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add custom symptom")
                }
            }

            // Moods
            Text("Moods", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                allMoods.forEach { mood ->
                    val selected = moods.contains(mood)
                    FilterChip(
                        selected = selected,
                        onClick = { moods = if (selected) moods - mood else moods + mood },
                        label = { Text(mood) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newMoodText,
                    onValueChange = { newMoodText = it.take(24) },
                    placeholder = { Text("Add custom mood") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newMoodText.isNotBlank()) {
                            onAddCustomMood(newMoodText)
                            moods = (moods + newMoodText.trim()).distinct()
                            newMoodText = ""
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add custom mood")
                }
            }

            // Energy & Sleep (1..5)
            Text("Energy level (1 = very low, 5 = very high)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { lvl ->
                    FilterChip(
                        selected = energy == lvl,
                        onClick = { energy = if (energy == lvl) 0 else lvl },
                        label = { Text("$lvl") }
                    )
                }
            }

            Text("Sleep quality (optional, 1–5)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { lvl ->
                    FilterChip(
                        selected = sleep == lvl,
                        onClick = { sleep = if (sleep == lvl) 0 else lvl },
                        label = { Text("$lvl") }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Water glasses (optional): $water", style = MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(onClick = { if (water > 0) water-- }) { Text("-1") }
                    TextButton(onClick = { if (water < 20) water++ }) { Text("+1") }
                }
            }

            OutlinedTextField(
                value = medication,
                onValueChange = { medication = it.take(80) },
                label = { Text("Medication / supplements (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = exercise,
                onValueChange = { exercise = it.take(80) },
                label = { Text("Movement / exercise (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = journal,
                onValueChange = { journal = it.take(1000) },
                label = { Text("Private daily journal note") },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_log_journal_input")
            )

            TuulitPrimaryButton(
                text = "Save Day Log",
                onClick = {
                    onSave(
                        initialLog.copy(
                            flow = flow,
                            symptoms = symptoms,
                            moods = moods,
                            energyLevel = energy,
                            sleepQuality = sleep,
                            waterGlasses = water,
                            medicationNote = medication.trim(),
                            exerciseNote = exercise.trim(),
                            journalText = journal.trim()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_daily_log_button")
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

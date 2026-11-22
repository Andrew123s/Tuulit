package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.TuulitRepository
import com.example.domain.PainLog
import com.example.domain.TrackingCatalog
import com.example.ui.TuulitUiState
import com.example.ui.components.TuulitBadge
import com.example.ui.components.TuulitCard
import com.example.ui.components.TuulitNoticeBanner
import com.example.ui.components.TuulitPrimaryButton
import com.example.ui.theme.TuulitThemeTokens
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(
    uiState: TuulitUiState,
    onInsightsViewed: () -> Unit,
    onExportCsv: () -> String,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    LaunchedEffect(Unit) { onInsightsViewed() }

    val context = LocalContext.current
    val extended = TuulitThemeTokens.extended
    val insights = uiState.insights

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("insights_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Tuulit Insights & Mood Map", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Patterns generated strictly from your recorded logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Key Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        label = "Avg Cycle",
                        value = insights.averageCycleLength?.let { "${it}d" } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Avg Period",
                        value = insights.averagePeriodDuration?.let { "${it}d" } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Variability",
                        value = insights.cycleVariabilityDays?.let { "±${it}d" } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Insufficient Data Honest Notice
            if (!insights.hasEnoughDataForTrends) {
                item {
                    TuulitNoticeBanner(
                        title = "Building your personal baseline",
                        message = "Tuulit never fabricates patterns. Log at least 2 completed cycles or 7 daily check-ins to unlock cycle length charts and multi-cycle trend notes.",
                        icon = Icons.Outlined.Info
                    )
                }
            }

            // Data-Driven Trend Notes
            if (insights.dataDrivenNotes.isNotEmpty()) {
                item {
                    TuulitCard(containerColor = extended.warmTan) {
                        Text("Observed Patterns", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Correlations reflect your logged entries and are not medical diagnoses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        insights.dataDrivenNotes.forEach { note ->
                            Text("• $note", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }

            // Flat Canvas Bar Chart: Cycle Lengths History
            if (insights.cycleLengthsHistory.isNotEmpty()) {
                item {
                    val barFill = extended.bananaYellow
                    val barBorder = MaterialTheme.colorScheme.primary
                    TuulitCard {
                        Text("Cycle Length History (days)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        val maxVal = (insights.cycleLengthsHistory.maxOfOrNull { it.second } ?: 35).coerceAtLeast(35)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val count = insights.cycleLengthsHistory.size
                            val slotW = size.width / count
                            val barW = (slotW * 0.55f).coerceAtMost(56f)
                            insights.cycleLengthsHistory.forEachIndexed { idx, (_, days) ->
                                val ratio = days.toFloat() / maxVal.toFloat()
                                val barH = (size.height * 0.85f * ratio).coerceAtLeast(12f)
                                val left = idx * slotW + (slotW - barW) / 2f
                                val top = size.height - barH
                                drawRoundRect(
                                    color = barFill,
                                    topLeft = Offset(left, top),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                                drawRoundRect(
                                    color = barBorder,
                                    topLeft = Offset(left, top),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(10f, 10f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            insights.cycleLengthsHistory.forEach { (lbl, days) ->
                                Text("$lbl: ${days}d", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Cycle Mood Map
            item {
                TuulitCard {
                    Text("Cycle Mood Map", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "How your logged moods, symptoms, and energy distribute across phases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    insights.phaseMoodMap.forEach { phaseSummary ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(extended.warmTan)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(phaseSummary.phase.title, style = MaterialTheme.typography.titleSmall)
                                Text("${phaseSummary.sampleDays} days", style = MaterialTheme.typography.labelSmall)
                            }
                            if (phaseSummary.sampleDays == 0) {
                                Text(
                                    "Not enough logs in this phase yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                if (phaseSummary.topMoods.isNotEmpty()) {
                                    Text("Top moods: ${phaseSummary.topMoods.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (phaseSummary.topSymptoms.isNotEmpty()) {
                                    Text("Common symptoms: ${phaseSummary.topSymptoms.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                                }
                                phaseSummary.averageEnergy?.let {
                                    Text("Avg energy: ${String.format("%.1f", it)}/5", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Symptom & Mood Frequency Bars
            if (insights.symptomFrequency.isNotEmpty() || insights.moodFrequency.isNotEmpty()) {
                item {
                    TuulitCard {
                        Text("Logged Frequency", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (insights.symptomFrequency.isNotEmpty()) {
                            Text("Symptoms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val maxSym = insights.symptomFrequency.maxOf { it.second }.coerceAtLeast(1)
                            insights.symptomFrequency.forEach { (name, count) ->
                                FrequencyBarRow(label = name, count = count, max = maxSym, barColor = extended.periodClay)
                            }
                        }
                        if (insights.moodFrequency.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("Moods", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val maxMood = insights.moodFrequency.maxOf { it.second }.coerceAtLeast(1)
                            insights.moodFrequency.forEach { (name, count) ->
                                FrequencyBarRow(label = name, count = count, max = maxMood, barColor = extended.bananaYellow)
                            }
                        }
                    }
                }
            }

            // Export CSV Button
            item {
                TuulitPrimaryButton(
                    text = "Export Cycle & Symptom History (CSV)",
                    icon = Icons.Outlined.FileDownload,
                    onClick = {
                        val csv = onExportCsv()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Tuulit Health & Symptom Export")
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(Intent.createChooser(intent, "Export Tuulit History"))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("insights_export_csv_button")
                )
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    TuulitCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun FrequencyBarRow(
    label: String,
    count: Int,
    max: Int,
    barColor: androidx.compose.ui.graphics.Color
) {
    val ratio = (count.toFloat() / max.toFloat()).coerceIn(0.08f, 1f)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$count", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(TuulitThemeTokens.extended.warmTan)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(barColor)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PainJournalScreen(
    uiState: TuulitUiState,
    onSavePainLog: (PainLog) -> Unit,
    onDeletePainLog: (Long) -> Unit,
    onExportCsv: () -> String,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val extended = TuulitThemeTokens.extended

    var intensity by remember { mutableIntStateOf(5) }
    var location by remember { mutableStateOf(TrackingCatalog.painLocations.first()) }
    var painType by remember { mutableStateOf(TrackingCatalog.painTypes.first()) }
    var durationHours by remember { mutableIntStateOf(2) }
    var medication by remember { mutableStateOf("") }
    var impactWork by remember { mutableStateOf(false) }
    var impactExercise by remember { mutableStateOf(false) }
    var impactSleep by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("pain_journal_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Pain Journal", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Track intensity, impact, and patterns for medical visits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            uiState.insights.painPatternAlert?.let { alert ->
                item {
                    TuulitNoticeBanner(
                        title = if (alert.hasRecurringSeverePain) "Recurring high-pain pattern noticed" else "Pain log observation",
                        message = alert.summaryMessage,
                        icon = Icons.Outlined.WarningAmber,
                        containerColor = if (alert.hasRecurringSeverePain) extended.alertSoft else extended.estimateOchreSoft,
                        accentColor = if (alert.hasRecurringSeverePain) MaterialTheme.colorScheme.error else extended.estimateOchre
                    )
                }
            }

            item {
                TuulitCard {
                    Text("Record Pain Entry (Today)", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Intensity", style = MaterialTheme.typography.titleSmall)
                        Text("$intensity / 10", style = MaterialTheme.typography.labelLarge, color = if (intensity >= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground)
                    }
                    Slider(
                        value = intensity.toFloat(),
                        onValueChange = { intensity = it.roundToInt() },
                        valueRange = 1f..10f,
                        steps = 8
                    )

                    Text("Location", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TrackingCatalog.painLocations.forEach { loc ->
                            FilterChip(
                                selected = location == loc,
                                onClick = { location = loc },
                                label = { Text(loc) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Type of pain", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TrackingCatalog.painTypes.forEach { type ->
                            FilterChip(
                                selected = painType == type,
                                onClick = { painType = type },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Duration", style = MaterialTheme.typography.titleSmall)
                        Text("$durationHours hours", style = MaterialTheme.typography.labelLarge)
                    }
                    Slider(
                        value = durationHours.toFloat(),
                        onValueChange = { durationHours = it.roundToInt() },
                        valueRange = 1f..24f
                    )

                    Text("Daily activity impact", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = impactWork, onCheckedChange = { impactWork = it })
                        Text("Affected work or study", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = impactExercise, onCheckedChange = { impactExercise = it })
                        Text("Affected movement / exercise", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = impactSleep, onCheckedChange = { impactSleep = it })
                        Text("Affected sleep", style = MaterialTheme.typography.bodyMedium)
                    }

                    OutlinedTextField(
                        value = medication,
                        onValueChange = { medication = it.take(80) },
                        label = { Text("Medication or relief tried (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(300) },
                        label = { Text("Notes for yourself or your clinician") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    TuulitPrimaryButton(
                        text = "Save Pain Entry",
                        icon = Icons.Outlined.Add,
                        onClick = {
                            onSavePainLog(
                                PainLog(
                                    epochDay = uiState.todayEpochDay,
                                    intensity = intensity,
                                    location = location,
                                    painType = painType,
                                    durationHours = durationHours,
                                    medicationTaken = medication.trim(),
                                    impactedWorkOrStudy = impactWork,
                                    impactedExercise = impactExercise,
                                    impactedSleep = impactSleep,
                                    notes = notes.trim()
                                )
                            )
                            medication = ""
                            notes = ""
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_pain_log_button")
                    )
                }
            }

            if (uiState.painLogs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recorded Pain History", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Tuulit Pain Journal Summary")
                                putExtra(Intent.EXTRA_TEXT, onExportCsv())
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Pain Summary"))
                        }) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = "Share pain summary")
                        }
                    }
                }
                items(uiState.painLogs, key = { it.id }) { log ->
                    TuulitCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                TuulitBadge(
                                    text = "${log.intensity}/10",
                                    containerColor = if (log.intensity >= 7) extended.alertSoft else extended.warmTan,
                                    contentColor = if (log.intensity >= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                                )
                                Text(TuulitRepository.epochDayToReadable(log.epochDay), style = MaterialTheme.typography.titleSmall)
                            }
                            IconButton(onClick = { onDeletePainLog(log.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete pain log")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${log.location} • ${log.painType} • ${log.durationHours}h", style = MaterialTheme.typography.bodyMedium)
                        if (log.medicationTaken.isNotBlank()) {
                            Text("Relief tried: ${log.medicationTaken}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val impacts = listOfNotNull(
                            if (log.impactedWorkOrStudy) "Work/study" else null,
                            if (log.impactedExercise) "Exercise" else null,
                            if (log.impactedSleep) "Sleep" else null
                        )
                        if (impacts.isNotEmpty()) {
                            Text("Impacted: ${impacts.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = extended.periodClay)
                        }
                        if (log.notes.isNotBlank()) {
                            Text(log.notes, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

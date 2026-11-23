package app.tuulit.ui.cycle

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.tuulit.domain.Achievement
import app.tuulit.domain.CyclePhase
import app.tuulit.domain.Dates
import app.tuulit.domain.MedicalSafetyGuard
import app.tuulit.domain.PainLog
import app.tuulit.domain.TrackingCatalog
import app.tuulit.domain.UrgencyLevel
import app.tuulit.ui.LocalShell
import app.tuulit.ui.ScreenColumn
import app.tuulit.ui.VSpace
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.AppBar
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Chip
import app.tuulit.ui.designsystem.ChipRow
import app.tuulit.ui.designsystem.EmptyState
import app.tuulit.ui.designsystem.Field
import app.tuulit.ui.designsystem.GroupTitle
import app.tuulit.ui.designsystem.Hairline
import app.tuulit.ui.designsystem.IconBtn
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.Notice
import app.tuulit.ui.designsystem.NoticeKind
import app.tuulit.ui.designsystem.Panel
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import kotlinx.coroutines.launch
import java.io.File

/** Shares a CSV export through the system share sheet. The file lives in the cache only. */
suspend fun shareExport(ctx: android.content.Context, csv: String) {
    val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "tuulit-export-${Dates.iso(Dates.today())}.csv")
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(send, "Export Tuulit data").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun InsightsScreen(onBack: () -> Unit) {
    val c = container()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snap by c.cycleState.snapshot.collectAsState()
    val i = snap.insights
    LaunchedEffect(Unit) { c.carePlay.unlock(Achievement.INSIGHTS_EXPLORED) }
    ScreenColumn {
        AppBar("Insights", onBack) { IconBtn(TuulitIcons.Download, "Export my data as CSV", { scope.launch { shareExport(ctx, c.tracking.exportCsv()) } }) }
        val lengths = i.cycleLengthsHistory.map { it.second }
        if (i.completedCyclesCount < 2) {
            T("Insights", Tuulit.type.display, modifier = Modifier.padding(top = 10.dp))
            EmptyState("Building your baseline",
                if (i.completedCyclesCount == 0) "Log two periods and your cycle patterns will appear here." else "Log one more period and your patterns will appear here.")
        } else {
            T("Your cycles run ${lengths.min()}–${lengths.max()} days.", Tuulit.type.display, modifier = Modifier.padding(top = 10.dp))
            val typical = lengths.all { it in 21..35 }
            T(if (typical) "That's within the typical range. Based on ${i.completedCyclesCount} cycles." else "Some cycles fall outside 21–35 days. Worth noting for a check-up.",
                Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp, bottom = 18.dp))
            Hairline()
            Row(Modifier.fillMaxWidth().height(70.dp)) {
                Stat("${i.averageCycleLength ?: "–"} d", "Avg cycle", Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(70.dp).background(Tuulit.colors.hairline))
                Stat("${i.averagePeriodDuration ?: "–"} d", "Avg period", Modifier.weight(1f).padding(start = 14.dp))
                Box(Modifier.width(1.dp).height(70.dp).background(Tuulit.colors.hairline))
                Stat("±${(i.cycleVariabilityDays ?: 0) / 2} d", "Variation", Modifier.weight(1f).padding(start = 14.dp))
            }
            Hairline()
            CycleBars(i.cycleLengthsHistory, i.averageCycleLength ?: 28, snap.prediction.currentCycleDay, Modifier.padding(top = 20.dp))
        }
        T("Cycle Mood Map", Tuulit.type.subtitle, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
        val map = i.phaseMoodMap
        val moods = map.flatMap { it.topMoods }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(4).map { it.key }
        if (moods.isEmpty()) {
            T("Log moods for a few days in different phases and the map fills in.", Tuulit.type.bodySm, Tuulit.colors.inkMuted)
        } else {
            MoodMap(moods, map.map { s -> s.phase to s.topMoods }, snap.logs, snap)
        }
        if (i.symptomFrequency.isNotEmpty()) {
            T("Most logged symptoms", Tuulit.type.subtitle, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
            val max = i.symptomFrequency.maxOf { it.second }.toFloat()
            i.symptomFrequency.take(5).forEach { (name, count) -> FreqBar(name, count, count / max) }
        }
        if (i.dataDrivenNotes.isNotEmpty()) {
            GroupTitle("Observed patterns", Modifier.padding(top = 24.dp))
            i.dataDrivenNotes.forEachIndexed { idx, n -> ListRow(n, icon = TuulitIcons.Spark, last = idx == i.dataDrivenNotes.lastIndex) }
        }
        T("Patterns describe your own logs. They aren't a diagnosis.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) = Column(modifier.padding(vertical = 14.dp)) {
    T(value, Tuulit.type.title); T(label, Tuulit.type.caption, Tuulit.colors.inkMuted)
}

@Composable
private fun CycleBars(history: List<Pair<String, Int>>, avg: Int, currentDay: Int?, modifier: Modifier) {
    val c = Tuulit.colors
    val values = history.map { it.second } + listOfNotNull(currentDay)
    val maxV = (values.maxOrNull() ?: avg).coerceAtLeast(avg) + 2
    val minV = ((values.minOrNull() ?: avg) - 8).coerceAtLeast(0)
    fun h(v: Int) = (16 + (v - minV).toFloat() / (maxV - minV) * 90).dp
    Box(modifier.fillMaxWidth().height(160.dp).semantics(mergeDescendants = true) {
        contentDescription = "Cycle lengths: ${history.joinToString { "${it.second} days" }}. Average $avg days."
    }) {
        Box(Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(bottom = 20.dp + h(avg)).height(1.5.dp).background(c.inkFaint))
        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            history.takeLast(6).forEachIndexed { idx, (_, v) -> Bar(v.toString(), "C${idx + 1}", h(v), false, Modifier.weight(1f)) }
            if (currentDay != null) Bar(currentDay.toString(), "Now", h(currentDay), true, Modifier.weight(1f))
        }
        T("avg $avg", Tuulit.type.caption, c.inkMuted, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun Bar(value: String, label: String, height: androidx.compose.ui.unit.Dp, now: Boolean, modifier: Modifier) {
    val c = Tuulit.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        T(value, Tuulit.type.caption, if (now) c.ink else c.inkMuted)
        Box(Modifier.widthIn(max = 28.dp).fillMaxWidth().height(height).clip(RoundedCornerShape(8.dp, 8.dp, 4.dp, 4.dp)).background(if (now) c.period else c.peach))
        T(label, Tuulit.type.caption, c.inkFaint)
    }
}

@Composable
private fun MoodMap(moods: List<String>, phases: List<Pair<CyclePhase, List<String>>>, logs: List<app.tuulit.domain.DailyLog>, snap: app.tuulit.data.CycleSnapshot) {
    val c = Tuulit.colors
    // Count how often each mood was logged per phase (from the same phase buckets the calculator uses).
    val counts = snap.insights.phaseMoodMap.associate { s -> s.phase to s }
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.width(72.dp))
        listOf("Period", "Follic.", "Ovul.", "Luteal").forEach { T(it, Tuulit.type.caption, c.inkMuted, Modifier.weight(1f), align = TextAlign.Center) }
    }
    moods.forEach { mood ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            T(mood, Tuulit.type.caption, c.ink, Modifier.width(72.dp))
            listOf(CyclePhase.MENSTRUAL, CyclePhase.FOLLICULAR, CyclePhase.ESTIMATED_OVULATION, CyclePhase.LUTEAL).forEach { ph ->
                val s = counts[ph]
                val rank = s?.topMoods?.indexOf(mood) ?: -1
                val size = when (rank) { 0 -> 22; 1 -> 15; 2 -> 10; else -> 0 }
                Box(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.Center) {
                    if (size > 0) Box(Modifier.size(size.dp).clip(CircleShape).background(c.toffee))
                    else Box(Modifier.size(4.dp).clip(CircleShape).background(c.hairline))
                }
            }
        }
    }
    T("Bigger dots mean that mood was logged more often in that phase.", Tuulit.type.caption, c.inkFaint, Modifier.padding(top = 6.dp))
}

@Composable
private fun FreqBar(name: String, count: Int, fraction: Float) {
    val c = Tuulit.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        T(name, Tuulit.type.bodySm, modifier = Modifier.width(110.dp), maxLines = 1)
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.sand)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0.05f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.toffee))
        }
        T(count.toString(), Tuulit.type.caption, c.inkMuted, Modifier.padding(start = 10.dp).width(24.dp))
    }
}

@Composable
fun PainJournalScreen(onBack: () -> Unit, go: (String) -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val snap by c.cycleState.snapshot.collectAsState()
    var intensity by remember { mutableIntStateOf(0) }
    var location by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf<String?>(null) }
    var work by remember { mutableStateOf(false) }
    var exercise by remember { mutableStateOf(false) }
    var sleep by remember { mutableStateOf(false) }
    var relief by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var hours by remember { mutableIntStateOf(2) }
    var more by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val warn = MedicalSafetyGuard.assess(notes)
    ScreenColumn {
        AppBar("Pain Journal", onBack)
        Panel(Modifier.padding(top = 8.dp), padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("How bad is it right now?", Tuulit.type.subtitle, modifier = Modifier.weight(1f))
                if (intensity > 0) T("$intensity/10", Tuulit.type.title, Tuulit.colors.period)
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..10).forEach { n ->
                    val on = n == intensity; val fill = n < intensity
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (on) Tuulit.colors.period else if (fill) Tuulit.colors.periodSoft else Tuulit.colors.sand)
                        .clickable(role = Role.RadioButton, onClickLabel = "Pain $n of 10") { intensity = n; error = null },
                        contentAlignment = Alignment.Center) { T("$n", Tuulit.type.label, if (on) Tuulit.colors.onPeriod else Tuulit.colors.ink) }
                }
            }
            GroupTitle("Where", Modifier.padding(top = 16.dp))
            ChipRow { TrackingCatalog.painLocations.forEach { l -> Chip(l, location == l, { location = if (location == l) null else l }) } }
            GroupTitle("It stopped me from", Modifier.padding(top = 14.dp))
            ChipRow {
                Chip("Work or study", work, { work = !work }); Chip("Exercise", exercise, { exercise = !exercise }); Chip("Sleep", sleep, { sleep = !sleep })
            }
            TButton(if (more) "Fewer details" else "More details", { more = !more }, kind = ButtonKind.TEXT)
            if (more) {
                GroupTitle("Type of pain")
                ChipRow { TrackingCatalog.painTypes.forEach { t -> Chip(t, type == t, { type = if (type == t) null else t }) } }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    T("Lasted about $hours ${if (hours == 1) "hour" else "hours"}", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.weight(1f))
                    IconBtn(TuulitIcons.Minus, "Shorter", { hours = (hours - 1).coerceAtLeast(0) }); Box(Modifier.width(8.dp))
                    IconBtn(TuulitIcons.Plus, "Longer", { hours = (hours + 1).coerceAtMost(72) })
                }
                Field("Relief tried", relief, { relief = it.take(120) }, Modifier.padding(top = 12.dp), placeholder = "e.g. heat, ibuprofen")
                Field("Notes for you or your clinician", notes, { notes = it.take(500) }, Modifier.padding(top = 12.dp), singleLine = false, minLines = 2)
            }
            if (warn.level == UrgencyLevel.URGENT || warn.level == UrgencyLevel.EMERGENCY || intensity >= 9) {
                Notice(warn.headline.ifEmpty { "Pain this severe deserves care" },
                    warn.message.ifEmpty { "If painkillers don't help or you also have fever, fainting or heavy bleeding, please see a doctor today." },
                    Modifier.padding(top = 12.dp), kind = NoticeKind.MEDICAL)
            }
            error?.let { T(it, Tuulit.type.caption, Tuulit.colors.alert, Modifier.padding(top = 8.dp)) }
            TButton("Save entry", {
                if (intensity == 0) { error = "Choose a number from 1 to 10."; return@TButton }
                scope.launch {
                    c.tracking.savePainLog(PainLog(epochDay = snap.today, intensity = intensity, location = location.orEmpty(), painType = type.orEmpty(),
                        durationHours = hours, medicationTaken = relief.trim(), impactedWorkOrStudy = work, impactedExercise = exercise,
                        impactedSleep = sleep, notes = notes.trim()))
                    c.carePlay.unlock(Achievement.PAIN_JOURNAL)
                    intensity = 0; location = null; type = null; work = false; exercise = false; sleep = false; relief = ""; notes = ""; more = false
                    shell.showToast("Pain entry saved")
                }
            }, Modifier.fillMaxWidth().padding(top = 16.dp))
        }
        snap.insights.painPatternAlert?.takeIf { it.hasRecurringSeverePain }?.let { a ->
            Notice("Severe pain ${a.consecutiveHighPainCycles} cycles in a row", "Worth raising with a clinician.", Modifier.padding(top = 14.dp), action = {
                TButton("Prepare for the appointment", { go("ask/chat/appointment") }, kind = ButtonKind.TEXT)
            })
        }
        if (snap.painLogs.isEmpty()) {
            T("Your entries will appear here.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 16.dp))
        } else {
            GroupTitle("History", Modifier.padding(top = 20.dp))
            snap.painLogs.forEachIndexed { idx, l ->
                val impacts = listOfNotNull("missed work".takeIf { l.impactedWorkOrStudy }, "no exercise".takeIf { l.impactedExercise }, "poor sleep".takeIf { l.impactedSleep })
                ListRow("${Dates.short(l.epochDay)}${if (l.painType.isNotEmpty()) " · " + l.painType.lowercase() else ""}",
                    sub = (listOf(l.location) + impacts + listOf(l.medicationTaken)).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { null },
                    leading = { T("${l.intensity}", Tuulit.type.title, Tuulit.colors.period, Modifier.width(32.dp)) },
                    trailing = { IconBtn(TuulitIcons.Trash, "Delete entry", {
                        scope.launch { c.tracking.deletePainLog(l.id); shell.showToast("Entry deleted") { scope.launch { c.tracking.savePainLog(l) } } }
                    }) }, last = idx == snap.painLogs.lastIndex)
            }
        }
    }
}

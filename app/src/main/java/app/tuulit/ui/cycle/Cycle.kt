package app.tuulit.ui.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import app.tuulit.ui.designsystem.ChipRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tuulit.domain.CycleRecord
import app.tuulit.domain.Dates
import app.tuulit.domain.FlowIntensity
import app.tuulit.ui.LocalShell
import app.tuulit.ui.ScreenColumn
import app.tuulit.ui.TuulitSheet
import app.tuulit.ui.VSpace
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.AppBar
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.EmptyState
import app.tuulit.ui.designsystem.Hairline
import app.tuulit.ui.designsystem.Legend
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.MonthGrid
import app.tuulit.ui.designsystem.MonthHeader
import app.tuulit.ui.designsystem.Notice
import app.tuulit.ui.designsystem.NoticeKind
import app.tuulit.ui.designsystem.ScreenHeader
import app.tuulit.ui.designsystem.Segmented
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import app.tuulit.ui.designsystem.marksFor
import app.tuulit.ui.designsystem.moodFaceLabels
import kotlinx.coroutines.launch
import java.time.YearMonth

@Composable
fun CycleScreen(go: (String) -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val snap by c.cycleState.snapshot.collectAsState()
    val p = snap.prediction
    var seg by rememberSaveable { mutableStateOf(0) }
    var month by remember { mutableStateOf(YearMonth.now(Dates.clock)) }
    var selected by rememberSaveable { mutableLongStateOf(snap.today) }
    val logged = snap.logs.map { it.epochDay }.toSet()

    ScreenColumn {
        CycleSummary(snap)
        Segmented(listOf("Month", "History"), seg, { seg = it }, Modifier.padding(top = 18.dp, bottom = 14.dp))
        if (seg == 0) {
            MonthHeader(month, { month = month.minusMonths(1) }, { month = month.plusMonths(1) })
            MonthGrid(month, { marksFor(it, p, logged, snap.today) }, selected, { selected = it }, onLongPress = { d -> if (d <= snap.today) shell.openLog(d) })
            Legend(Modifier.padding(top = 10.dp))
            T("Predictions are estimates and can't be used as contraception.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 6.dp))
            Hairline(Modifier.padding(top = 12.dp))
            val log = snap.log(selected)
            val parts = buildList {
                if (selected in p.loggedPeriodDays) add("period")
                log?.let { l ->
                    if (l.flow != FlowIntensity.NONE) add(l.flow.label.lowercase() + " flow")
                    if (l.moodScore > 0) add("mood ${moodFaceLabels[l.moodScore - 1].lowercase()}")
                    l.symptoms.entries.take(3).forEach { (k, v) -> add("${v.label.lowercase()} ${k.lowercase()}") }
                    if (l.energyLevel > 0) add("energy ${l.energyLevel}")
                }
                if (selected in p.predictedPeriodDays) add("predicted period")
                if (selected == p.estimatedOvulationDay) add("estimated ovulation") else if (selected in p.estimatedFertileDays) add("fertile window (estimate)")
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).clip(RoundedCornerShape(24.dp)).background(Tuulit.colors.oat).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    T(Dates.day(selected), Tuulit.type.title)
                    if (parts.isEmpty()) T(if (selected > snap.today) "Nothing predicted for this day" else "Nothing logged yet",
                        Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 4.dp))
                    else ChipRow(Modifier.padding(top = 8.dp)) {
                        parts.forEach { part ->
                            T(part, Tuulit.type.caption, Tuulit.colors.ink, Modifier.clip(RoundedCornerShape(999.dp)).background(
                                when {
                                    part.contains("period") || part.contains("flow") -> Tuulit.colors.periodSoft
                                    part.contains("fertile") || part.contains("ovulation") -> Tuulit.colors.fertileSoft
                                    part.contains("mood") -> Tuulit.colors.peachSoft
                                    else -> Tuulit.colors.sand
                                }).padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
                if (selected <= snap.today) app.tuulit.ui.designsystem.IconBtn(TuulitIcons.Edit, "Log this day", { shell.openLog(selected) })
            }
            ListRow("Insights and Mood Map", icon = TuulitIcons.Chart, chevron = true, onClick = { go("insights") })
            ListRow("Pain Journal", icon = TuulitIcons.Pain, trailingText = "${snap.painLogs.size} ${if (snap.painLogs.size == 1) "entry" else "entries"}", chevron = true,
                onClick = { go("pain") }, last = true)
        } else {
            HistoryList(snap.cycles, snap.today, onEdit = { go("history") })
        }
    }
}

@Composable
private fun HistoryList(cycles: List<CycleRecord>, today: Long, onEdit: () -> Unit) {
    if (cycles.isEmpty()) {
        EmptyState("No cycles yet", "Log a period with the Log button and it will appear here.")
        return
    }
    val sorted = cycles.sortedByDescending { it.startEpochDay }
    sorted.forEachIndexed { i, cyc ->
        val next = sorted.getOrNull(i - 1)
        val length = next?.let { (it.startEpochDay - cyc.startEpochDay).toInt() }
        val periodDays = cyc.endEpochDay?.let { (it - cyc.startEpochDay + 1).toInt() }
        ListRow(
            "${Dates.short(cyc.startEpochDay)}${cyc.endEpochDay?.let { " – " + Dates.short(it) } ?: if (i == 0) " – now" else ""}",
            sub = listOfNotNull(length?.let { "$it-day cycle" } ?: if (i == 0) "Current cycle, day ${today - cyc.startEpochDay + 1}" else null,
                periodDays?.let { "period $it days" }).joinToString(" · "),
            last = i == sorted.lastIndex,
        )
    }
    TButton("Edit or delete periods", onEdit, Modifier.padding(top = 8.dp), kind = ButtonKind.TEXT, icon = TuulitIcons.Edit)
}

/** Period history with editing and deletion of each record. */
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val snap by c.cycleState.snapshot.collectAsState()
    var editing by remember { mutableStateOf<CycleRecord?>(null) }
    ScreenColumn {
        AppBar("Period history", onBack)
        T("Tap a period to fix its dates or remove it. Predictions update straight away.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(vertical = 8.dp))
        if (snap.cycles.isEmpty()) EmptyState("No periods logged", "Use the Log button to record your period.")
        snap.cycles.sortedByDescending { it.startEpochDay }.forEachIndexed { i, cyc ->
            ListRow("${Dates.day(cyc.startEpochDay)} → ${cyc.endEpochDay?.let { Dates.day(it) } ?: "ongoing"}",
                icon = TuulitIcons.Drop, chevron = true, last = i == snap.cycles.lastIndex, onClick = { editing = cyc })
        }
    }
    editing?.let { cyc ->
        var start by remember(cyc.id) { mutableLongStateOf(cyc.startEpochDay) }
        var end by remember(cyc.id) { mutableStateOf(cyc.endEpochDay) }
        var error by remember(cyc.id) { mutableStateOf<String?>(null) }
        var confirmDelete by remember(cyc.id) { mutableStateOf(false) }
        TuulitSheet({ editing = null }) {
            T("Edit period", Tuulit.type.headline)
            DayStepper("Started", start, { start = it }, max = snap.today)
            DayStepper("Ended", end ?: start, { end = it }, max = snap.today, enabled = end != null)
            TButton(if (end == null) "Set an end date" else "Still ongoing", { end = if (end == null) start + 4 else null }, kind = ButtonKind.TEXT)
            error?.let { Notice("Can't save", it, Modifier.padding(vertical = 8.dp), kind = NoticeKind.MEDICAL) }
            VSpace(12.dp)
            if (!confirmDelete) Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                TButton("Delete", { confirmDelete = true }, Modifier.weight(1f), kind = ButtonKind.QUIET, icon = TuulitIcons.Trash)
                TButton("Save", {
                    scope.launch {
                        val e = c.tracking.editCycle(cyc.id, start, end)
                        if (e == null) { editing = null; shell.showToast("Period updated") } else error = e
                    }
                }, Modifier.weight(1f))
            } else {
                T("Delete this period? Its daily logs stay; only the period record is removed.", Tuulit.type.bodySm, Tuulit.colors.inkMuted)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    TButton("Keep it", { confirmDelete = false }, Modifier.weight(1f), kind = ButtonKind.QUIET)
                    TButton("Delete period", { scope.launch { c.tracking.deleteCycle(cyc.id); editing = null; shell.showToast("Period deleted") } },
                        Modifier.weight(1f), kind = ButtonKind.DANGER)
                }
            }
        }
    }
}

@Composable
fun DayStepper(label: String, day: Long, onChange: (Long) -> Unit, max: Long, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        T(label, Tuulit.type.subtitle, if (enabled) Tuulit.colors.ink else Tuulit.colors.inkFaint, Modifier.weight(1f))
        app.tuulit.ui.designsystem.IconBtn(TuulitIcons.Minus, "Day earlier", { onChange(day - 1) }, enabled = enabled)
        T(Dates.day(day), Tuulit.type.title, if (enabled) Tuulit.colors.ink else Tuulit.colors.inkFaint, Modifier.padding(horizontal = 12.dp))
        app.tuulit.ui.designsystem.IconBtn(TuulitIcons.Plus, "Day later", { onChange(day + 1) }, enabled = enabled && day < max)
    }
}


/** Phase-tinted summary card at the top of Cycle: phase, day, next period and averages. */
@Composable
private fun CycleSummary(snap: app.tuulit.data.CycleSnapshot) {
    val c = Tuulit.colors
    val p = snap.prediction
    val ph = app.tuulit.ui.designsystem.phaseStyle(if (p.hasRecordedCycles) p.currentPhase else app.tuulit.domain.CyclePhase.UNKNOWN)
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(32.dp)).background(ph.soft)) {
        app.tuulit.ui.designsystem.Blob(ph.fill.copy(alpha = 0.35f), Modifier.size(150.dp).align(Alignment.TopEnd)
            .offset(x = 40.dp, y = (-30).dp), seed = 2)
        Column(Modifier.padding(20.dp)) {
            app.tuulit.ui.designsystem.Overline(if (p.hasRecordedCycles) "Cycle ${snap.cycles.size} · day ${p.currentCycleDay}" else "Your cycle", c.inkMuted)
            T(if (p.hasRecordedCycles) "${ph.label} phase" else "Let's begin", Tuulit.type.display, modifier = Modifier.padding(top = 4.dp))
            T(if (p.hasRecordedCycles) p.currentPhase.shortSummary.substringBefore(". ") + "." else "Log a period and your phases and predictions appear here.",
                Tuulit.type.bodySm, c.inkMuted, Modifier.padding(top = 4.dp))
            if (p.hasRecordedCycles) Row(Modifier.padding(top = 14.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Stat(p.nextPeriodStartEpochDay?.let { Dates.short(it) } ?: "–", "Next period")
                Stat("${p.averageCycleLength} d", "Avg cycle")
                Stat("${p.averagePeriodDuration} d", "Avg period")
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(value: String, label: String) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Tuulit.colors.oat).padding(horizontal = 12.dp, vertical = 10.dp)) {
        T(value, Tuulit.type.title, maxLines = 1)
        T(label, Tuulit.type.caption, Tuulit.colors.inkMuted)
    }
}

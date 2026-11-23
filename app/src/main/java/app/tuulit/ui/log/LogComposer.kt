package app.tuulit.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tuulit.AppContainer
import app.tuulit.domain.Achievement
import app.tuulit.domain.DailyLog
import app.tuulit.domain.Dates
import app.tuulit.domain.FlowIntensity
import app.tuulit.domain.SymptomSeverity
import app.tuulit.domain.TrackingCatalog
import app.tuulit.data.isEmpty
import app.tuulit.ui.LocalShell
import app.tuulit.ui.TuulitSheet
import app.tuulit.ui.VSpace
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Chip
import app.tuulit.ui.designsystem.ChipRow
import app.tuulit.ui.designsystem.Field
import app.tuulit.ui.designsystem.GroupTitle
import app.tuulit.ui.designsystem.IconBtn
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import app.tuulit.ui.today.FaceButton
import app.tuulit.ui.tuulitViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Keeps the composer's working copy and saves every change (text changes are debounced). */
class LogViewModel(private val c: AppContainer) : ViewModel() {
    private val _log = MutableStateFlow<DailyLog?>(null)
    val log: StateFlow<DailyLog?> = _log
    val settings = c.prefs.settings
    private var textSave: Job? = null
    private var firstChange = true

    fun load(day: Long) = viewModelScope.launch {
        firstChange = true
        _log.value = c.tracking.dailyLog(day) ?: DailyLog(epochDay = day)
    }

    /** Returns true for the first change of this session (the composer toasts it once). */
    fun update(change: (DailyLog) -> DailyLog, debounce: Boolean = false): Boolean {
        val cur = _log.value ?: return false
        val next = change(cur)
        _log.value = next
        textSave?.cancel()
        textSave = viewModelScope.launch {
            if (debounce) delay(600)
            c.tracking.saveDailyLog(next)
            c.carePlay.unlock(Achievement.FIRST_LOG)
            if (next.flow.level >= FlowIntensity.LIGHT.level) c.carePlay.unlock(Achievement.FIRST_PERIOD)
        }
        val first = firstChange
        firstChange = false
        return first
    }

    fun endPeriod(day: Long) = viewModelScope.launch { c.tracking.logPeriodEnd(day) }
    fun addMood(m: String) = viewModelScope.launch { c.prefs.addCustomMood(m) }
    fun addSymptom(s: String) = viewModelScope.launch { c.prefs.addCustomSymptom(s) }
    fun deleteDay(day: Long, after: () -> Unit) = viewModelScope.launch {
        c.tracking.deleteDailyLog(day); c.media.deleteLocal(day); after()
    }
}

private val symptomIcons: Map<String, ImageVector> = mapOf(
    "Cramps" to TuulitIcons.Pain, "Headache" to TuulitIcons.Info, "Back pain" to TuulitIcons.Stretch, "Breast tenderness" to TuulitIcons.Care,
    "Bloating" to TuulitIcons.Food, "Acne" to TuulitIcons.Spark, "Fatigue" to TuulitIcons.Moon, "Nausea" to TuulitIcons.Alert,
    "Food cravings" to TuulitIcons.Food, "Sleep problems" to TuulitIcons.Moon, "Mood changes" to TuulitIcons.Breath,
)

/** The log composer sheet, opened by the centre Log button. Every tap saves. */
@Composable
fun LogComposerSheet(startDay: Long, onDismiss: () -> Unit) {
    val vm = tuulitViewModel(key = "log") { LogViewModel(it) }
    val shell = LocalShell.current
    var day by remember { mutableLongStateOf(startDay) }
    LaunchedEffect(day) { vm.load(day) }
    val log by vm.log.collectAsState()
    val settings by vm.settings.collectAsState(initial = null)
    val today = Dates.today()
    var moreOpen by remember { mutableStateOf(false) }
    var notesOpen by remember { mutableStateOf(false) }
    var showAllSymptoms by remember { mutableStateOf(false) }
    var newMood by remember { mutableStateOf("") }
    var newSymptom by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    fun change(debounce: Boolean = false, f: (DailyLog) -> DailyLog) {
        if (vm.update(f, debounce) && !debounce) shell.showToast("Saved")
    }

    TuulitSheet(onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBtn(TuulitIcons.ChevL, "Previous day", { day -= 1 })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                T(if (day == today) "Today" else if (day == today - 1) "Yesterday" else Dates.day(day), Tuulit.type.headline)
                T("${Dates.day(day)} · saves as you tap", Tuulit.type.caption, Tuulit.colors.inkMuted)
            }
            IconBtn(TuulitIcons.ChevR, "Next day", { day += 1 }, enabled = day < today)
        }
        val l = log ?: return@TuulitSheet
        if (l.epochDay != day) return@TuulitSheet

        GroupTitle("Period", Modifier.padding(top = 18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FlowIntensity.entries.forEach { f ->
                FlowStep(f, l.flow == f) { change { it.copy(flow = f) } }
            }
        }
        if (l.flow.level >= FlowIntensity.LIGHT.level) {
            TButton("My period ended today", { vm.endPeriod(day); shell.showToast("Period end saved") }, kind = ButtonKind.TEXT)
        }

        GroupTitle("Mood", Modifier.padding(top = 18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..5).forEach { s ->
                val on = l.moodScore == s
                Column(Modifier.clip(RoundedCornerShape(18.dp)).clickable(role = Role.RadioButton) { change { it.copy(moodScore = if (it.moodScore == s) 0 else s) } }
                    .semantics { contentDescription = app.tuulit.ui.designsystem.moodFaceLabels[s - 1] + if (on) ", selected" else "" }.padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    app.tuulit.ui.designsystem.MoodBlob(s, on, 52.dp)
                    T(app.tuulit.ui.designsystem.moodFaceLabels[s - 1], Tuulit.type.caption, if (on) Tuulit.colors.ink else Tuulit.colors.inkMuted, Modifier.padding(top = 2.dp))
                }
            }
        }
        VSpace(10.dp)
        ChipRow {
            (TrackingCatalog.defaultMoods + (settings?.customMoods ?: emptyList())).distinct().forEach { m ->
                Chip(m, m in l.moods, { change { it.copy(moods = if (m in it.moods) it.moods - m else it.moods + m) } })
            }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Field("", newMood, { newMood = it.take(24) }, Modifier.weight(1f), placeholder = "Add your own mood")
            TButton("Add", { if (newMood.isNotBlank()) { vm.addMood(newMood); change { it.copy(moods = it.moods + newMood.trim()) }; newMood = "" } },
                kind = ButtonKind.TEXT, enabled = newMood.isNotBlank())
        }

        GroupTitle("Symptoms", Modifier.padding(top = 18.dp))
        T("Tap to add. Tap again for moderate, then severe.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(bottom = 8.dp))
        val all = (TrackingCatalog.defaultSymptoms + (settings?.customSymptoms ?: emptyList())).distinct()
        val shown = if (showAllSymptoms) all else all.take(8)
        shown.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { name ->
                    SymptomTile(name, l.symptoms[name], Modifier.weight(1f).fillMaxHeight()) {
                        change {
                            val cur = it.symptoms[name]
                            val next = when (cur) { null -> SymptomSeverity.MILD; SymptomSeverity.MILD -> SymptomSeverity.MODERATE; SymptomSeverity.MODERATE -> SymptomSeverity.SEVERE; SymptomSeverity.SEVERE -> null }
                            it.copy(symptoms = if (next == null) it.symptoms - name else it.symptoms + (name to next))
                        }
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        if (!showAllSymptoms && all.size > 8) TButton("All symptoms", { showAllSymptoms = true }, kind = ButtonKind.TEXT)
        if (showAllSymptoms) Row(verticalAlignment = Alignment.CenterVertically) {
            Field("", newSymptom, { newSymptom = it.take(24) }, Modifier.weight(1f), placeholder = "Add your own symptom")
            TButton("Add", { if (newSymptom.isNotBlank()) { vm.addSymptom(newSymptom); change { it.copy(symptoms = it.symptoms + (newSymptom.trim() to SymptomSeverity.MILD)) }; newSymptom = "" } },
                kind = ButtonKind.TEXT, enabled = newSymptom.isNotBlank())
        }

        ListRow("Energy, sleep, water", trailingText = if (l.energyLevel > 0) "Energy ${l.energyLevel}" else null,
            trailing = { Icon(TuulitIcons.ChevD, null, Modifier.size(18.dp), tint = Tuulit.colors.inkFaint) }, onClick = { moreOpen = !moreOpen })
        if (moreOpen) Column(Modifier.padding(bottom = 12.dp)) {
            Row { T("Energy", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.weight(1f)); T(TrackingCatalog.energyLabels.firstOrNull { it.first == l.energyLevel }?.second ?: "Not set", Tuulit.type.caption, Tuulit.colors.inkMuted) }
            EnergyBars(l.energyLevel) { v -> change { it.copy(energyLevel = if (it.energyLevel == v) 0 else v) } }
            VSpace(14.dp)
            T("Sleep quality", Tuulit.type.bodySm, Tuulit.colors.inkMuted)
            ChipRow(Modifier.padding(top = 6.dp)) {
                listOf("Poor", "Restless", "Okay", "Good", "Great").forEachIndexed { i, s -> Chip(s, l.sleepQuality == i + 1, { change { it.copy(sleepQuality = if (it.sleepQuality == i + 1) 0 else i + 1) } }) }
            }
            VSpace(14.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("Water: ${l.waterGlasses} glasses", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.weight(1f))
                IconBtn(TuulitIcons.Minus, "One glass less", { change { it.copy(waterGlasses = (it.waterGlasses - 1).coerceAtLeast(0)) } })
                Box(Modifier.width(8.dp))
                IconBtn(TuulitIcons.Plus, "One more glass", { change { it.copy(waterGlasses = (it.waterGlasses + 1).coerceAtMost(30)) } })
            }
        }
        ListRow("Medication, notes and voice", trailing = { Icon(TuulitIcons.ChevD, null, Modifier.size(18.dp), tint = Tuulit.colors.inkFaint) },
            onClick = { notesOpen = !notesOpen }, last = !notesOpen)
        if (notesOpen) Column {
            Field("Medication or supplements", l.medicationNote, { v -> change(debounce = true) { it.copy(medicationNote = v.take(200)) } }, placeholder = "e.g. ibuprofen 400 mg")
            VSpace(12.dp)
            Field("Movement", l.exerciseNote, { v -> change(debounce = true) { it.copy(exerciseNote = v.take(200)) } }, placeholder = "e.g. a short walk")
            VSpace(12.dp)
            Field("Private note", l.journalText, { v -> change(debounce = true) { it.copy(journalText = v.take(4000)) } }, placeholder = "Anything you want to remember",
                singleLine = false, minLines = 3)
            VoiceJournal(day, onTranscript = { text -> change { it.copy(journalText = (it.journalText + " " + text).trim()) } },
                hasKey = l.voiceNoteKey != null, onBackedUp = { key -> change { it.copy(voiceNoteKey = key) } })
        }
        TButton("Done", { shell.showToast(if (log.isNullOrEmptyLog()) "Nothing logged for this day" else "Saved for ${Dates.short(day)}"); onDismiss() },
            Modifier.fillMaxWidth().padding(top = 20.dp), kind = ButtonKind.BANANA, icon = TuulitIcons.Check)
        if (!log.isNullOrEmptyLog()) {
            TButton("Clear this day", { confirmDelete = true }, Modifier.padding(top = 12.dp), kind = ButtonKind.TEXT)
        }
        if (confirmDelete) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TButton("Keep it", { confirmDelete = false }, Modifier.weight(1f), kind = ButtonKind.QUIET)
            TButton("Clear day", { vm.deleteDay(day) { confirmDelete = false; vm.load(day) }; shell.showToast("Day cleared") }, Modifier.weight(1f), kind = ButtonKind.DANGER)
        }
    }
}

private fun DailyLog?.isNullOrEmptyLog(): Boolean = this == null || this.isEmpty()

@Composable
private fun FlowStep(f: FlowIntensity, on: Boolean, onClick: () -> Unit) {
    val c = Tuulit.colors
    Column(Modifier.width(60.dp).clickable(role = Role.RadioButton, onClick = onClick).semantics { stateDescription = if (on) "Selected" else "" },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(if (on) (if (f == FlowIntensity.NONE) c.tan else c.period) else if (f == FlowIntensity.NONE) c.sand else c.periodSoft),
            contentAlignment = Alignment.Center) {
            val tint = if (on) c.onPeriod else c.periodDeep
            if (f == FlowIntensity.NONE) Icon(TuulitIcons.Close, null, Modifier.size(18.dp), tint = c.ink)
            else Icon(TuulitIcons.Drop, null, Modifier.size((10 + f.level * 4).dp), tint = tint)
        }
        T(f.label.replace("No flow", "None"), Tuulit.type.caption, if (on) c.ink else c.inkMuted, align = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun SymptomTile(name: String, severity: SymptomSeverity?, modifier: Modifier, onClick: () -> Unit) {
    val c = Tuulit.colors
    val on = severity != null
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(if (on) c.fertileSoft else c.sand)
            .then(if (on) Modifier.border(2.dp, c.fertile, RoundedCornerShape(18.dp)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name + (severity?.let { ", ${it.label.lowercase()}" } ?: ", not logged") }
            .padding(top = 12.dp, bottom = 10.dp, start = 4.dp, end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(symptomIcons[name] ?: TuulitIcons.Spark, null, Modifier.size(22.dp), tint = if (on) c.lilacDeep else c.inkMuted)
        T(name, Tuulit.type.caption, c.ink, align = TextAlign.Center, maxLines = 2)
        if (on) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { i -> Box(Modifier.size(7.dp).clip(CircleShape).background(c.lilacDeep.copy(alpha = if (i < severity!!.level) 1f else 0.25f))) }
        }
    }
}

@Composable
fun EnergyBars(value: Int, onSet: (Int) -> Unit) {
    val c = Tuulit.colors
    Row(Modifier.fillMaxWidth().height(56.dp).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        (1..5).forEach { i ->
            Box(Modifier.weight(1f).height((14 + i * 8).dp).clip(RoundedCornerShape(10.dp)).background(if (i <= value) c.banana else c.sand)
                .clickable(role = Role.Button, onClickLabel = "Energy $i") { onSet(i) })
        }
    }
}

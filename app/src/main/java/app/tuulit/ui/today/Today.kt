package app.tuulit.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tuulit.AppContainer
import app.tuulit.data.CycleSnapshot
import app.tuulit.data.remote.AuthState
import app.tuulit.domain.Achievement
import app.tuulit.domain.DailyLog
import app.tuulit.domain.Dates
import app.tuulit.domain.SuggestionEngine
import app.tuulit.domain.TodayHeroBuilder
import app.tuulit.ui.LocalShell
import app.tuulit.ui.ScreenColumn
import app.tuulit.ui.ShellSheet
import app.tuulit.ui.VSpace
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.Avatar
import app.tuulit.ui.designsystem.BananaField
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.CycleRibbon
import app.tuulit.ui.designsystem.Hairline
import app.tuulit.ui.designsystem.Legend
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotMood
import app.tuulit.ui.designsystem.MoodFace
import app.tuulit.ui.designsystem.Overline
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import app.tuulit.ui.designsystem.marksFor
import app.tuulit.ui.designsystem.mood
import app.tuulit.ui.designsystem.moodFaceLabels
import app.tuulit.ui.tuulitViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TodayViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot: StateFlow<CycleSnapshot> = c.cycleState.snapshot
    val auth = c.auth.state

    fun setMood(day: Long, score: Int) = viewModelScope.launch {
        val existing = c.tracking.dailyLog(day) ?: DailyLog(epochDay = day)
        c.tracking.saveDailyLog(existing.copy(moodScore = if (existing.moodScore == score) 0 else score))
        c.carePlay.unlock(Achievement.FIRST_LOG)
    }

    fun periodStarted(day: Long, onDone: () -> Unit) = viewModelScope.launch {
        c.tracking.logPeriodStart(day); c.carePlay.unlock(Achievement.FIRST_PERIOD); onDone()
    }

    fun undoPeriodStart(day: Long) = viewModelScope.launch {
        c.tracking.dailyLog(day)?.let { c.tracking.saveDailyLog(it.copy(flow = app.tuulit.domain.FlowIntensity.NONE)) }
    }

    fun dismissSuggestion(day: Long) = viewModelScope.launch { c.prefs.dismissSuggestion(day) }
    fun leaveNotTheDay() = viewModelScope.launch { c.prefs.setNotTheDay(null) }
    fun legendSeen() = viewModelScope.launch { c.prefs.setRibbonLegendSeen() }
    fun dismissAccountPrompt() = viewModelScope.launch { c.prefs.dismissAccountPrompt() }
}

@Composable
fun TodayScreen(go: (String) -> Unit) {
    val vm = tuulitViewModel { TodayViewModel(it) }
    val snap by vm.snapshot.collectAsState()
    val auth by vm.auth.collectAsState(initial = AuthState.SignedOut)
    val shell = LocalShell.current
    val s = snap.settings
    if (s.notTheDayOn == snap.today) { NotTheDay(go, onLeave = vm::leaveNotTheDay); return }

    val p = snap.prediction
    val hero = TodayHeroBuilder.build(p, snap.today, Dates::short)
    val signedInName = (auth as? AuthState.SignedIn)?.displayName.orEmpty()
    val name = s.displayName.ifBlank { signedInName }
    val greeting = when (Dates.hourNow()) { in 5..11 -> "Morning"; in 12..17 -> "Afternoon"; in 18..22 -> "Evening"; else -> "Hi" }

    ScreenColumn {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.clickable(role = Role.Button, onClickLabel = "Change Banana Mode") { shell.sheet = ShellSheet.BANANA_MODE }) {
                Mascot(s.bananaMode.mood(), 44.dp)
            }
            Column(Modifier.weight(1f)) {
                Overline(Dates.long(snap.today))
                T(if (name.isBlank()) "$greeting" else "$greeting, $name", Tuulit.type.subtitle)
            }
            Avatar(name.ifBlank { "You" }, onClick = { go("you") })
        }

        Column(Modifier.padding(top = 26.dp)) {
            if (hero.lead.isNotEmpty()) T(hero.lead, if (hero.number == null && !p.hasRecordedCycles) Tuulit.type.display else Tuulit.type.bodySm,
                if (hero.number == null && !p.hasRecordedCycles) Tuulit.colors.ink else Tuulit.colors.inkMuted)
            if (hero.number != null) Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                T(hero.number, Tuulit.type.numeral); T(hero.unit, Tuulit.type.display)
            } else if (p.hasRecordedCycles && hero.unit.isNotEmpty()) T(hero.unit, Tuulit.type.display)
            T(hero.caption, if (p.hasRecordedCycles) Tuulit.type.caption else Tuulit.type.body, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp))
            val showStart = !p.isCurrentlyMenstruating && (!p.hasRecordedCycles || (p.nextPeriodStartEpochDay ?: Long.MAX_VALUE) - snap.today <= 3)
            if (showStart) TButton("My period started", {
                vm.periodStarted(snap.today) { shell.showToast("Period logged") { vm.undoPeriodStart(snap.today) } }
            }, Modifier.padding(top = 18.dp), kind = ButtonKind.BANANA, icon = TuulitIcons.Drop)
            if (p.isCurrentlyMenstruating) TButton("Log today's flow", { shell.openLog(snap.today) }, Modifier.padding(top = 18.dp), kind = ButtonKind.QUIET, icon = TuulitIcons.Drop)
        }

        // Ribbon: the current cycle plus the next few days.
        val start = snap.cycles.lastOrNull { it.startEpochDay <= snap.today }?.startEpochDay ?: (snap.today - 6)
        val end = maxOf(snap.today + 6, (p.nextPeriodStartEpochDay ?: snap.today) + 2)
        val days = (start..end).toList().takeLast(60)
        val logged = snap.logs.map { it.epochDay }.toSet()
        Box(Modifier.padding(top = 10.dp).padding(horizontal = 0.dp)) {
            Box(Modifier.fillMaxWidth().then(Modifier.padding(start = 0.dp))) {
                CycleRibbon(days, { marksFor(it, p, logged, snap.today) }, { d -> Dates.date(d).dayOfMonth.toString() },
                    onDay = { shell.openLog(it.coerceAtMost(snap.today)) }, modifier = Modifier.padding(horizontal = 0.dp).then(Modifier.fillMaxWidth()))
            }
        }
        if (!s.ribbonLegendSeen && p.hasRecordedCycles) {
            Legend(Modifier.padding(top = 4.dp))
            TButton("Got it", vm::legendSeen, kind = ButtonKind.TEXT)
        }

        Column(Modifier.padding(top = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("How are you today?", Tuulit.type.subtitle, modifier = Modifier.weight(1f))
                TButton("Full log", { shell.openLog(snap.today) }, kind = ButtonKind.TEXT)
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                (1..5).forEach { score -> FaceButton(score, snap.todayLog?.moodScore == score) { vm.setMood(snap.today, score) } }
            }
        }

        Hairline(Modifier.padding(top = 20.dp, bottom = 4.dp))
        if (s.suggestionDismissedOn != snap.today) {
            val sug = SuggestionEngine.forToday(p, snap.todayLog, Dates.hourNow())
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button) {
                when {
                    sug.sessionId != null -> go("session/${sug.sessionId}")
                    sug.route == "log" -> shell.openLog(snap.today)
                    sug.route != null -> go(sug.route)
                }
            }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Overline(sug.kicker, Tuulit.colors.toffee)
                    T(sug.title, Tuulit.type.title, modifier = Modifier.padding(top = 2.dp))
                    T(sug.body, Tuulit.type.bodySm, Tuulit.colors.inkMuted)
                }
                Icon(TuulitIcons.ChevR, null, tint = Tuulit.colors.inkFaint)
            }
            TButton("Not today", { vm.dismissSuggestion(snap.today) }, kind = ButtonKind.TEXT)
        }
        BananaField("What would help right now?", "Pain, mood, rest or a distraction", { shell.sheet = ShellSheet.NEED_SOMETHING }, Modifier.padding(top = 6.dp))

        val signedOut = auth is AuthState.SignedOut
        if (signedOut && snap.cycles.isNotEmpty() && !s.accountPromptDismissed) {
            Hairline(Modifier.padding(top = 20.dp))
            app.tuulit.ui.designsystem.ListRow("Back up with an account", sub = "Restore Tuulit on a new phone. Optional.", icon = TuulitIcons.Shield,
                chevron = true, last = true, onClick = { go("auth/register") })
            TButton("No thanks", vm::dismissAccountPrompt, kind = ButtonKind.TEXT)
        }
        TButton("Tuulit SOS", { shell.sheet = ShellSheet.SOS }, Modifier.padding(top = 12.dp), kind = ButtonKind.TEXT, icon = TuulitIcons.Sos)
    }
}

@Composable
fun FaceButton(score: Int, on: Boolean, size: androidx.compose.ui.unit.Dp = 52.dp, onClick: () -> Unit) {
    val c = Tuulit.colors
    Box(
        Modifier.size(size).clip(CircleShape).background(if (on) c.ink else c.sand)
            .clickable(role = Role.RadioButton, onClickLabel = moodFaceLabels[score - 1], onClick = onClick)
            .semantics { contentDescription = moodFaceLabels[score - 1] + if (on) ", selected" else "" },
        contentAlignment = Alignment.Center,
    ) { MoodFace(score, if (on) c.onInk else c.inkMuted) }
}

/** Today Is Not the Day: no tab bar, no numbers, four gentle actions. Ends at midnight. */
@Composable
fun NotTheDay(go: (String) -> Unit, onLeave: () -> Unit) {
    val shell = LocalShell.current
    app.tuulit.ui.FixedScreen {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TButton("Back to normal view", onLeave, kind = ButtonKind.TEXT) }
        Mascot(MascotMood.SLEEPY, 128.dp, Modifier.padding(top = 28.dp))
        T("Take it slow today.", Tuulit.type.display, modifier = Modifier.padding(top = 16.dp))
        T("Everything else can wait. Here's all you need.", Tuulit.type.body, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp))
        Box(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple("Log how I feel", TuulitIcons.Edit) { shell.openLog(Dates.today()) },
                Triple("Help me relax", TuulitIcons.Breath) { go("session/slow_breathing") },
                Triple("Distract me", TuulitIcons.Play) { go("care/play") },
                Triple("Tell my person", TuulitIcons.Sos) { shell.sheet = ShellSheet.SOS },
            ).forEach { (label, icon, action) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                    .background(Tuulit.colors.sand).clickable(role = Role.Button, onClick = action).padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, null, tint = Tuulit.colors.ink); T(label, Tuulit.type.label)
                }
            }
        }
        VSpace(8.dp)
    }
}

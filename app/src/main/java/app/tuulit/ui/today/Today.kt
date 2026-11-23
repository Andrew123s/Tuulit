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
import app.tuulit.ui.designsystem.Blob
import app.tuulit.ui.designsystem.CycleRing
import app.tuulit.ui.designsystem.Illo
import app.tuulit.ui.designsystem.Illustration
import app.tuulit.ui.designsystem.MoodBlob
import app.tuulit.ui.designsystem.phaseStyle
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val c = Tuulit.colors
    val phase = phaseStyle(if (p.hasRecordedCycles) p.currentPhase else app.tuulit.domain.CyclePhase.UNKNOWN)
    val timeGreeting = when (Dates.hourNow()) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; in 18..22 -> "Good evening"; else -> "Hello" }
    val subline = when {
        p.isCurrentlyMenstruating -> "Take it slow today. You're doing great."
        snap.todayLog == null -> "A little check-in with yourself?"
        p.currentPhase == app.tuulit.domain.CyclePhase.FOLLICULAR -> "Energy often lifts around now."
        p.currentPhase == app.tuulit.domain.CyclePhase.LUTEAL -> "Be extra gentle with yourself this week."
        else -> "Thanks for checking in today."
    }

    ScreenColumn(bottomPad = 24.dp) {
        // Greeting
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                T(if (name.isBlank()) "$timeGreeting, sunshine" else "$timeGreeting, $name", Tuulit.type.headline)
                T(subline, Tuulit.type.bodySm, c.inkMuted, Modifier.padding(top = 2.dp))
            }
            Avatar(name.ifBlank { "You" }, 44.dp, onClick = { go("you") })
        }

        // Hero: phase-tinted card with the cycle ring
        Box(Modifier.fillMaxWidth().padding(top = 18.dp).clip(RoundedCornerShape(32.dp)).background(phase.soft)) {
            Blob(phase.fill.copy(alpha = 0.35f), Modifier.size(170.dp).align(Alignment.TopEnd).offset(x = 50.dp, y = (-40).dp), seed = 1)
            Blob(c.oat.copy(alpha = 0.55f), Modifier.size(120.dp).align(Alignment.BottomStart).offset(x = (-40).dp, y = 40.dp), seed = 3)
            Box(Modifier.align(Alignment.TopStart).padding(14.dp).clickable(role = Role.Button, onClickLabel = "Change Banana Mode") { shell.sheet = ShellSheet.BANANA_MODE }) {
                Mascot(s.bananaMode.mood(), 52.dp)
            }
            Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 22.dp, start = 20.dp, end = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (p.hasRecordedCycles) {
                    val cycleStart = snap.cycles.lastOrNull { it.startEpochDay <= snap.today }?.startEpochDay
                    CycleRing(p, cycleStart, snap.today, 236.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            T(hero.lead.removePrefix("Your ").replaceFirstChar { it.uppercase() }, Tuulit.type.caption, c.inkMuted)
                            if (hero.number != null) {
                                T(hero.number, Tuulit.type.numeral.copy(fontSize = Tuulit.type.numeral.fontSize * 1.15f))
                                T(hero.unit.trim(), Tuulit.type.subtitle, c.inkMuted)
                            } else T(hero.unit, Tuulit.type.display)
                        }
                    }
                    Row(Modifier.padding(top = 14.dp).clip(RoundedCornerShape(999.dp)).background(c.oat).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(phase.fill))
                        T("  ${phase.label} phase · day ${p.currentCycleDay}", Tuulit.type.label)
                    }
                    val next = p.nextPeriodStartEpochDay
                    if (next != null && !p.isCurrentlyMenstruating) T(
                        if (p.isIrregularEstimate) "Next period in ${p.nextPeriodRangeMinDays}–${p.nextPeriodRangeMaxDays} days · estimate"
                        else "Next period around ${Dates.short(next)} · estimate", Tuulit.type.caption, c.inkMuted, Modifier.padding(top = 8.dp))
                } else {
                    Mascot(app.tuulit.ui.designsystem.MascotMood.HAPPY, 120.dp, Modifier.padding(top = 18.dp))
                    T("Let's get to know your cycle.", Tuulit.type.display, align = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    T("Log your next period and your cycle ring fills in.", Tuulit.type.bodySm, c.inkMuted, Modifier.padding(top = 6.dp),
                        align = androidx.compose.ui.text.style.TextAlign.Center)
                }
                val showStart = !p.isCurrentlyMenstruating && (!p.hasRecordedCycles || (p.nextPeriodStartEpochDay ?: Long.MAX_VALUE) - snap.today <= 3)
                if (showStart) TButton("My period started", {
                    vm.periodStarted(snap.today) { shell.showToast("Period logged") { vm.undoPeriodStart(snap.today) } }
                }, Modifier.padding(top = 16.dp), kind = ButtonKind.BANANA, icon = TuulitIcons.Drop)
                if (p.isCurrentlyMenstruating) TButton("Log today's flow", { shell.openLog(snap.today) }, Modifier.padding(top = 16.dp), icon = TuulitIcons.Drop)
            }
        }

        // Quick actions
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickAction("Log period", TuulitIcons.Drop, c.periodSoft, c.periodDeep) { shell.openLog(snap.today) }
            QuickAction("Symptoms", TuulitIcons.Spark, c.fertileSoft, c.lilacDeep) { shell.openLog(snap.today) }
            QuickAction("Feel better", TuulitIcons.Care, c.roseSoft, c.periodDeep) { shell.sheet = ShellSheet.NEED_SOMETHING }
            QuickAction("Calendar", TuulitIcons.Cycle, c.bananaSoft, c.toffee) { go("cycle") }
        }

        // Mood check-in
        Column(Modifier.padding(top = 24.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.oat).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("How are you feeling?", Tuulit.type.title, modifier = Modifier.weight(1f))
                TButton("Full log", { shell.openLog(snap.today) }, kind = ButtonKind.TEXT)
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                (1..5).forEach { score ->
                    val on = snap.todayLog?.moodScore == score
                    Column(
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .clickable(role = Role.RadioButton, onClickLabel = moodFaceLabels[score - 1]) { vm.setMood(snap.today, score) }
                            .semantics { contentDescription = moodFaceLabels[score - 1] + if (on) ", selected" else "" }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MoodBlob(score, on, 54.dp)
                        T(moodFaceLabels[score - 1], Tuulit.type.caption, if (on) c.ink else c.inkMuted, Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        // Recommendation
        if (s.suggestionDismissedOn != snap.today) {
            val sug = SuggestionEngine.forToday(p, snap.todayLog, Dates.hourNow())
            val illo = when {
                sug.sessionId?.contains("sleep") == true -> Illo.MOON
                sug.sessionId?.contains("heat") == true -> Illo.HEAT
                sug.sessionId?.contains("stretch") == true -> Illo.STRETCH
                sug.sessionId?.contains("breath") == true -> Illo.BREATH
                sug.route?.startsWith("care/play") == true -> Illo.SPARK
                else -> Illo.SUN
            }
            Row(Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.bananaSoft).clickable(role = Role.Button) {
                when {
                    sug.sessionId != null -> go("session/${sug.sessionId}")
                    sug.route == "log" -> shell.openLog(snap.today)
                    sug.route != null -> go(sug.route)
                }
            }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Overline(sug.kicker, c.toffee)
                    T(sug.title, Tuulit.type.title, modifier = Modifier.padding(top = 4.dp))
                    T(sug.body, Tuulit.type.bodySm, c.inkMuted, Modifier.padding(top = 2.dp))
                    T("Not today", Tuulit.type.label, c.toffee, Modifier.padding(top = 10.dp).clickable(role = Role.Button) { vm.dismissSuggestion(snap.today) })
                }
                Illustration(illo, 84.dp, bg = c.oat)
            }
        }

        // I need something
        Row(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.roseSoft)
            .clickable(role = Role.Button) { shell.sheet = ShellSheet.NEED_SOMETHING }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Illustration(Illo.HEART, 56.dp, bg = c.oat)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                T("What would help right now?", Tuulit.type.subtitle)
                T("Pain, mood, rest or a distraction", Tuulit.type.bodySm, c.inkMuted)
            }
            Icon(TuulitIcons.ChevR, null, tint = c.ink)
        }

        val signedOut = auth is AuthState.SignedOut
        if (signedOut && snap.cycles.isNotEmpty() && !s.accountPromptDismissed) {
            app.tuulit.ui.designsystem.ListRow("Back up with an account", sub = "Restore Tuulit on a new phone. Optional.", icon = TuulitIcons.Shield,
                chevron = true, last = true, onClick = { go("auth/register") }, modifier = Modifier.padding(top = 12.dp))
            TButton("No thanks", vm::dismissAccountPrompt, kind = ButtonKind.TEXT)
        }
        TButton("Tuulit SOS", { shell.sheet = ShellSheet.SOS }, Modifier.padding(top = 8.dp), kind = ButtonKind.TEXT, icon = TuulitIcons.Sos)
    }
}

@Composable
private fun QuickAction(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit,
) {
    Column(Modifier.width(78.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(26.dp), tint = tint)
        }
        T(label, Tuulit.type.caption, Tuulit.colors.ink, Modifier.padding(top = 6.dp), maxLines = 1)
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

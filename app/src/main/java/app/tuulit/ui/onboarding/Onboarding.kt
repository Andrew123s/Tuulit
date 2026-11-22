package app.tuulit.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tuulit.domain.CycleRegularity
import app.tuulit.domain.Dates
import app.tuulit.ui.FixedScreen
import app.tuulit.ui.VSpace
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.AppBar
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Chip
import app.tuulit.ui.designsystem.ChipRow
import app.tuulit.ui.designsystem.Hairline
import app.tuulit.ui.designsystem.IconBtn
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotMood
import app.tuulit.ui.designsystem.MonthGrid
import app.tuulit.ui.designsystem.MonthHeader
import app.tuulit.ui.designsystem.Notice
import app.tuulit.ui.designsystem.Progress
import app.tuulit.ui.designsystem.SettingRow
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import app.tuulit.ui.designsystem.DayMarks
import app.tuulit.ui.you.PinSetupSheet
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * Three-step onboarding from the approved design: welcome, last period, cycle basics, then
 * privacy and reminders. Signing in is optional and offered on the welcome screen.
 */
@Composable
fun OnboardingFlow(onSignIn: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var lastStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var cycleLen by rememberSaveable { mutableIntStateOf(28) }
    var periodLen by rememberSaveable { mutableIntStateOf(5) }
    var regularity by rememberSaveable { mutableStateOf(CycleRegularity.REGULAR) }
    when (step) {
        0 -> Welcome(onStart = { step = 1 }, onSignIn = onSignIn)
        1 -> LastPeriod(lastStart, onPick = { lastStart = it }, onBack = { step = 0 }, onNext = { step = 2 }, onSkip = { lastStart = null; step = 2 })
        2 -> CycleBasics(cycleLen, periodLen, regularity, { cycleLen = it }, { periodLen = it }, { regularity = it },
            onBack = { step = 1 }, onNext = { step = 3 })
        else -> PrivacyStep(onBack = { step = 2 }, lastStart = lastStart, cycleLen = cycleLen, periodLen = periodLen, regularity = regularity)
    }
}

@Composable
private fun Welcome(onStart: () -> Unit, onSignIn: () -> Unit) = FixedScreen {
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Mascot(MascotMood.WARM, 200.dp) }
    T("Your cycle, understood. Your hard days, handled.", Tuulit.type.display)
    T("Tracking, comfort and honest answers, kept private on your phone.", Tuulit.type.body, Tuulit.colors.inkMuted, Modifier.padding(top = 12.dp, bottom = 28.dp))
    TButton("Get started", onStart, Modifier.fillMaxWidth())
    TButton("I already have an account", onSignIn, Modifier.fillMaxWidth().padding(top = 8.dp), kind = ButtonKind.TEXT)
}

@Composable
private fun StepBar(done: Int, onBack: () -> Unit, onSkip: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBtn(TuulitIcons.Back, "Back", onBack)
        Progress(3, done, Modifier.weight(1f).padding(horizontal = 12.dp))
        if (onSkip != null) TButton("Skip", onSkip, kind = ButtonKind.TEXT) else Spacer(Modifier.width(44.dp))
    }
}

@Composable
private fun LastPeriod(selected: Long?, onPick: (Long) -> Unit, onBack: () -> Unit, onNext: () -> Unit, onSkip: () -> Unit) = FixedScreen {
    val today = Dates.today()
    var month by remember { mutableStateOf(YearMonth.now(Dates.clock)) }
    StepBar(1, onBack, onSkip)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        T("When did your last period start?", Tuulit.type.display, modifier = Modifier.padding(top = 20.dp))
        T("A rough guess is fine. You can change it later.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 18.dp))
        MonthHeader(month, { month = month.minusMonths(1) }, { month = month.plusMonths(1) }, nextEnabled = month < YearMonth.now(Dates.clock))
        MonthGrid(month, marks = { d -> DayMarks(period = d == selected, today = d == today && d != selected) }, selected = null,
            onSelect = onPick, disableAfter = today)
    }
    ChipRow(Modifier.padding(vertical = 16.dp)) {
        Chip("I'm not sure", false, onSkip)
        Chip("I don't have periods right now", false, onSkip)
    }
    TButton("Continue", onNext, Modifier.fillMaxWidth(), enabled = selected != null)
}

@Composable
private fun Stepper(label: String, value: Int, unit: String, range: IntRange, onChange: (Int) -> Unit, big: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (!big) T(label, Tuulit.type.subtitle, modifier = Modifier.weight(1f))
        IconBtn(TuulitIcons.Minus, "Fewer days", { onChange((value - 1).coerceIn(range)) }, enabled = value > range.first)
        Row(if (big) Modifier.weight(1f) else Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            T(value.toString(), if (big) Tuulit.type.numeral else Tuulit.type.title)
            T(" $unit", if (big) Tuulit.type.headline else Tuulit.type.title, if (big) Tuulit.colors.inkMuted else Tuulit.colors.ink)
        }
        IconBtn(TuulitIcons.Plus, "More days", { onChange((value + 1).coerceIn(range)) }, enabled = value < range.last)
    }
}

@Composable
private fun CycleBasics(
    cycleLen: Int, periodLen: Int, regularity: CycleRegularity,
    onCycle: (Int) -> Unit, onPeriod: (Int) -> Unit, onRegularity: (CycleRegularity) -> Unit,
    onBack: () -> Unit, onNext: () -> Unit,
) = FixedScreen {
    StepBar(2, onBack, onSkip = onNext)
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        T("How long are your cycles, usually?", Tuulit.type.display, modifier = Modifier.padding(top = 20.dp, bottom = 28.dp))
        Stepper("Cycle length", cycleLen, "days", 15..60, onCycle, big = true)
        Hairline(Modifier.padding(vertical = 20.dp))
        Stepper("Period length", periodLen, "days", 1..14, onPeriod, big = false)
        Hairline(Modifier.padding(vertical = 20.dp))
        T("How regular are they?", Tuulit.type.subtitle, modifier = Modifier.padding(bottom = 10.dp))
        ChipRow {
            listOf(CycleRegularity.REGULAR to "Mostly regular", CycleRegularity.SOMEWHAT_VARIABLE to "They vary",
                CycleRegularity.IRREGULAR to "Unpredictable", CycleRegularity.NOT_SURE to "Not sure").forEach { (r, l) ->
                Chip(l, regularity == r, { onRegularity(r) })
            }
        }
    }
    Notice("Not sure?", "Keep the defaults. Tuulit learns from what you log.", Modifier.padding(vertical = 16.dp))
    TButton("Continue", onNext, Modifier.fillMaxWidth())
}

@Composable
private fun PrivacyStep(onBack: () -> Unit, lastStart: Long?, cycleLen: Int, periodLen: Int, regularity: CycleRegularity) {
    val c = container()
    val scope = rememberCoroutineScope()
    var reminders by rememberSaveable { mutableStateOf(true) }
    var discreet by rememberSaveable { mutableStateOf(true) }
    var aiContext by rememberSaveable { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var pinSet by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (!granted) reminders = false }
    FixedScreen {
        StepBar(3, onBack, null)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            T("Your data stays on this phone.", Tuulit.type.display, modifier = Modifier.padding(top = 20.dp))
            T("Nothing leaves it unless you choose to share or back up. You can change all of this in You → Privacy.",
                Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 12.dp))
            SettingRow("Gentle reminders", "Before your period. Never guilt.", reminders, { on ->
                reminders = on
                if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
            SettingRow("Discreet notifications", "No cycle words on the lock screen", discreet, { discreet = it })
            SettingRow("Personal answers in Ask", "Lets Ask use a summary of your cycle data", aiContext, { aiContext = it })
            ListRow("App lock", sub = if (pinSet) "PIN set" else "Set a PIN now or later", icon = TuulitIcons.Lock, chevron = true, last = true,
                onClick = { showPin = true })
        }
        TButton("Start using Tuulit", {
            saving = true
            // App scope: finishing onboarding replaces this screen, which would cancel a screen-scoped coroutine.
            c.appScope.launch {
                val today = Dates.today()
                lastStart?.let { c.tracking.addPastPeriod(it, periodLen, today) }
                c.unlocked.value = true
                c.prefs.completeOnboarding(cycleLen, periodLen, regularity, reminders, discreet, aiContext)
            }
        }, Modifier.fillMaxWidth(), kind = ButtonKind.BANANA, loading = saving)
    }
    if (showPin) PinSetupSheet(onDone = { pinSet = it; showPin = false })
}

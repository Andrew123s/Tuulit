package app.tuulit.ui.care

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tuulit.domain.CareContent
import app.tuulit.domain.PlayContent
import app.tuulit.ui.FixedScreen
import app.tuulit.ui.LocalShell
import app.tuulit.ui.ScreenColumn
import app.tuulit.ui.TuulitSheet
import app.tuulit.ui.VSpace
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Field
import app.tuulit.ui.designsystem.GroupTitle
import app.tuulit.ui.designsystem.IconBtn
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.Notice
import app.tuulit.ui.designsystem.Overline
import app.tuulit.ui.designsystem.Panel
import app.tuulit.ui.designsystem.Progress
import app.tuulit.ui.designsystem.ScreenHeader
import app.tuulit.ui.designsystem.Segmented
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tile
import app.tuulit.ui.designsystem.TileGrid
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun CareScreen(initialSegment: Int, go: (String) -> Unit) {
    var seg by rememberSaveable { mutableIntStateOf(initialSegment) }
    val c = container()
    val snap by c.cycleState.snapshot.collectAsState()
    val headline = when (seg) { 0 -> CareContent.heroHeadline(snap.todayLog); 1 -> "Two minutes of not thinking about it."; else -> "What works for you." }
    ScreenColumn {
        ScreenHeader(when { seg == 0 && snap.todayLog?.symptoms?.containsKey("Cramps") == true -> "You logged cramps today"; else -> "Care" }, headline)
        Segmented(listOf("Comfort", "Play", "Library"), seg, { seg = it }, Modifier.padding(vertical = 16.dp))
        when (seg) {
            0 -> Comfort(go)
            1 -> PlayHub(go)
            else -> Library(go)
        }
    }
}

@Composable
private fun Comfort(go: (String) -> Unit) {
    val c = container()
    val snap by c.cycleState.snapshot.collectAsState()
    val hero = CareContent.heroFor(snap.todayLog, snap.prediction.currentPhase)
    Panel(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Overline("Start here · ${hero.minutesLabel}", Tuulit.colors.toffee, Modifier.weight(1f))
            Icon(TuulitIcons.named(hero.icon), null, tint = Tuulit.colors.toffee)
        }
        T(hero.title, Tuulit.type.title, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
        hero.steps.take(3).forEachIndexed { i, s -> T("${i + 1}. ${s.title}", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(bottom = 4.dp)) }
        TButton("Start", { go("session/${hero.id}") }, Modifier.fillMaxWidth().padding(top = 12.dp))
    }
    GroupTitle("Also good right now", Modifier.padding(top = 20.dp, bottom = 6.dp))
    TileGrid(CareContent.all.filter { it.id != hero.id }.map { s ->
        { m: Modifier -> Tile(s.title, "${s.category} · ${s.minutesLabel}", TuulitIcons.named(s.icon), { go("session/${s.id}") }, m) }
    })
    GroupTitle("When to see a doctor", Modifier.padding(top = 20.dp))
    CareContent.redFlags.forEachIndexed { i, f -> ListRow(f, icon = TuulitIcons.Alert, last = i == CareContent.redFlags.lastIndex) }
    T("If any of these happen, contact a clinician or emergency services.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 8.dp))
}

@Composable
private fun PlayHub(go: (String) -> Unit) {
    val c = container()
    val scores by c.carePlay.gameScores.collectAsState(initial = emptyMap())
    TileGrid(PlayContent.games.map { g ->
        { m: Modifier ->
            val best = scores[g.id]?.best
            Tile(g.title, if (best != null && best > 0) "${g.kind} · best $best" else g.kind, TuulitIcons.named(g.icon), { go("game/${g.id}") }, m)
        }
    })
    GroupTitle("Quizzes", Modifier.padding(top = 20.dp))
    PlayContent.quizzes.forEachIndexed { i, q ->
        ListRow(q.title, sub = q.subtitle, icon = TuulitIcons.named(q.icon), chevron = true, last = i == PlayContent.quizzes.lastIndex, onClick = { go("quiz/${q.id}") })
    }
    val achievements by c.carePlay.achievements.collectAsState(initial = emptyList())
    if (achievements.isNotEmpty()) {
        GroupTitle("Moments", Modifier.padding(top = 20.dp))
        achievements.forEachIndexed { i, a -> ListRow(a.achievement.title, sub = a.achievement.description, icon = TuulitIcons.Spark, last = i == achievements.lastIndex) }
    }
    T("Scores are personal bests. There are no streaks to keep.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 12.dp))
}

@Composable
private fun Library(go: (String) -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val items by c.carePlay.comfortItems.collectAsState(initial = emptyList())
    val settings by c.prefs.settings.collectAsState(initial = null)
    var adding by remember { mutableStateOf<String?>(null) }
    val comfort = items.filter { it.kind == "comfort" }
    val kit = items.filter { it.kind == "kit" }
    GroupTitle("My comfort list")
    if (comfort.isEmpty()) T("Finish a Care session or add what helps you, and it appears here.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(vertical = 8.dp))
    comfort.forEach { i ->
        ListRow(i.title, sub = listOfNotNull(i.detail.ifBlank { null }, if (i.helpedCount > 0) "Helped ${i.helpedCount} ${if (i.helpedCount == 1) "time" else "times"}" else null).joinToString(" · ").ifEmpty { null },
            icon = TuulitIcons.named(i.icon), onClick = i.sessionId?.let { sid -> { go("session/$sid") } },
            trailing = { IconBtn(TuulitIcons.Trash, "Remove ${i.title}", { scope.launch { c.carePlay.deleteComfortItem(i.id); shell.showToast("Removed") } }) })
    }
    TButton("Add something that helps", { adding = "comfort" }, kind = ButtonKind.TEXT, icon = TuulitIcons.Plus)
    GroupTitle("Period survival kit", Modifier.padding(top = 18.dp))
    kit.forEach { i ->
        ListRow(i.title, leading = { Icon(if (i.checked) TuulitIcons.Check else TuulitIcons.Kit, null, tint = if (i.checked) Tuulit.colors.ink else Tuulit.colors.inkFaint) },
            state = if (i.checked) "Packed" else "Not packed", role = androidx.compose.ui.semantics.Role.Checkbox,
            onClick = { scope.launch { c.carePlay.toggleKitItem(i.id) } },
            trailing = { IconBtn(TuulitIcons.Trash, "Remove ${i.title}", { scope.launch { c.carePlay.deleteComfortItem(i.id) } }) })
    }
    TButton("Add an item", { adding = "kit" }, kind = ButtonKind.TEXT, icon = TuulitIcons.Plus)
    GroupTitle("Sleep routine", Modifier.padding(top = 18.dp))
    var note by remember(settings?.sleepNote) { mutableStateOf(settings?.sleepNote.orEmpty()) }
    Field("", note, { note = it.take(500) }, singleLine = false, minLines = 2, placeholder = "What helps you wind down?")
    if (note != settings?.sleepNote.orEmpty()) TButton("Save routine", { scope.launch { c.prefs.setSleepNote(note); shell.showToast("Routine saved") } }, kind = ButtonKind.TEXT)

    adding?.let { kind ->
        var title by remember { mutableStateOf("") }
        var detail by remember { mutableStateOf("") }
        TuulitSheet({ adding = null }) {
            T(if (kind == "kit") "Add to your kit" else "Add something that helps", Tuulit.type.headline)
            Field("Name", title, { title = it.take(80) }, Modifier.padding(top = 16.dp), placeholder = if (kind == "kit") "e.g. spare tampons" else "e.g. hot water bottle and ginger tea")
            if (kind == "comfort") Field("When it helps (optional)", detail, { detail = it.take(160) }, Modifier.padding(top = 12.dp), placeholder = "e.g. day 1–2 cramps")
            TButton("Add", {
                scope.launch { c.carePlay.addComfortItem(kind, title, detail, if (kind == "kit") "kit" else "care"); adding = null; shell.showToast("Added") }
            }, Modifier.fillMaxWidth().padding(top = 16.dp), enabled = title.isNotBlank())
        }
    }
}

/** Full-screen guided session with a breathing circle, step timer, pause and end. */
@Composable
fun SessionScreen(id: String, onBack: () -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val session = CareContent.byId(id) ?: run { onBack(); return }
    val reduced = Tuulit.reducedMotion
    var stepIndex by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(session.steps.first().seconds) }
    var paused by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var phaseLabel by remember { mutableStateOf("Breathe in") }
    val circle = remember { Animatable(0.72f) }

    LaunchedEffect(paused, done, stepIndex) {
        while (isActive && !paused && !done) {
            delay(1_000)
            if (remaining > 1) remaining-- else if (stepIndex < session.steps.lastIndex) { stepIndex++; remaining = session.steps[stepIndex].seconds } else done = true
        }
    }
    val pattern = session.breathing
    LaunchedEffect(paused, done) {
        if (pattern == null || paused || done) return@LaunchedEffect
        while (isActive) {
            phaseLabel = "Breathe in"
            if (reduced) delay(pattern.inhale * 1000L) else circle.animateTo(1f, tween(pattern.inhale * 1000, easing = LinearEasing))
            if (pattern.hold > 0) { phaseLabel = "Hold"; delay(pattern.hold * 1000L) }
            phaseLabel = "Breathe out"
            if (reduced) delay(pattern.exhale * 1000L) else circle.animateTo(0.72f, tween(pattern.exhale * 1000, easing = LinearEasing))
        }
    }
    LaunchedEffect(done) {
        if (done) {
            val unlocked = c.carePlay.completeSession(session.id, fully = true)
            shell.showToast(if (unlocked.isNotEmpty()) "${unlocked.first().title} · added to your library" else "Nice. Saved to your library")
        }
    }

    FixedScreen {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(TuulitIcons.Close, "Close", {
                if (!done) scope.launch { c.carePlay.completeSession(session.id, fully = false) }
                onBack()
            })
            val elapsed = session.steps.take(stepIndex).sumOf { it.seconds } + (session.steps[stepIndex].seconds - remaining)
            T(if (done) "Done" else "${elapsed / 60}:${"%02d".format(elapsed % 60)} of ${session.minutesLabel}", Tuulit.type.caption, Tuulit.colors.inkMuted,
                Modifier.weight(1f), align = TextAlign.Center)
            Box(Modifier.size(44.dp))
        }
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (done) {
                app.tuulit.ui.designsystem.Mascot(app.tuulit.ui.designsystem.MascotMood.HAPPY, 140.dp)
                T("Well done.", Tuulit.type.display, modifier = Modifier.padding(top = 16.dp))
                T("${session.title} is now in your Comfort Library.", Tuulit.type.body, Tuulit.colors.inkMuted, align = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            } else {
                if (pattern != null) {
                    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(240.dp).scale(if (reduced) 1f else circle.value).clip(CircleShape).background(Tuulit.colors.bananaSoft))
                        Box(Modifier.size(160.dp).scale(if (reduced) 1f else circle.value).clip(CircleShape).background(Tuulit.colors.banana))
                        T(phaseLabel, Tuulit.type.headline, Tuulit.colors.onBanana, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    VSpace(28.dp)
                }
                val step = session.steps[stepIndex]
                T(step.title, Tuulit.type.title, align = TextAlign.Center)
                T(step.detail, Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp), align = TextAlign.Center)
                if (pattern != null) T(pattern.label, Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 10.dp))
            }
        }
        if (!done) Progress(session.steps.size, stepIndex + 1, Modifier.padding(bottom = 20.dp))
        if (done) {
            TButton("Back to Care", onBack, Modifier.fillMaxWidth())
        } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TButton(if (paused) "Resume" else "Pause", { paused = !paused }, Modifier.weight(1f), kind = ButtonKind.QUIET)
            TButton(if (stepIndex < session.steps.lastIndex) "Next step" else "Finish", {
                if (stepIndex < session.steps.lastIndex) { stepIndex++; remaining = session.steps[stepIndex].seconds } else done = true
            }, Modifier.weight(1f), kind = ButtonKind.QUIET)
        }
    }
}

package app.tuulit.ui.care

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import app.tuulit.domain.Dates
import app.tuulit.ui.designsystem.Blob
import app.tuulit.ui.designsystem.Chip
import app.tuulit.ui.designsystem.GameArt
import app.tuulit.ui.designsystem.Illo
import app.tuulit.ui.designsystem.Illustration
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotMood
import app.tuulit.ui.designsystem.gameLook
import app.tuulit.ui.designsystem.illoFor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
    val col = Tuulit.colors
    val crampy = snap.todayLog?.symptoms?.containsKey("Cramps") == true
    val (kicker, headline, bg, mood) = when (seg) {
        0 -> CareHead(if (crampy) "You logged cramps today" else "Tuulit Care", CareContent.heroHeadline(snap.todayLog), col.peachSoft,
            if (crampy) MascotMood.CRAMPS else MascotMood.CHILL)
        1 -> CareHead("Tuulit Play", "Two minutes of not thinking about it.", col.bananaSoft, MascotMood.HAPPY)
        else -> CareHead("Your library", "What works for you.", col.roseSoft, MascotMood.WARM)
    }
    ScreenColumn {
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(32.dp)).background(bg)) {
            Blob(col.oat.copy(alpha = 0.6f), Modifier.size(150.dp).align(Alignment.CenterEnd).offset(x = 30.dp), seed = 4)
            Mascot(mood, 92.dp, Modifier.align(Alignment.CenterEnd).padding(end = 18.dp))
            Column(Modifier.padding(start = 22.dp, top = 22.dp, bottom = 22.dp, end = 120.dp)) {
                Overline(kicker, col.toffee)
                T(headline, Tuulit.type.headline, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Segmented(listOf("Comfort", "Play", "Library"), seg, { seg = it }, Modifier.padding(vertical = 16.dp))
        when (seg) {
            0 -> Comfort(go) { seg = 2 }
            1 -> PlayHub(go)
            else -> Library(go)
        }
    }
}

private data class CareHead(val kicker: String, val headline: String, val bg: androidx.compose.ui.graphics.Color, val mood: MascotMood)

private enum class CareCat(val label: String) { ALL("All"), CRAMPS("Cramps"), RELAX("Relaxation"), SLEEP("Sleep"), MOVE("Movement"), MOOD("Mood"), BREATH("Breathing") }

private fun matches(cat: CareCat, s: app.tuulit.domain.CareSession) = when (cat) {
    CareCat.ALL -> true
    CareCat.CRAMPS -> s.category == "Cramps"
    CareCat.RELAX -> s.category == "Calm" || s.category == "Sleep"
    CareCat.SLEEP -> s.category == "Sleep"
    CareCat.MOVE -> s.id == "gentle_stretches"
    CareCat.MOOD -> s.category == "Mood"
    CareCat.BREATH -> s.breathing != null
}

@Composable
private fun sessionColor(s: app.tuulit.domain.CareSession): androidx.compose.ui.graphics.Color {
    val c = Tuulit.colors
    return when (s.category) { "Cramps" -> c.periodSoft; "Calm" -> c.fertileSoft; "Sleep" -> c.fertileSoft; "Mood" -> c.roseSoft; else -> c.peachSoft }
}

@Composable
private fun Comfort(go: (String) -> Unit, openLibrary: () -> Unit) {
    val c = container()
    val col = Tuulit.colors
    val snap by c.cycleState.snapshot.collectAsState()
    val items by c.carePlay.comfortItems.collectAsState(initial = emptyList())
    val hero = CareContent.heroFor(snap.todayLog, snap.prediction.currentPhase)
    var cat by rememberSaveable { mutableStateOf(CareCat.ALL) }
    var flagsOpen by remember { mutableStateOf(false) }

    // Featured experience
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(sessionColor(hero)).clickable(role = androidx.compose.ui.semantics.Role.Button) { go("session/${hero.id}") }.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline("Made for today · ${hero.minutesLabel}", col.toffee)
                T(hero.title, Tuulit.type.display, modifier = Modifier.padding(top = 4.dp))
                T(hero.summary, Tuulit.type.bodySm, col.inkMuted, Modifier.padding(top = 6.dp))
            }
            Illustration(illoFor(hero.icon), 110.dp, bg = col.oat)
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            hero.steps.take(3).forEachIndexed { i, st ->
                T("${i + 1}  ${st.title}", Tuulit.type.caption, col.ink, Modifier.clip(RoundedCornerShape(999.dp)).background(col.oat).padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1)
            }
        }
        TButton("Start", { go("session/${hero.id}") }, Modifier.fillMaxWidth().padding(top = 16.dp), icon = TuulitIcons.Breath)
    }

    // Categories + carousel
    T("Find what you need", Tuulit.type.title, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CareCat.entries) { k -> Chip(k.label, cat == k, { cat = k }) }
    }
    val shown = CareContent.all.filter { matches(cat, it) }
    LazyRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(shown, key = { it.id }) { s ->
            Column(Modifier.width(168.dp).height(222.dp).clip(RoundedCornerShape(28.dp)).background(sessionColor(s))
                .clickable(role = androidx.compose.ui.semantics.Role.Button) { go("session/${s.id}") }.padding(16.dp)) {
                Illustration(illoFor(s.icon), 72.dp, bg = col.oat)
                T(s.title, Tuulit.type.title, modifier = Modifier.padding(top = 12.dp), maxLines = 2)
                T("${s.category} · ${s.minutesLabel}", Tuulit.type.caption, col.inkMuted, Modifier.padding(top = 4.dp))
            }
        }
    }

    // Comfort library preview
    val mine = items.filter { it.kind == "comfort" }.sortedByDescending { it.helpedCount }.take(3)
    Row(Modifier.padding(top = 26.dp), verticalAlignment = Alignment.CenterVertically) {
        T("Your comfort library", Tuulit.type.title, modifier = Modifier.weight(1f))
        TButton("Open", openLibrary, kind = ButtonKind.TEXT)
    }
    if (mine.isEmpty()) {
        T("Finish a session or add what helps you, and it shows up here.", Tuulit.type.bodySm, col.inkMuted, Modifier.padding(top = 4.dp))
    } else Column(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(24.dp)).background(col.oat).padding(horizontal = 16.dp)) {
        mine.forEachIndexed { i, it ->
            ListRow(it.title, sub = if (it.helpedCount > 0) "Helped ${it.helpedCount} ${if (it.helpedCount == 1) "time" else "times"}" else it.detail.ifBlank { null },
                leading = { Illustration(illoFor(it.icon), 40.dp, bg = col.peachSoft) }, last = i == mine.lastIndex,
                onClick = it.sessionId?.let { sid -> { go("session/$sid") } })
        }
    }

    // Red flags, folded
    Column(Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(col.alertSoft)
        .clickable(role = androidx.compose.ui.semantics.Role.Button) { flagsOpen = !flagsOpen }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(TuulitIcons.Alert, null, tint = col.alert)
            T("When to see a doctor", Tuulit.type.subtitle, modifier = Modifier.weight(1f).padding(start = 10.dp))
            Icon(TuulitIcons.ChevD, if (flagsOpen) "Collapse" else "Expand", tint = col.ink)
        }
        if (flagsOpen) {
            CareContent.redFlags.forEach { f -> T("•  $f", Tuulit.type.bodySm, col.ink, Modifier.padding(top = 8.dp)) }
            T("If any of these happen, contact a clinician or emergency services.", Tuulit.type.caption, col.inkMuted, Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun PlayHub(go: (String) -> Unit) {
    val c = container()
    val col = Tuulit.colors
    val scores by c.carePlay.gameScores.collectAsState(initial = emptyMap())
    val achievements by c.carePlay.achievements.collectAsState(initial = emptyList())
    val featured = PlayContent.game("banana_catch")
    val daily = PlayContent.games[(Dates.today() % PlayContent.games.size).toInt().let { if (it < 0) -it else it }]
    val plays = scores.values.sumOf { it.plays }

    // Featured game
    val look = gameLook(featured.id)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(look.bg)
        .clickable(role = androidx.compose.ui.semantics.Role.Button) { go("game/${featured.id}") }) {
        GameArt(featured.id, Modifier.size(170.dp).align(Alignment.CenterEnd))
        Column(Modifier.padding(start = 22.dp, top = 22.dp, bottom = 22.dp, end = 150.dp)) {
            Overline("Featured game", look.ink)
            T(featured.title, Tuulit.type.display, look.ink, Modifier.padding(top = 4.dp))
            T(featured.blurb, Tuulit.type.bodySm, look.ink, Modifier.padding(top = 4.dp))
            scores[featured.id]?.best?.takeIf { it > 0 }?.let { T("Your best: $it", Tuulit.type.label, look.ink, Modifier.padding(top = 8.dp)) }
            TButton("Play now", { go("game/${featured.id}") }, Modifier.padding(top = 14.dp))
        }
    }

    // Daily pick + progress
    Row(Modifier.padding(top = 14.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1.2f).fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(gameLook(daily.id).bg)
            .clickable(role = androidx.compose.ui.semantics.Role.Button) { go("game/${daily.id}") }.padding(16.dp)) {
            Overline("Today's pick", gameLook(daily.id).ink)
            T(daily.title, Tuulit.type.title, gameLook(daily.id).ink, Modifier.padding(top = 4.dp))
            val best = scores[daily.id]?.best ?: 0
            T(if (best > 0) "Beat your best of $best" else "Give it a first try", Tuulit.type.caption, gameLook(daily.id).ink, Modifier.padding(top = 4.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(col.oat).padding(16.dp)) {
            T("$plays", Tuulit.type.display)
            T(if (plays == 1) "game played" else "games played", Tuulit.type.caption, col.inkMuted)
            T("${achievements.size} moments", Tuulit.type.label, col.toffee, Modifier.padding(top = 6.dp))
        }
    }

    // All games: each with its own artwork and colour
    T("All games", Tuulit.type.title, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
    PlayContent.games.filter { it.id != featured.id }.chunked(2).forEach { pair ->
        Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pair.forEach { g ->
                val lk = gameLook(g.id)
                Column(Modifier.weight(1f).clip(RoundedCornerShape(28.dp)).background(col.oat)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { go("game/${g.id}") }) {
                    Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(28.dp)).background(lk.bg)) {
                        GameArt(g.id, Modifier.fillMaxSize())
                    }
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp)) {
                        T(g.title, Tuulit.type.subtitle, maxLines = 1)
                        val best = scores[g.id]?.best
                        T(if (best != null && best > 0) "${g.kind} · best $best" else g.kind, Tuulit.type.caption, col.inkMuted)
                    }
                }
            }
            if (pair.size == 1) Box(Modifier.weight(1f))
        }
    }

    // Quizzes
    T("Quizzes", Tuulit.type.title, modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))
    val quizColors = listOf(col.roseSoft, col.fertileSoft, col.peachSoft, col.bananaSoft)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(PlayContent.quizzes.size) { i ->
            val q = PlayContent.quizzes[i]
            Column(Modifier.width(200.dp).height(196.dp).clip(RoundedCornerShape(28.dp)).background(quizColors[i % quizColors.size])
                .clickable(role = androidx.compose.ui.semantics.Role.Button) { go("quiz/${q.id}") }.padding(18.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(col.oat), contentAlignment = Alignment.Center) {
                    Icon(TuulitIcons.named(q.icon), null, tint = col.ink)
                }
                T(q.title, Tuulit.type.title, modifier = Modifier.padding(top = 12.dp), maxLines = 2)
                T(q.subtitle, Tuulit.type.caption, col.inkMuted, Modifier.padding(top = 4.dp), maxLines = 2)
            }
        }
    }

    // Moments
    if (achievements.isNotEmpty()) {
        T("Moments", Tuulit.type.title, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements.size) { i ->
                val a = achievements[i]
                Column(Modifier.width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Illustration(Illo.SPARK, 64.dp, bg = col.bananaSoft)
                    T(a.achievement.title, Tuulit.type.caption, col.ink, Modifier.padding(top = 6.dp), align = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
    T("Scores are personal bests. There are no streaks to keep.", Tuulit.type.caption, col.inkFaint, Modifier.padding(top = 16.dp))
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

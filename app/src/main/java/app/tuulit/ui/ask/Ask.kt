package app.tuulit.ui.ask

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import app.tuulit.ui.designsystem.Blob
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotMood
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tuulit.AppContainer
import app.tuulit.data.local.AiMessageEntity
import app.tuulit.data.remote.AiContext
import app.tuulit.data.remote.AiTool
import app.tuulit.data.remote.ApiError
import app.tuulit.data.remote.AuthState
import app.tuulit.domain.Dates
import app.tuulit.domain.MedicalSafetyGuard
import app.tuulit.domain.UrgencyLevel
import app.tuulit.ui.LocalShell
import app.tuulit.ui.ScreenColumn
import app.tuulit.ui.ShellSheet
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.AppBar
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Chip
import app.tuulit.ui.designsystem.ChipRow
import app.tuulit.ui.designsystem.EmptyState
import app.tuulit.ui.designsystem.Field
import app.tuulit.ui.designsystem.GroupTitle
import app.tuulit.ui.designsystem.IconBtn
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.Notice
import app.tuulit.ui.designsystem.NoticeKind
import app.tuulit.ui.designsystem.Overline
import app.tuulit.ui.designsystem.Panel
import app.tuulit.ui.designsystem.ScreenHeader
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import app.tuulit.ui.tuulitViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class AskViewModel(private val c: AppContainer, val tool: AiTool) : ViewModel() {
    val conversationId = "ask-${tool.wire}"
    val messages: Flow<List<AiMessageEntity>> = c.ai.conversation(conversationId)
    var sending by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var lastUrgency by mutableStateOf(UrgencyLevel.NONE); private set
    var verdict by mutableStateOf<String?>(null); private set

    fun send(text: String) {
        val q = text.trim()
        if (q.isEmpty() || sending) return
        sending = true; error = null
        lastUrgency = MedicalSafetyGuard.assess(q).level
        viewModelScope.launch {
            val s = c.prefs.current()
            val snap = c.cycleState.snapshot.value
            val ctx = if (s.aiContextOptIn && snap.prediction.hasRecordedCycles) AiContext.from(snap.prediction, snap.insights) else null
            try {
                val r = c.ai.ask(conversationId, tool, q, ctx)
                if (r.urgency.ordinal > lastUrgency.ordinal) lastUrgency = r.urgency
                verdict = r.verdict
            } catch (e: ApiError) {
                error = e.message
            }
            sending = false
        }
    }

    fun clear() = viewModelScope.launch { c.ai.clear(conversationId); lastUrgency = UrgencyLevel.NONE; verdict = null; error = null }
}

private data class Shortcut(val label: String, val sub: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)

@Composable
fun AskHome(go: (String) -> Unit) {
    val c = container()
    val col = Tuulit.colors
    val settings by c.prefs.settings.collectAsState(initial = null)
    var draft by remember { mutableStateOf("") }
    val enc = { q: String -> android.net.Uri.encode(q) }
    val shortcuts = listOf(
        Shortcut("Explain my cycle", "Phases, simply", TuulitIcons.Book, "ask/chat/explain?topic=${enc("Explain the phase of my cycle I'm in right now")}"),
        Shortcut("Why am I feeling this way?", "Mood and energy", TuulitIcons.Care, "ask/chat/chat?topic=${enc("Why might I be feeling more emotional than usual?")}"),
        Shortcut("Ask about a symptom", "Cramps, bloating…", TuulitIcons.Pain, "ask/chat/chat"),
        Shortcut("Prepare for my period", "What helps ahead", TuulitIcons.Kit, "ask/chat/chat?topic=${enc("How can I prepare for my period this week?")}"),
        Shortcut("Help me feel better", "Gentle ideas", TuulitIcons.Heat, "ask/chat/chat?topic=${enc("I don't feel great today. What could help?")}"),
        Shortcut("Doctor appointment", "A summary to share", TuulitIcons.Edit, "ask/chat/appointment"),
    )
    val tints = listOf(col.fertileSoft, col.roseSoft, col.peachSoft, col.bananaSoft, col.periodSoft, col.sand)
    val iconTints = listOf(col.lilacDeep, col.periodDeep, col.toffee, col.toffee, col.periodDeep, col.ink)
    ScreenColumn {
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(32.dp)).background(col.fertileSoft)) {
            Blob(col.oat.copy(alpha = 0.6f), Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-30).dp), seed = 5)
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(col.oat), contentAlignment = Alignment.Center) { Mascot(MascotMood.WARM, 46.dp) }
                    Column(Modifier.padding(start = 12.dp)) {
                        Overline("Ask Tuulit", col.lilacDeep)
                        T("Hi, I'm here for you.", Tuulit.type.title)
                    }
                }
                T("What's on your mind?", Tuulit.type.display, modifier = Modifier.padding(top = 14.dp))
                T("Clear, kind answers about your body. Never a diagnosis.", Tuulit.type.bodySm, col.inkMuted, Modifier.padding(top = 6.dp))
            }
        }
        AskGate(go)
        T("Start with", Tuulit.type.title, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        shortcuts.chunked(2).forEachIndexed { r, pair ->
            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { k, sc ->
                    val idx = r * 2 + k
                    Column(Modifier.weight(1f).height(124.dp).clip(RoundedCornerShape(24.dp)).background(tints[idx])
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) { go(sc.route) }.padding(14.dp)) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(col.oat), contentAlignment = Alignment.Center) {
                            Icon(sc.icon, null, Modifier.size(20.dp), tint = iconTints[idx])
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        T(sc.label, Tuulit.type.label, maxLines = 2)
                        T(sc.sub, Tuulit.type.caption, col.inkMuted, maxLines = 1)
                    }
                }
            }
        }
        Composer(draft, { draft = it }, onSend = { if (draft.isNotBlank()) { go("ask/chat/chat?topic=${enc(draft)}"); draft = "" } }, enabled = true)
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            T(if (settings?.aiContextOptIn == true) "Personal answers on" else "Personal answers off · answers stay general",
                Tuulit.type.caption, col.inkFaint, Modifier.weight(1f))
            TButton("Change", { go("you/privacy") }, kind = ButtonKind.TEXT)
        }
        TButton("Myth or fact? Check something you heard", { go("ask/chat/myth") }, kind = ButtonKind.TEXT, icon = TuulitIcons.Quiz)
    }
}

/** Explains why Ask can't run yet (no server URL, no Firebase, not signed in). Renders nothing when ready. */
@Composable
fun AskGate(go: (String) -> Unit) {
    val c = container()
    val auth by c.auth.state.collectAsState(initial = c.auth.current)
    when {
        !c.api.isConfigured -> Notice("Ask isn't connected yet", "This build has no Tuulit server address (TUULIT_API_BASE_URL). See backend/worker/README.md.", Modifier.padding(top = 14.dp))
        auth is AuthState.Unavailable -> Notice("Ask needs an account", app.tuulit.data.remote.Firebase.notConfiguredMessage, Modifier.padding(top = 14.dp))
        auth is AuthState.SignedOut -> Notice("Sign in to use Ask", "Questions go to Tuulit's server with your account so they can't be abused. Your conversations stay on this phone.",
            Modifier.padding(top = 14.dp), action = { TButton("Sign in or create an account", { go("auth/signin") }, kind = ButtonKind.TEXT) })
    }
}

@Composable
private fun Composer(value: String, onChange: (String) -> Unit, onSend: () -> Unit, enabled: Boolean, placeholder: String = "Ask about your body") {
    val c = Tuulit.colors
    Row(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(999.dp), ambientColor = c.fertile, spotColor = c.fertile)
        .clip(RoundedCornerShape(999.dp)).background(c.oat).padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Field("", value, { onChange(it.take(1000)) }, Modifier.weight(1f), placeholder = placeholder,
            keyboard = KeyboardOptions(imeAction = ImeAction.Send), actions = KeyboardActions(onSend = { onSend() }), bare = true)
        IconBtn(TuulitIcons.ArrowUp, "Send", onSend, filled = true, enabled = enabled && value.isNotBlank())
    }
}

private val explainTopics = mapOf(
    "follicular" to "What happens in the follicular phase?",
    "ovulation" to "What does ovulation feel like?",
)

@Composable
fun AskChat(toolName: String, topic: String?, onBack: () -> Unit, go: (String) -> Unit) {
    val tool = AiTool.entries.firstOrNull { it.wire == toolName } ?: AiTool.CHAT
    if (tool == AiTool.APPOINTMENT) { AppointmentPrep(onBack, go); return }
    val vm = tuulitViewModel(key = "ask-$toolName") { AskViewModel(it, tool) }
    val shell = LocalShell.current
    val messages by vm.messages.collectAsState(initial = emptyList())
    var draft by remember { mutableStateOf("") }
    LaunchedEffect(topic) {
        val q = topic?.let { explainTopics[it] ?: it }
        if (!q.isNullOrBlank()) vm.send(q)
    }
    val list = rememberLazyListState()
    LaunchedEffect(messages.size, vm.sending) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.size) }
    val title = when (tool) { AiTool.EXPLAIN -> "Explain my body"; AiTool.MYTH -> "Myth or fact?"; else -> "Ask" }
    Box(Modifier.fillMaxSize().background(Tuulit.colors.cream), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().padding(horizontal = 20.dp).imePadding()) {
            Box(Modifier.padding(top = 32.dp)) {
                AppBar(title, onBack) { if (messages.isNotEmpty()) IconBtn(TuulitIcons.Trash, "Clear conversation", { vm.clear() }) }
            }
            AskGate(go)
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (messages.isEmpty()) item {
                    EmptyState(
                        when (tool) { AiTool.EXPLAIN -> "What would you like explained?"; AiTool.MYTH -> "What did you hear?"; else -> "Ask anything about your cycle" },
                        when (tool) { AiTool.MYTH -> "e.g. \"Every normal cycle is 28 days\""; AiTool.EXPLAIN -> "e.g. \"Why do periods hurt?\""; else -> "Answers are general information, never a diagnosis." },
                        mood = null, icon = if (tool == AiTool.MYTH) TuulitIcons.Quiz else TuulitIcons.Book)
                }
                items(messages, key = { it.id }) { m ->
                    val urgent = m.role == "user" && (m.urgency == UrgencyLevel.URGENT.name || m.urgency == UrgencyLevel.EMERGENCY.name)
                    Bubble(m)
                    if (urgent) SafetyBlock(UrgencyLevel.valueOf(m.urgency), onTellSomeone = { shell.sheet = ShellSheet.SOS })
                }
                if (vm.sending) item { Bubble(AiMessageEntity(conversationId = "", role = "assistant", text = "…", createdAt = 0)) }
                item { Box(Modifier.size(8.dp)) }
            }
            vm.error?.let { T(it, Tuulit.type.caption, Tuulit.colors.alert, Modifier.padding(bottom = 6.dp)) }
            if (messages.isEmpty() && tool == AiTool.EXPLAIN) ChipRow(Modifier.padding(bottom = 10.dp)) {
                listOf("Why do periods hurt?", "What is the luteal phase?", "Why am I bloated?").forEach { s -> Chip(s, false, { vm.send(s) }) }
            }
            Composer(draft, { draft = it }, onSend = { vm.send(draft); draft = "" }, enabled = !vm.sending,
                placeholder = when (tool) { AiTool.MYTH -> "e.g. Every normal cycle is 28 days"; AiTool.EXPLAIN -> "What would you like explained?"; else -> "Ask about your body" })
            T("General information · not a diagnosis", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(vertical = 8.dp).fillMaxWidth())
        }
    }
}

@Composable
private fun Bubble(m: AiMessageEntity) {
    val c = Tuulit.colors
    val mine = m.role == "user"
    val typing = !mine && m.text == "…"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!mine) {
            Box(Modifier.padding(end = 8.dp).size(34.dp).clip(CircleShape).background(c.bananaSoft), contentAlignment = Alignment.Center) {
                Mascot(MascotMood.WARM, 28.dp)
            }
        }
        Column(
            Modifier.widthIn(max = 290.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = if (mine) 22.dp else 6.dp, bottomEnd = if (mine) 6.dp else 22.dp))
                .background(if (mine) c.peach else if (m.failed) c.alertSoft else c.oat).padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { if (!mine) liveRegion = LiveRegionMode.Polite },
        ) {
            if (typing) TypingDots() else {
                val paragraphs = m.text.split("\n").filter { it.isNotBlank() }
                paragraphs.forEachIndexed { i, para ->
                    val bullet = para.trimStart().let { it.startsWith("- ") || it.startsWith("• ") || it.matches(Regex("^\\d+[.)] .*")) }
                    T(para.trim(), (if (i == 0 && !mine && !m.failed) Tuulit.type.subtitle.copy(fontSize = Tuulit.type.body.fontSize) else Tuulit.type.body),
                        c.ink, Modifier.padding(top = if (i == 0) 0.dp else if (bullet) 4.dp else 8.dp, start = if (bullet) 4.dp else 0.dp))
                }
            }
            if (!mine && !m.failed && !typing) T(if (m.usedContext) "Used a summary of your cycle · not a diagnosis" else "General information · not a diagnosis",
                Tuulit.type.caption, c.inkFaint, Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun TypingDots() {
    val c = Tuulit.colors
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")
    Row(Modifier.semantics { contentDescription = "Tuulit is thinking" }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val a by t.animateFloat(0.3f, 1f, androidx.compose.animation.core.infiniteRepeatable(
                androidx.compose.animation.core.tween(500, delayMillis = i * 160), androidx.compose.animation.core.RepeatMode.Reverse), label = "dot$i")
            Box(Modifier.size(9.dp).clip(CircleShape).background(c.fertile.copy(alpha = if (Tuulit.reducedMotion) 1f else a)))
        }
    }
}

@Composable
private fun SafetyBlock(level: UrgencyLevel, onTellSomeone: () -> Unit) {
    val ctx = LocalContext.current
    val a = MedicalSafetyGuard.assess(if (level == UrgencyLevel.EMERGENCY) "chest pain" else "fever")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Notice(a.headline, a.message, kind = NoticeKind.MEDICAL)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (level == UrgencyLevel.EMERGENCY) TButton("Call emergency", {
                ctx.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }, Modifier.weight(1f), kind = ButtonKind.DANGER, icon = TuulitIcons.Phone)
            TButton("Tell my person", onTellSomeone, Modifier.weight(1f), kind = ButtonKind.QUIET, icon = TuulitIcons.Sos)
        }
    }
}

/** Appointment prep: a local summary from the person's own logs, plus optional AI-suggested questions. */
@Composable
private fun AppointmentPrep(onBack: () -> Unit, go: (String) -> Unit) {
    val c = container()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snap by c.cycleState.snapshot.collectAsState()
    val vm = tuulitViewModel(key = "ask-appointment") { AskViewModel(it, AiTool.APPOINTMENT) }
    val messages by vm.messages.collectAsState(initial = emptyList())
    var concern by remember { mutableStateOf("") }
    val i = snap.insights
    val summary = buildList {
        add("Cycle" to if (i.averageCycleLength != null) "${i.averageCycleLength} days avg${i.cycleVariabilityDays?.let { v -> " (±${v / 2})" } ?: ""} · period ${i.averagePeriodDuration ?: snap.settings.periodLength} days" else "Not enough cycles logged yet")
        i.painPatternAlert?.let { add("Pain" to "Severe (7+) in ${it.consecutiveHighPainCycles} cycles · affected daily life ${it.impactedActivitiesCount} times") }
        if (i.symptomFrequency.isNotEmpty()) add("Most logged" to i.symptomFrequency.take(3).joinToString { it.first })
        snap.cycles.lastOrNull()?.let { add("Last period" to Dates.long(it.startEpochDay)) }
    }
    val questions = messages.lastOrNull { it.role == "assistant" && !it.failed }?.text
    ScreenColumn {
        AppBar("Appointment prep", onBack)
        T("For your appointment", Tuulit.type.display, modifier = Modifier.padding(top = 10.dp))
        T("Built from your own logs on this phone.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp, bottom = 10.dp))
        summary.forEachIndexed { idx, (k, v) -> ListRow(v, sub = k, last = idx == summary.lastIndex) }
        GroupTitle("Questions to ask", Modifier.padding(top = 18.dp))
        if (questions == null) {
            Field("Main concern (optional)", concern, { concern = it.take(200) }, placeholder = "e.g. painful periods")
            T("Suggesting questions sends this summary (not your notes) to Tuulit's server.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 6.dp))
            AskGate(go)
            TButton("Suggest questions", {
                vm.send("Concern: ${concern.ifBlank { "general check-up" }}. Summary: " + summary.joinToString("; ") { "${it.first}: ${it.second}" })
            }, Modifier.padding(top = 10.dp), kind = ButtonKind.QUIET, loading = vm.sending, icon = TuulitIcons.Spark)
            vm.error?.let { T(it, Tuulit.type.caption, Tuulit.colors.alert, Modifier.padding(top = 6.dp)) }
        } else {
            Panel(padding = 16.dp) { T(questions, Tuulit.type.bodySm) }
            TButton("Start again", { vm.clear() }, kind = ButtonKind.TEXT)
        }
        TButton("Share as PDF", { scope.launch { sharePdf(ctx, summary, questions) } }, Modifier.fillMaxWidth().padding(top = 18.dp), icon = TuulitIcons.Share)
        T("A summary of your own records, not a diagnosis.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 8.dp))
    }
}

private fun sharePdf(ctx: Context, rows: List<Pair<String, String>>, questions: String?) {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val cv = page.canvas
    val title = Paint().apply { textSize = 22f; isFakeBoldText = true }
    val label = Paint().apply { textSize = 11f; color = 0xFF5E554A.toInt() }
    val body = Paint().apply { textSize = 13f }
    var y = 60f
    cv.drawText("Tuulit · appointment summary", 48f, y, title); y += 22f
    cv.drawText("Created ${Dates.long(Dates.today())} from the patient's own logs. Not a diagnosis.", 48f, y, label); y += 34f
    rows.forEach { (k, v) -> cv.drawText(k, 48f, y, label); y += 16f; cv.drawText(v, 48f, y, body); y += 26f }
    if (questions != null) {
        y += 8f; cv.drawText("Questions to ask", 48f, y, label); y += 18f
        questions.lines().flatMap { it.chunked(80) }.forEach { line -> if (y < 800) { cv.drawText(line, 48f, y, body); y += 18f } }
    }
    doc.finishPage(page)
    val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "tuulit-appointment-${Dates.iso(Dates.today())}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", file)
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Share summary").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

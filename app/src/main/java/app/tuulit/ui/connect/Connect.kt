package app.tuulit.ui.connect

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.tuulit.data.remote.AuthState
import app.tuulit.data.remote.Connection
import app.tuulit.data.remote.Invite
import app.tuulit.data.remote.SharePermissions
import app.tuulit.domain.Achievement
import app.tuulit.domain.Dates
import app.tuulit.domain.Validation
import app.tuulit.ui.LocalShell
import app.tuulit.ui.ScreenColumn
import app.tuulit.ui.TuulitSheet
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.AppBar
import app.tuulit.ui.designsystem.Avatar
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Chip
import app.tuulit.ui.designsystem.ChipRow
import app.tuulit.ui.designsystem.EmptyState
import app.tuulit.ui.designsystem.Field
import app.tuulit.ui.designsystem.GroupTitle
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotMood
import app.tuulit.ui.designsystem.Notice
import app.tuulit.ui.designsystem.NoticeKind
import app.tuulit.ui.designsystem.Overline
import app.tuulit.ui.designsystem.Panel
import app.tuulit.ui.designsystem.SettingRow
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import kotlinx.coroutines.launch

/** Sign-in gate shared by every Connect screen. Returns true when Connect can be used. */
@Composable
private fun connectReady(go: ((String) -> Unit)?): Boolean {
    val c = container()
    val auth by c.auth.state.collectAsState(initial = c.auth.current)
    return when (auth) {
        is AuthState.SignedIn -> true
        AuthState.Unavailable -> { Notice("Connect needs accounts", app.tuulit.data.remote.Firebase.notConfiguredMessage, Modifier.padding(top = 12.dp)); false }
        AuthState.SignedOut -> {
            Notice("Sign in to connect", "Both of you need a Tuulit account so updates go only to the right person.", Modifier.padding(top = 12.dp),
                action = { if (go != null) TButton("Sign in or create an account", { go("auth/signin") }, kind = ButtonKind.TEXT) })
            false
        }
    }
}

@Composable
fun ConnectScreen(onBack: () -> Unit, go: (String) -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val mine by c.connect.myPeople.collectAsState(initial = emptyList())
    val sharing by c.connect.sharingWithMe.collectAsState(initial = emptyList())
    val settings by c.prefs.settings.collectAsState(initial = null)
    var editSos by remember { mutableStateOf(false) }
    ScreenColumn {
        AppBar("Connect", onBack)
        app.tuulit.ui.designsystem.Illustration(app.tuulit.ui.designsystem.Illo.HEART, 96.dp, Modifier.padding(top = 6.dp), bg = Tuulit.colors.roseSoft)
        T("Let someone in, on your terms.", Tuulit.type.display, modifier = Modifier.padding(top = 14.dp))
        T("They only see what you switch on. Symptoms, moods and notes are never shared.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp))
        if (!connectReady(go)) return@ScreenColumn
        GroupTitle("People who can see your updates", Modifier.padding(top = 20.dp))
        if (mine.isEmpty()) T("No one yet.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(vertical = 8.dp))
        mine.forEachIndexed { i, p ->
            val on = listOfNotNull("period soon".takeIf { p.permissions.periodSoon }, "period started".takeIf { p.permissions.periodStarted },
                "rough-day signal".takeIf { p.permissions.roughDay }, "phase".takeIf { p.permissions.phase })
            ListRow(p.memberName, sub = (p.relationship.ifBlank { "Trusted person" }) + " · " + if (on.isEmpty()) "sees nothing yet" else "sees " + on.joinToString(", "),
                leading = { Avatar(p.memberName) }, chevron = true, last = i == mine.lastIndex, onClick = { go("connect/person/${p.id}") })
        }
        TButton("Invite someone", { go("connect/invite") }, Modifier.fillMaxWidth().padding(top = 12.dp), kind = ButtonKind.QUIET, icon = TuulitIcons.Plus)
        GroupTitle("People sharing with you", Modifier.padding(top = 24.dp))
        if (sharing.isEmpty()) T("If someone invites you, enter their code here.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(vertical = 8.dp))
        sharing.forEachIndexed { i, p -> ListRow(p.ownerName, sub = "Shares updates with you", leading = { Avatar(p.ownerName) }, chevron = true,
            last = i == sharing.lastIndex, onClick = { go("partner/${p.id}") }) }
        TButton("I have an invite code", { go("connect/accept") }, kind = ButtonKind.TEXT)
        GroupTitle("SOS messages", Modifier.padding(top = 24.dp))
        (settings?.sosMessages ?: emptyList()).forEachIndexed { i, m -> ListRow("“$m”", last = i == (settings?.sosMessages?.size ?: 1) - 1) }
        TButton("Edit SOS messages", { editSos = true }, kind = ButtonKind.TEXT, icon = TuulitIcons.Edit)
    }
    if (editSos) {
        val initial = settings?.sosMessages ?: emptyList()
        val drafts = remember { initial.toMutableList().let { l -> (l + List((4 - l.size).coerceAtLeast(0)) { "" }).toMutableList() } }
        var version by remember { mutableStateOf(0) }
        TuulitSheet({ editSos = false }) {
            T("Your SOS messages", Tuulit.type.headline)
            T("Sent with one tap from Tuulit SOS. Keep them short.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp, bottom = 12.dp))
            drafts.indices.forEach { i ->
                Field("Message ${i + 1}", drafts[i].also { version }, { drafts[i] = it.take(140); version++ }, Modifier.padding(bottom = 10.dp))
            }
            TButton("Save", { scope.launch { c.prefs.setSosMessages(drafts); editSos = false; shell.showToast("SOS messages saved") } }, Modifier.fillMaxWidth(),
                enabled = drafts.any { it.isNotBlank() })
        }
    }
}

@Composable
fun InviteScreen(onBack: () -> Unit) {
    val c = container()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by c.prefs.settings.collectAsState(initial = null)
    val auth by c.auth.state.collectAsState(initial = c.auth.current)
    var invite by remember { mutableStateOf<Invite?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    ScreenColumn {
        AppBar(onBack = onBack, closeIcon = true)
        T("Invite your person", Tuulit.type.display, modifier = Modifier.padding(top = 8.dp))
        T("They install Tuulit and enter this code. They'll only see what you switch on, and you can disconnect any time.",
            Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 20.dp))
        if (!connectReady(null)) return@ScreenColumn
        val inv = invite
        if (inv == null) {
            TButton("Create invite code", {
                busy = true
                scope.launch {
                    val name = settings?.displayName?.ifBlank { null } ?: (auth as? AuthState.SignedIn)?.displayName.orEmpty()
                    c.connect.createInvite(name).onSuccess { invite = it }.onFailure { error = it.message }
                    busy = false
                }
            }, Modifier.fillMaxWidth(), loading = busy)
        } else {
            Panel(padding = 28.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    T("Invite code", Tuulit.type.caption, Tuulit.colors.inkMuted)
                    T(inv.code.chunked(3).joinToString(" "), Tuulit.type.numeral, modifier = Modifier.padding(vertical = 8.dp))
                    T("Expires ${Dates.short(Dates.epochDayOfMillis(inv.expiresAt))}, 24 hours from now", Tuulit.type.caption, Tuulit.colors.inkFaint)
                }
            }
            T("When they accept, you'll get a notification. Everything starts switched off until you choose what they can see.",
                Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 16.dp))
            TButton("Send invite", {
                val text = "Join me on Tuulit so I can share period updates with you. Open Connect → \"I have an invite code\" and enter ${inv.code}."
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Send invite"))
            }, Modifier.fillMaxWidth().padding(top = 20.dp), icon = TuulitIcons.Share)
        }
        error?.let { Notice("Couldn't create a code", it, Modifier.padding(top = 12.dp), kind = NoticeKind.MEDICAL) }
    }
}

@Composable
fun AcceptInviteScreen(onBack: () -> Unit, go: (String) -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val settings by c.prefs.settings.collectAsState(initial = null)
    var code by remember { mutableStateOf("") }
    var name by remember(settings?.displayName) { mutableStateOf(settings?.displayName.orEmpty()) }
    var relationship by remember { mutableStateOf("Partner") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    ScreenColumn {
        AppBar(onBack = onBack, closeIcon = true)
        T("Enter an invite code", Tuulit.type.display, modifier = Modifier.padding(top = 8.dp))
        T("You'll see only the updates they choose to share.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 20.dp))
        if (!connectReady(go)) return@ScreenColumn
        Field("Invite code", code, { code = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(6); error = null }, placeholder = "e.g. 4K8P2Q",
            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
        Field("Your name, as they'll see it", name, { name = it.take(40) }, Modifier.padding(top = 14.dp))
        T("You are their…", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 14.dp, bottom = 6.dp))
        ChipRow { listOf("Partner", "Friend", "Family", "Other").forEach { r -> Chip(r, relationship == r, { relationship = r }) } }
        error?.let { Notice("That didn't work", it, Modifier.padding(top = 12.dp), kind = NoticeKind.MEDICAL) }
        TButton("Connect", {
            val v = Validation.inviteCode(code)
            if (v != null) { error = v; return@TButton }
            busy = true
            scope.launch {
                c.connect.acceptInvite(code, name, relationship)
                    .onSuccess { c.carePlay.unlock(Achievement.CONNECTED); shell.showToast("Connected"); onBack() }
                    .onFailure { error = it.message }
                busy = false
            }
        }, Modifier.fillMaxWidth().padding(top = 20.dp), loading = busy)
    }
}

/** One trusted person: what they can see, their relationship, and disconnecting. */
@Composable
fun PersonScreen(id: String, onBack: () -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val people by c.connect.myPeople.collectAsState(initial = null)
    val snap by c.cycleState.snapshot.collectAsState()
    val p = people?.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(p?.id) { if (p != null) c.carePlay.unlock(Achievement.CONNECTED) }
    ScreenColumn {
        AppBar(p?.memberName ?: "Trusted person", onBack)
        if (people != null && p == null) { EmptyState("This connection has ended", "They may have disconnected.", mood = MascotMood.CHILL); return@ScreenColumn }
        if (p == null) return@ScreenColumn
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(p.memberName, 56.dp)
            Column { T(p.memberName, Tuulit.type.headline); T(p.relationship.ifBlank { "Trusted person" } + " · connected", Tuulit.type.caption, Tuulit.colors.inkMuted) }
        }
        GroupTitle("${p.memberName} can see", Modifier.padding(top = 8.dp))
        fun update(perms: SharePermissions) = scope.launch {
            c.connect.updatePermissions(p, perms, snap.prediction).onFailure { shell.showToast(it.message ?: "Couldn't update") }
        }
        SettingRow("Period coming up", "A heads-up 2 days before", p.permissions.periodSoon, { update(p.permissions.copy(periodSoon = it)) })
        SettingRow("Period started", "With tips on how to help", p.permissions.periodStarted, { update(p.permissions.copy(periodStarted = it)) })
        SettingRow("Rough-day signal", "When you send an SOS", p.permissions.roughDay, { update(p.permissions.copy(roughDay = it)) })
        SettingRow("Cycle phase", "Never symptoms, mood or notes", p.permissions.phase, { update(p.permissions.copy(phase = it)) }, last = true)
        T("Changes apply straight away. Turning something off removes it from their view.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 8.dp))
        if (!confirm) TButton("Disconnect ${p.memberName}", { confirm = true }, Modifier.padding(top = 20.dp), kind = ButtonKind.TEXT)
        else Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TButton("Keep", { confirm = false }, Modifier.weight(1f), kind = ButtonKind.QUIET)
            TButton("Disconnect", { scope.launch { c.connect.disconnect(p); shell.showToast("Disconnected"); onBack() } }, Modifier.weight(1f), kind = ButtonKind.DANGER)
        }
    }
}

/** What a member sees for someone who shares with them: one status line, how to help, one-tap replies. */
@Composable
fun PartnerScreen(id: String, onBack: () -> Unit, go: (String) -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val sharing by c.connect.sharingWithMe.collectAsState(initial = null)
    val conn = sharing?.firstOrNull { it.id == id }
    val status by c.connect.sharedStatus(id).collectAsState(initial = null)
    var confirm by remember { mutableStateOf(false) }
    ScreenColumn {
        AppBar(onBack = onBack)
        if (sharing != null && conn == null) { EmptyState("This connection has ended", "They may have disconnected.", mood = MascotMood.CHILL); return@ScreenColumn }
        val name = conn?.ownerName ?: status?.ownerName ?: "Your person"
        val today = Dates.today()
        val s = status
        val started = s?.periodStartedOn
        val soon = s?.periodSoonOn
        val rough = s?.roughDayAt?.let { Dates.epochDayOfMillis(it) == today } == true
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Mascot(if (started != null || rough) MascotMood.CRAMPS else MascotMood.WARM, 44.dp)
            Column { Overline("Shared by $name"); T("Today", Tuulit.type.subtitle) }
        }
        val headline = when {
            started != null && started == today -> "$name's period started today."
            started != null -> "$name is on day ${today - started + 1} of their period."
            soon != null -> "$name's period may start ${if (soon - today == 0L) "today" else "in ${soon - today} days"}."
            s?.phase != null -> "$name is in the ${s.phase.lowercase()}."
            else -> "Nothing to share right now."
        }
        T(headline, Tuulit.type.display, modifier = Modifier.padding(top = 22.dp))
        if (rough) T("They sent a rough-day signal${s?.lastNote?.let { ": “$it”" } ?: "."}", Tuulit.type.body, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp))
        if (started != null || rough || soon != null) {
            GroupTitle("Ways to help today", Modifier.padding(top = 22.dp))
            ListRow("Warm up a heat pack", sub = "Heat helps cramps more than people expect", icon = TuulitIcons.Heat)
            ListRow("Take care of dinner", sub = "Energy is often lowest on day 1", icon = TuulitIcons.Food)
            ListRow("Ask, don't guess", sub = "“Company or quiet?” works well", icon = TuulitIcons.Ask, last = true)
        }
        GroupTitle("Send a little love", Modifier.padding(top = 18.dp, bottom = 8.dp))
        ChipRow {
            listOf("Thinking of you", "On my way with snacks", "Want company or quiet?").forEach { m ->
                Chip(m, false, {
                    val cn = conn ?: return@Chip
                    scope.launch { val r = c.connect.sendReply(cn, m); shell.showToast(if (r.isSuccess) "Sent" else (r.exceptionOrNull()?.message ?: "Couldn't send")) }
                })
            }
        }
        T("You only see what $name chooses to share.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 16.dp))
        TButton("Take the couple quiz", { go("quiz/couple_support") }, kind = ButtonKind.TEXT, icon = TuulitIcons.Quiz)
        if (conn != null) {
            if (!confirm) TButton("Stop receiving updates", { confirm = true }, kind = ButtonKind.TEXT)
            else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TButton("Keep", { confirm = false }, Modifier.weight(1f), kind = ButtonKind.QUIET)
                TButton("Disconnect", { scope.launch { c.connect.disconnect(conn); onBack() } }, Modifier.weight(1f), kind = ButtonKind.DANGER)
            }
        }
    }
}

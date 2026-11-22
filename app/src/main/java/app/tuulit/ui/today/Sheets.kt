package app.tuulit.ui.today

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import app.tuulit.data.remote.AuthState
import app.tuulit.data.remote.Connection
import app.tuulit.domain.BananaModePersona
import app.tuulit.domain.CareContent
import app.tuulit.domain.Dates
import app.tuulit.ui.LocalShell
import app.tuulit.ui.ShellSheet
import app.tuulit.ui.TuulitSheet
import app.tuulit.ui.VSpace
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.Avatar
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Hairline
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.Overline
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import app.tuulit.ui.designsystem.mood
import kotlinx.coroutines.launch

private data class Need(val label: String, val icon: ImageVector, val kicker: String, val title: String, val body: String,
                        val primary: String, val secondary: String?)

private val needs = listOf(
    Need("I'm in pain", TuulitIcons.Pain, "For pain, try this first", "Heat, then slow breathing",
        "A warm compress on your lower belly with 4-6 breathing for 5 minutes.", "Start 5 min", "Log cramps"),
    Need("Exhausted", TuulitIcons.Moon, "When you're running on empty", "Make today a quiet day",
        "Today Is Not the Day hides everything except four gentle actions until midnight.", "Quiet mode on", null),
    Need("Feeling low", TuulitIcons.Care, "For a low moment", "Two-minute mood reset",
        "Name the feeling, ground yourself, choose one kind thing.", "Start 2 min", "Log my mood"),
    Need("On edge", TuulitIcons.Breath, "To take the edge off", "Box breathing",
        "In 4, hold 4, out 4. Two minutes.", "Start 2 min", null),
    Need("Bored", TuulitIcons.Play, "Two minutes off", "Play something",
        "Banana Catch, Word Puzzle and six more. Personal bests, never streaks.", "Open Play", null),
    Need("Can't sleep", TuulitIcons.Moon, "For tonight", "Sleep wind-down",
        "Ten quiet minutes: body scan and 4-7-8 breathing.", "Start 10 min", null),
    Need("A question", TuulitIcons.Ask, "Ask Tuulit", "Clear answers about your body",
        "General information, never a diagnosis.", "Open Ask", null),
    Need("Tell my person", TuulitIcons.Sos, "", "", "", "", null),
)

/** I Need Something: triage in one sheet. Picking a feeling reveals one gentle next step below. */
@Composable
fun NeedSomethingSheet(nav: NavHostController, onDismiss: () -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<Need?>(null) }
    TuulitSheet(onDismiss) {
        T("What would help right now?", Tuulit.type.headline)
        T("Pick the closest. Tuulit takes you to one gentle next step.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp, bottom = 18.dp))
        needs.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { n ->
                    val on = picked == n
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) Tuulit.colors.ink else Tuulit.colors.sand)
                            .clickable(role = Role.RadioButton) {
                                if (n.label == "Tell my person") { onDismiss(); shell.sheet = ShellSheet.SOS } else picked = n
                            }.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(n.icon, null, Modifier.size(22.dp), tint = if (on) Tuulit.colors.onInk else Tuulit.colors.inkMuted)
                        T(n.label, Tuulit.type.caption, if (on) Tuulit.colors.onInk else Tuulit.colors.ink, align = TextAlign.Center)
                    }
                }
            }
        }
        val n = picked
        if (n != null) {
            Hairline(Modifier.padding(top = 12.dp, bottom = 16.dp))
            Overline(n.kicker, Tuulit.colors.toffee)
            T(n.title, Tuulit.type.title, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            T(n.body, Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(bottom = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (n.secondary != null) TButton(n.secondary, { onDismiss(); shell.openLog(Dates.today()) }, Modifier.weight(1f), kind = ButtonKind.QUIET)
                TButton(n.primary, {
                    onDismiss()
                    when (n.label) {
                        "I'm in pain" -> nav.navigate("session/${CareContent.heatAndEase.id}")
                        "Exhausted" -> scope.launch { c.prefs.setNotTheDay(Dates.today()) }
                        "Feeling low" -> nav.navigate("session/${CareContent.moodReset.id}")
                        "On edge" -> nav.navigate("session/${CareContent.boxBreathing.id}")
                        "Bored" -> nav.navigate("care/play")
                        "Can't sleep" -> nav.navigate("session/${CareContent.sleepWindDown.id}")
                        "A question" -> nav.navigate("ask")
                    }
                }, Modifier.weight(1f))
            }
        }
    }
}

/**
 * Tuulit SOS. Connected people with "Rough-day signal" switched on get an in-app push via the
 * Worker; otherwise the message opens in the person's own messaging app. Nothing sends without a tap.
 */
@Composable
fun SosSheet(nav: NavHostController, onDismiss: () -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by c.prefs.settings.collectAsState(initial = null)
    val auth by c.auth.state.collectAsState(initial = c.auth.current)
    val people by c.connect.myPeople.collectAsState(initial = emptyList())
    val snapshot by c.cycleState.snapshot.collectAsState()
    var target by remember { mutableStateOf<Connection?>(null) }
    val eligible = people.filter { it.permissions.roughDay }
    val person = target ?: eligible.firstOrNull()
    var sending by remember { mutableStateOf(false) }
    TuulitSheet(onDismiss) {
        T("Send a quick message", Tuulit.type.headline)
        if (person != null) {
            Row(Modifier.padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Avatar(person.memberName, 36.dp)
                Column(Modifier.weight(1f)) { T(person.memberName, Tuulit.type.subtitle); T(person.relationship.ifBlank { "Trusted person" }, Tuulit.type.caption, Tuulit.colors.inkMuted) }
                if (eligible.size > 1) TButton("Change", { target = eligible[(eligible.indexOf(person) + 1) % eligible.size] }, kind = ButtonKind.TEXT)
            }
        } else {
            T(if (auth is AuthState.SignedIn) "No one has Rough-day signal switched on yet, so this opens your messaging app."
            else "This opens your messaging app with the text ready. Connect a trusted person to send it in Tuulit.",
                Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 6.dp))
        }
        (settings?.sosMessages ?: emptyList()).forEachIndexed { i, msg ->
            ListRow("“$msg”", last = i == (settings?.sosMessages?.size ?: 0) - 1, trailing = {
                Icon(TuulitIcons.Send, null, Modifier.size(20.dp), tint = Tuulit.colors.inkMuted)
            }, onClick = {
                if (sending) return@ListRow
                if (person != null) {
                    sending = true
                    scope.launch {
                        val r = c.connect.sendSos(person, msg, snapshot.prediction)
                        sending = false
                        onDismiss()
                        shell.showToast(if (r.isSuccess) "Sent to ${person.memberName}" else (r.exceptionOrNull()?.message ?: "Couldn't send"))
                    }
                } else {
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, msg) }
                    ctx.startActivity(Intent.createChooser(send, "Send with"))
                    onDismiss()
                }
            })
        }
        T(if (person != null) "Sends a notification to ${person.memberName}'s phone." else "Nothing sends without you.",
            Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 14.dp))
        TButton("Edit messages or people", { onDismiss(); nav.navigate("connect") }, kind = ButtonKind.TEXT)
    }
}

@Composable
fun BananaModeSheet(onDismiss: () -> Unit) {
    val c = container()
    val scope = rememberCoroutineScope()
    val settings by c.prefs.settings.collectAsState(initial = null)
    TuulitSheet(onDismiss) {
        T("Banana Mode", Tuulit.type.headline)
        T("Choose the mood your banana keeps you company in.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp, bottom = 8.dp))
        BananaModePersona.entries.forEachIndexed { i, p ->
            ListRow(p.title, sub = p.tagline, last = false, leading = { Mascot(p.mood(), 44.dp) },
                trailing = { if (settings?.bananaMode == p) Icon(TuulitIcons.Check, "Selected", tint = Tuulit.colors.ink) },
                onClick = { scope.launch { c.prefs.setBananaMode(p) }; onDismiss() })
        }
        ListRow("Today is not the day", sub = "Hide everything but four gentle actions until midnight", icon = TuulitIcons.Moon, last = true,
            onClick = { scope.launch { c.prefs.setNotTheDay(Dates.today()) }; onDismiss() })
        Spacer(Modifier.size(4.dp))
    }
}

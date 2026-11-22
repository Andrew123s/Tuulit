package app.tuulit.ui.you

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.tuulit.BuildConfig
import app.tuulit.data.local.ThemeMode
import app.tuulit.data.remote.AuthResult
import app.tuulit.data.remote.AuthState
import app.tuulit.data.remote.SyncResult
import app.tuulit.domain.BananaModePersona
import app.tuulit.domain.CycleRegularity
import app.tuulit.domain.Dates
import app.tuulit.domain.Validation
import app.tuulit.notifications.Notifications
import app.tuulit.ui.LocalShell
import app.tuulit.ui.ScreenColumn
import app.tuulit.ui.TuulitSheet
import app.tuulit.ui.VSpace
import app.tuulit.ui.auth.Keypad
import app.tuulit.ui.auth.PinDots
import app.tuulit.ui.auth.VerifyEmailNotice
import app.tuulit.ui.container
import app.tuulit.ui.cycle.shareExport
import app.tuulit.ui.designsystem.AppBar
import app.tuulit.ui.designsystem.Avatar
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Chip
import app.tuulit.ui.designsystem.ChipRow
import app.tuulit.ui.designsystem.Field
import app.tuulit.ui.designsystem.GroupTitle
import app.tuulit.ui.designsystem.IconBtn
import app.tuulit.ui.designsystem.ListRow
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotMood
import app.tuulit.ui.designsystem.Notice
import app.tuulit.ui.designsystem.NoticeKind
import app.tuulit.ui.designsystem.Panel
import app.tuulit.ui.designsystem.SettingRow
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import app.tuulit.ui.designsystem.mood
import kotlinx.coroutines.launch

@Composable
fun YouScreen(onBack: () -> Unit, go: (String) -> Unit) {
    val c = container()
    val s by c.prefs.settings.collectAsState(initial = null)
    val auth by c.auth.state.collectAsState(initial = c.auth.current)
    val snap by c.cycleState.snapshot.collectAsState()
    val signed = auth as? AuthState.SignedIn
    val name = s?.displayName?.ifBlank { null } ?: signed?.displayName?.ifBlank { null } ?: "You"
    ScreenColumn {
        AppBar(onBack = onBack)
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(name, 56.dp)
            Column {
                T(name, Tuulit.type.headline)
                T(snap.cycles.firstOrNull()?.let { "Tracking since ${Dates.month(Dates.date(it.startEpochDay))} · ${snap.cycles.size} ${if (snap.cycles.size == 1) "cycle" else "cycles"}" }
                    ?: "No cycles logged yet", Tuulit.type.caption, Tuulit.colors.inkMuted)
            }
        }
        Column(Modifier.padding(top = 14.dp)) {
            ListRow("Cycle settings", icon = TuulitIcons.Cycle, trailingText = "${s?.cycleLength ?: 28} · ${s?.periodLength ?: 5} days", chevron = true, onClick = { go("you/cycle") })
            ListRow("Banana Mode", leading = { Mascot((s?.bananaMode ?: BananaModePersona.NEUTRAL).mood(), 24.dp) }, trailingText = s?.bananaMode?.title, chevron = true, onClick = { go("you/banana") })
            ListRow("Trusted people", icon = TuulitIcons.Users, chevron = true, onClick = { go("connect") })
            ListRow("Privacy and security", icon = TuulitIcons.Shield, chevron = true, onClick = { go("you/privacy") })
            ListRow("Notifications", icon = TuulitIcons.Bell, chevron = true, onClick = { go("you/notifications") })
            ListRow("Appearance and accessibility", icon = TuulitIcons.Eye, trailingText = when (s?.themeMode) { ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Warm dark"; else -> "System" },
                chevron = true, onClick = { go("you/appearance") })
            ListRow("Account and backup", icon = TuulitIcons.Download, trailingText = if (signed != null) "Signed in" else "Not set up", chevron = true, onClick = { go("you/account") })
            ListRow("Help and about", icon = TuulitIcons.Info, chevron = true, last = true, onClick = { go("you/help") })
        }
    }
}

@Composable
fun CycleSettingsScreen(onBack: () -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val s by c.prefs.settings.collectAsState(initial = null)
    val st = s ?: return
    var cycle by remember(st.cycleLength) { mutableIntStateOf(st.cycleLength) }
    var period by remember(st.periodLength) { mutableIntStateOf(st.periodLength) }
    var reg by remember(st.regularity) { mutableStateOf(st.regularity) }
    var name by remember(st.displayName) { mutableStateOf(st.displayName) }
    ScreenColumn {
        AppBar("Cycle settings", onBack)
        T("Used until Tuulit has enough of your own cycles to learn from. Logged cycles always take priority.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(vertical = 8.dp))
        NumberRow("Typical cycle length", cycle, 15..60) { cycle = it }
        NumberRow("Typical period length", period, 1..14) { period = it }
        T("How regular are they?", Tuulit.type.subtitle, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        ChipRow {
            listOf(CycleRegularity.REGULAR to "Mostly regular", CycleRegularity.SOMEWHAT_VARIABLE to "They vary", CycleRegularity.IRREGULAR to "Unpredictable", CycleRegularity.NOT_SURE to "Not sure")
                .forEach { (r, l) -> Chip(l, reg == r, { reg = r }) }
        }
        Field("Your name", name, { name = it.take(40) }, Modifier.padding(top = 20.dp), helper = "Only shown to you and people you connect with.", error = Validation.displayName(name))
        TButton("Save", {
            scope.launch {
                c.prefs.setCycleDefaults(cycle, period, reg); c.prefs.setDisplayName(name); c.auth.updateDisplayName(name)
                shell.showToast("Saved"); onBack()
            }
        }, Modifier.fillMaxWidth().padding(top = 20.dp))
    }
}

@Composable
private fun NumberRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        T(label, Tuulit.type.subtitle, modifier = Modifier.weight(1f))
        IconBtn(TuulitIcons.Minus, "Decrease", { onChange((value - 1).coerceIn(range)) }, enabled = value > range.first)
        T("$value days", Tuulit.type.title, modifier = Modifier.padding(horizontal = 12.dp))
        IconBtn(TuulitIcons.Plus, "Increase", { onChange((value + 1).coerceIn(range)) }, enabled = value < range.last)
    }
}

@Composable
fun BananaModeScreen(onBack: () -> Unit) {
    val c = container()
    val scope = rememberCoroutineScope()
    val s by c.prefs.settings.collectAsState(initial = null)
    ScreenColumn {
        AppBar("Banana Mode", onBack)
        T("Your banana keeps you company in the mood you choose.", Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(vertical = 8.dp))
        BananaModePersona.entries.forEachIndexed { i, p ->
            ListRow(p.title, sub = p.tagline, leading = { Mascot(p.mood(), 48.dp) },
                trailing = { if (s?.bananaMode == p) Icon(TuulitIcons.Check, "Selected", tint = Tuulit.colors.ink) },
                last = i == BananaModePersona.entries.lastIndex, onClick = { scope.launch { c.prefs.setBananaMode(p) } })
        }
        val on = s?.notTheDayOn == Dates.today()
        SettingRow("Today is not the day", "A quiet view with four gentle actions until midnight", on,
            { scope.launch { c.prefs.setNotTheDay(if (it) Dates.today() else null) } }, Modifier.padding(top = 16.dp), last = true)
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit, go: (String) -> Unit) {
    val c = container()
    val ctx = LocalContext.current
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val s by c.prefs.settings.collectAsState(initial = null)
    val st = s ?: return
    var pinSheet by remember { mutableStateOf(false) }
    var wipe by remember { mutableStateOf(false) }
    ScreenColumn {
        AppBar("Privacy", onBack)
        Notice("Stored only on this phone", "Your logs live in an encrypted database on this device. Nothing leaves it unless you back up, use Ask or share with someone.",
            Modifier.padding(top = 8.dp), icon = TuulitIcons.Shield)
        VSpace(8.dp)
        SettingRow("App lock", if (st.appLockEnabled) "PIN required when opening Tuulit" else "Off", st.appLockEnabled, { on ->
            if (on) pinSheet = true else scope.launch { c.prefs.setPin(null); shell.showToast("App lock off") }
        })
        SettingRow("Discreet notifications", "No cycle words on the lock screen", st.discreetNotifications, { scope.launch { c.prefs.setDiscreet(it) } })
        SettingRow("Personal answers in Ask", "Shares a summary (cycle length, phase, top symptoms). Never notes or dates.", st.aiContextOptIn,
            { scope.launch { c.prefs.setAiContext(it) } })
        ListRow("Export my data", sub = "CSV of logs and pain entries", icon = TuulitIcons.Download, chevron = true,
            onClick = { scope.launch { shareExport(ctx, c.tracking.exportCsv()) } })
        ListRow("Trusted people", sub = "Who can see what", icon = TuulitIcons.Users, chevron = true, onClick = { go("connect") })
        ListRow("Delete all data on this phone", sub = "Asks you to confirm first", titleColor = Tuulit.colors.alert, chevron = true, last = true, onClick = { wipe = true })
        TButton("Read the privacy policy", { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.TUULIT_PRIVACY_URL))) }, Modifier.padding(top = 12.dp), kind = ButtonKind.TEXT)
    }
    if (pinSheet) PinSetupSheet { set -> pinSheet = false; if (set) shell.showToast("App lock on") }
    if (wipe) TuulitSheet({ wipe = false }) {
        T("Delete all Tuulit data?", Tuulit.type.headline)
        T("Every log, pain entry, setting and voice note on this phone will be erased. This can't be undone. A cloud backup, if you have one, is not affected; delete it in Account.",
            Tuulit.type.body, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TButton("Keep my data", { wipe = false }, Modifier.weight(1f), kind = ButtonKind.QUIET)
            TButton("Delete", { scope.launch { c.account.wipeThisDevice(); c.auth.signOut(); wipe = false } }, Modifier.weight(1f), kind = ButtonKind.DANGER)
        }
    }
}

/** Two-step PIN creation. [onDone] receives true when a PIN was saved. */
@Composable
fun PinSetupSheet(onDone: (Boolean) -> Unit) {
    val c = container()
    val scope = rememberCoroutineScope()
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    TuulitSheet({ onDone(false) }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            T(if (first == null) "Choose a 4–6 digit PIN" else "Enter it again", Tuulit.type.headline)
            T("You'll need it each time you open Tuulit. It can't be recovered, but you can always delete the app's data.",
                Tuulit.type.caption, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp))
            PinDots(pin.length, maxOf(4, pin.length), Modifier.padding(vertical = 18.dp))
            error?.let { T(it, Tuulit.type.bodySm, Tuulit.colors.alert, Modifier.padding(bottom = 8.dp)) }
            Keypad(onDigit = { d -> if (pin.length < 6) pin += d; error = null }, onDelete = { pin = pin.dropLast(1) }, onSubmit = {
                val f = first
                if (f == null) {
                    val v = Validation.pin(pin)
                    if (v != null) error = v else { first = pin; pin = "" }
                } else if (f == pin) {
                    scope.launch { c.prefs.setPin(pin); c.unlocked.value = true; onDone(true) }
                } else { error = "Those didn't match. Start again."; first = null; pin = "" }
            })
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val c = container()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by c.prefs.settings.collectAsState(initial = null)
    val st = s ?: return
    var allowed by remember { mutableStateOf(Notifications.canPost(ctx)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    fun hm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
    ScreenColumn {
        AppBar("Notifications", onBack)
        if (!allowed) Notice("Notifications are off for Tuulit", "Allow them so reminders can reach you.", Modifier.padding(top = 8.dp), action = {
            TButton("Allow notifications", {
                if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else ctx.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName))
            }, kind = ButtonKind.TEXT)
        })
        Panel(Modifier.padding(top = 8.dp), padding = 16.dp) {
            T("Lock screen preview", Tuulit.type.caption, Tuulit.colors.inkMuted, Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Mascot(MascotMood.WARM, 28.dp)
                Column(Modifier.weight(1f)) {
                    T("Tuulit", Tuulit.type.label)
                    T(if (st.discreetNotifications) "Take a quick moment for yourself." else "Your period may start in ${st.periodLeadDays} days", Tuulit.type.bodySm)
                }
            }
        }
        VSpace(8.dp)
        SettingRow("Period coming up", "${st.periodLeadDays} days before · at check-in time", st.notifyPeriod, { scope.launch { c.prefs.setNotifyPeriod(it, st.periodLeadDays) } })
        if (st.notifyPeriod) ChipRow(Modifier.padding(vertical = 8.dp)) {
            (1..3).forEach { d -> Chip("$d ${if (d == 1) "day" else "days"} before", st.periodLeadDays == d, { scope.launch { c.prefs.setNotifyPeriod(true, d) } }) }
        }
        SettingRow("Evening check-in", "${hm(st.checkInMinuteOfDay)} · only if you haven't logged · skipped days are fine", st.notifyCheckIn,
            { scope.launch { c.prefs.setNotifyCheckIn(it, st.checkInMinuteOfDay) } })
        if (st.notifyCheckIn) ChipRow(Modifier.padding(vertical = 8.dp)) {
            listOf(18 * 60, 20 * 60, 21 * 60 + 30).forEach { m -> Chip(hm(m), st.checkInMinuteOfDay == m, { scope.launch { c.prefs.setNotifyCheckIn(true, m) } }) }
        }
        SettingRow("Discreet notifications", "No cycle words on the lock screen", st.discreetNotifications, { scope.launch { c.prefs.setDiscreet(it) } })
        T("Quiet hours", Tuulit.type.subtitle, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        ChipRow {
            listOf(Triple("22:00–08:00", 22 * 60, 8 * 60), Triple("23:00–07:00", 23 * 60, 7 * 60), Triple("21:00–09:00", 21 * 60, 9 * 60), Triple("Off", 0, 0))
                .forEach { (l, a, b) -> Chip(l, st.quietStartMinute == a && st.quietEndMinute == b, { scope.launch { c.prefs.setQuietHours(a, b) } }) }
        }
        T("Tuulit never sends streak or “you missed a day” alerts.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 16.dp))
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val c = container()
    val scope = rememberCoroutineScope()
    val s by c.prefs.settings.collectAsState(initial = null)
    val st = s ?: return
    ScreenColumn {
        AppBar("Appearance", onBack)
        T("Theme", Tuulit.type.subtitle, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        app.tuulit.ui.designsystem.Segmented(listOf("System", "Light", "Warm dark"), st.themeMode.ordinal, { i -> scope.launch { c.prefs.setTheme(ThemeMode.entries[i]) } })
        VSpace(12.dp)
        SettingRow("Reduced motion", "Cross-fades only; the breathing guide becomes a counted text", st.reducedMotion, { scope.launch { c.prefs.setReducedMotion(it) } }, last = true)
        T("Text size follows your phone's font size setting.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 12.dp))
    }
}

@Composable
fun AccountScreen(onBack: () -> Unit, go: (String) -> Unit) {
    val c = container()
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    val auth by c.auth.state.collectAsState(initial = c.auth.current)
    val s by c.prefs.settings.collectAsState(initial = null)
    var syncing by remember { mutableStateOf(false) }
    var deleteSheet by remember { mutableStateOf(false) }
    ScreenColumn {
        AppBar("Account and backup", onBack)
        when (val a = auth) {
            AuthState.Unavailable -> Notice("Accounts aren't available in this build", app.tuulit.data.remote.Firebase.notConfiguredMessage, Modifier.padding(top = 8.dp))
            AuthState.SignedOut -> {
                T("Back up your data", Tuulit.type.display, modifier = Modifier.padding(top = 8.dp))
                T("An account lets you restore Tuulit on a new phone and use Ask and Connect. Tuulit works fully without one.",
                    Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 20.dp))
                TButton("Create an account", { go("auth/register") }, Modifier.fillMaxWidth())
                TButton("Sign in", { go("auth/signin") }, Modifier.fillMaxWidth().padding(top = 8.dp), kind = ButtonKind.TEXT)
            }
            is AuthState.SignedIn -> {
                ListRow(a.email, sub = if (a.emailVerified) "Email confirmed" else "Email not confirmed yet", icon = TuulitIcons.Mail)
                VerifyEmailNotice(Modifier.padding(vertical = 8.dp))
                ListRow("Back up now", sub = s?.lastSyncAt?.takeIf { it > 0 }?.let { "Last backup ${Dates.day(Dates.epochDayOfMillis(it))}" } ?: "Not backed up yet",
                    icon = TuulitIcons.Refresh, trailing = { if (syncing) T("…", Tuulit.type.label) }, onClick = {
                        syncing = true
                        scope.launch {
                            val r = c.sync.sync()
                            syncing = false
                            shell.showToast(when (r) { is SyncResult.Done -> "Backed up"; is SyncResult.Failed -> r.message; SyncResult.Skipped -> "Nothing to back up" })
                        }
                    })
                ListRow("Sign out", sub = "Your data stays on this phone", icon = TuulitIcons.User, onClick = { c.auth.signOut(); shell.showToast("Signed out") })
                ListRow("Delete account", sub = "Removes your account, cloud backup and connections", titleColor = Tuulit.colors.alert, last = true, onClick = { deleteSheet = true })
                T("Backups are stored in Firestore under your account only. Only you can read them.", Tuulit.type.caption, Tuulit.colors.inkFaint, Modifier.padding(top = 12.dp))
            }
        }
    }
    if (deleteSheet) {
        var password by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        TuulitSheet({ deleteSheet = false }) {
            T("Delete your account?", Tuulit.type.headline)
            T("This removes your account, cloud backup, voice-note backups, invites and connections, and erases data on this phone. It can't be undone.",
                Tuulit.type.body, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 16.dp))
            Field("Confirm your password", password, { password = it }, keyboard = KeyboardOptions(keyboardType = KeyboardType.Password),
                visual = PasswordVisualTransformation(), error = error)
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TButton("Keep my account", { deleteSheet = false }, Modifier.weight(1f), kind = ButtonKind.QUIET)
                TButton("Delete", {
                    busy = true
                    scope.launch {
                        when (val r = c.account.deleteAccount(password)) {
                            AuthResult.Ok -> { deleteSheet = false; shell.showToast("Account deleted") }
                            is AuthResult.Failed -> error = r.message
                        }
                        busy = false
                    }
                }, Modifier.weight(1f), kind = ButtonKind.DANGER, loading = busy, enabled = password.isNotEmpty())
            }
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    ScreenColumn {
        AppBar("Help and about", onBack)
        Mascot(MascotMood.WARM, 72.dp, Modifier.padding(top = 8.dp))
        T("Tuulit ${BuildConfig.VERSION_NAME}", Tuulit.type.headline, modifier = Modifier.padding(top = 10.dp))
        T("A menstrual health and wellness companion. Predictions are estimates and can't be used as contraception. Tuulit gives general information, never a diagnosis.",
            Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 12.dp))
        GroupTitle("How predictions work")
        T("Tuulit averages your recent cycles, giving more weight to the latest, and widens the window when your cycles vary. The fertile window is estimated as the days around 14 days before your next predicted period.",
            Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(bottom = 12.dp))
        GroupTitle("When to get help")
        T("If you have severe pain, very heavy bleeding, fever, fainting, or thoughts of harming yourself, contact a clinician or emergency services.",
            Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(bottom = 12.dp))
        ListRow("Privacy policy", icon = TuulitIcons.Shield, chevron = true, onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.TUULIT_PRIVACY_URL))) })
        ListRow("Fonts", sub = "Fraunces and Plus Jakarta Sans, SIL Open Font License 1.1", icon = TuulitIcons.Book, last = true)
    }
}

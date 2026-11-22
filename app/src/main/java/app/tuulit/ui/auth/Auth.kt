package app.tuulit.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.tuulit.data.remote.AuthResult
import app.tuulit.data.remote.AuthState
import app.tuulit.domain.Validation
import app.tuulit.ui.FixedScreen
import app.tuulit.ui.VSpace
import app.tuulit.ui.container
import app.tuulit.ui.designsystem.AppBar
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Field
import app.tuulit.ui.designsystem.IconBtn
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotMood
import app.tuulit.ui.designsystem.Notice
import app.tuulit.ui.designsystem.NoticeKind
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, REGISTER, FORGOT }

/** Sign in, create account and password reset. Firebase Authentication (email and password). */
@Composable
fun AuthScreen(startMode: AuthMode, onDone: () -> Unit, onClose: () -> Unit) {
    val c = container()
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(startMode) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var pwErr by remember { mutableStateOf<String?>(null) }

    fun submit() {
        error = null; info = null
        emailErr = Validation.email(email)
        pwErr = when (mode) { AuthMode.REGISTER -> Validation.password(password); AuthMode.SIGN_IN -> if (password.isEmpty()) "Enter your password." else null; else -> null }
        if (emailErr != null || pwErr != null) return
        busy = true
        scope.launch {
            val r = when (mode) {
                AuthMode.SIGN_IN -> c.auth.signIn(email, password)
                AuthMode.REGISTER -> c.auth.register(email, password, name).also { if (it is AuthResult.Ok && name.isNotBlank()) c.prefs.setDisplayName(name) }
                AuthMode.FORGOT -> c.auth.sendPasswordReset(email)
            }
            busy = false
            when {
                r is AuthResult.Failed -> error = r.message
                mode == AuthMode.FORGOT -> { info = "If an account exists for $email, a reset link is on its way."; mode = AuthMode.SIGN_IN }
                else -> onDone()
            }
        }
    }

    FixedScreen {
        AppBar(onBack = onClose, closeIcon = true)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Mascot(if (mode == AuthMode.REGISTER) MascotMood.HAPPY else MascotMood.WARM, 72.dp, Modifier.padding(top = 4.dp))
            T(when (mode) { AuthMode.SIGN_IN -> "Welcome back"; AuthMode.REGISTER -> "Back up your data"; AuthMode.FORGOT -> "Reset your password" },
                Tuulit.type.headline, modifier = Modifier.padding(top = 12.dp))
            T(when (mode) {
                AuthMode.SIGN_IN -> "Sign in to restore your backup, use Ask and connect with your person."
                AuthMode.REGISTER -> "An account lets you restore Tuulit on a new phone, use Ask and Connect. It's optional: Tuulit works fully without one."
                AuthMode.FORGOT -> "Enter your email and we'll send a link to choose a new password."
            }, Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 8.dp, bottom = 20.dp))
            if (!c.firebase.isConfigured) Notice("Accounts aren't available yet", app.tuulit.data.remote.Firebase.notConfiguredMessage, Modifier.padding(bottom = 16.dp))
            if (mode == AuthMode.REGISTER) {
                Field("Name (optional)", name, { name = it.take(40) }, placeholder = "First name or nickname", helper = "Only shown to you and people you connect with.",
                    keyboard = KeyboardOptions(imeAction = ImeAction.Next))
                VSpace(14.dp)
            }
            Field("Email", email, { email = it.trim(); emailErr = null }, placeholder = "you@example.com", error = emailErr,
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = if (mode == AuthMode.FORGOT) ImeAction.Done else ImeAction.Next))
            if (mode != AuthMode.FORGOT) {
                VSpace(14.dp)
                Field("Password", password, { password = it; pwErr = null }, placeholder = if (mode == AuthMode.REGISTER) "At least 10 characters, with a number" else "",
                    error = pwErr, keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    visual = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = { IconBtn(TuulitIcons.Eye, if (showPw) "Hide password" else "Show password", { showPw = !showPw }) })
            }
            error?.let { Notice("That didn't work", it, Modifier.padding(top = 12.dp), kind = NoticeKind.MEDICAL) }
            info?.let { Notice("Check your inbox", it, Modifier.padding(top = 12.dp)) }
        }
        TButton(when (mode) { AuthMode.SIGN_IN -> "Sign in"; AuthMode.REGISTER -> "Create account"; AuthMode.FORGOT -> "Send reset link" },
            ::submit, Modifier.fillMaxWidth(), loading = busy, enabled = c.firebase.isConfigured)
        when (mode) {
            AuthMode.SIGN_IN -> {
                TButton("Forgot password?", { mode = AuthMode.FORGOT; error = null }, Modifier.fillMaxWidth().padding(top = 4.dp), kind = ButtonKind.TEXT)
                TButton("Create an account instead", { mode = AuthMode.REGISTER; error = null }, Modifier.fillMaxWidth(), kind = ButtonKind.TEXT)
            }
            AuthMode.REGISTER -> TButton("Sign in instead", { mode = AuthMode.SIGN_IN; error = null }, Modifier.fillMaxWidth().padding(top = 4.dp), kind = ButtonKind.TEXT)
            AuthMode.FORGOT -> TButton("Back to sign in", { mode = AuthMode.SIGN_IN; error = null }, Modifier.fillMaxWidth().padding(top = 4.dp), kind = ButtonKind.TEXT)
        }
    }
}

/** Shown in You → Account while the email address is unverified. Polls until verified. */
@Composable
fun VerifyEmailNotice(modifier: Modifier = Modifier) {
    val c = container()
    val scope = rememberCoroutineScope()
    val state by c.auth.state.collectAsState(initial = c.auth.current)
    val s = state as? AuthState.SignedIn ?: return
    if (s.emailVerified) return
    var sent by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(s.uid) { while (true) { delay(15_000); runCatching { c.auth.refresh() } } }
    Notice("Confirm your email", msg ?: "We sent a link to ${s.email}. Confirming keeps your account recoverable.", modifier, action = {
        TButton(if (sent) "Sent" else "Send again", {
            scope.launch {
                val r = c.auth.sendVerification()
                sent = r is AuthResult.Ok
                msg = (r as? AuthResult.Failed)?.message
            }
        }, kind = ButtonKind.TEXT, enabled = !sent)
    })
}

/** App lock (PIN). The PIN is checked against a PBKDF2 hash; it is never stored. */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val c = container()
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(0) }
    var lockedUntil by remember { mutableStateOf(0L) }
    fun check(p: String) {
        scope.launch {
            if (c.prefs.verifyPin(p)) onUnlocked()
            else {
                wrong++; pin = ""
                if (wrong % 5 == 0) lockedUntil = System.currentTimeMillis() + 30_000
            }
        }
    }
    FixedScreen {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(MascotMood.WARM, 56.dp, Modifier.padding(top = 56.dp))
            T("Enter your PIN", Tuulit.type.headline, modifier = Modifier.padding(top = 16.dp))
            PinDots(pin.length, 4.coerceAtLeast(pin.length), Modifier.padding(top = 20.dp))
            if (wrong > 0) T(if (System.currentTimeMillis() < lockedUntil) "Too many tries. Wait 30 seconds." else "That PIN didn't match.",
                Tuulit.type.bodySm, Tuulit.colors.alert, Modifier.padding(top = 12.dp))
        }
        Keypad(
            onDigit = { d ->
                if (System.currentTimeMillis() < lockedUntil) return@Keypad
                if (pin.length < 6) pin += d
                if (pin.length >= 4) scope.launch { if (c.prefs.verifyPin(pin)) onUnlocked() else if (pin.length == 6) check(pin) }
            },
            onDelete = { pin = pin.dropLast(1) },
            onSubmit = { if (pin.length >= 4) check(pin) },
        )
    }
}

@Composable
fun PinDots(filled: Int, total: Int, modifier: Modifier = Modifier) {
    val col = Tuulit.colors
    Row(modifier.semantics { contentDescription = "$filled digits entered" }, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.size(14.dp).clip(CircleShape)
                    .then(if (i < filled) Modifier.background(col.ink) else Modifier.border(1.5.dp, col.inkFaint, CircleShape))
            )
        }
    }
}

@Composable
fun Keypad(onDigit: (String) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "OK", "0", "DEL")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        keys.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { k ->
                    when (k) {
                        "OK" -> Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { TButton("Enter", onSubmit, kind = ButtonKind.TEXT) }
                        "DEL" -> Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { TButton("Delete", onDelete, kind = ButtonKind.TEXT) }
                        else -> KeyButton(k) { onDigit(k) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(72.dp).clip(CircleShape).background(Tuulit.colors.sand).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(label, Tuulit.type.headline) }
}

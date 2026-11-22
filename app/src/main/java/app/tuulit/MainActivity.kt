package app.tuulit

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tuulit.ui.LocalShell
import app.tuulit.ui.ShellSheet
import app.tuulit.ui.ShellState
import app.tuulit.ui.Tab
import app.tuulit.ui.TabBar
import app.tuulit.ui.ToastHost
import app.tuulit.ui.auth.AuthMode
import app.tuulit.ui.auth.AuthScreen
import app.tuulit.ui.auth.LockScreen
import app.tuulit.ui.designsystem.Mascot
import app.tuulit.ui.designsystem.MascotMood
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitTheme
import app.tuulit.ui.log.LogComposerSheet
import app.tuulit.ui.nav.TuulitNavGraph
import app.tuulit.ui.nav.isTabRoute
import app.tuulit.ui.onboarding.OnboardingFlow
import app.tuulit.ui.today.BananaModeSheet
import app.tuulit.ui.today.NeedSomethingSheet
import app.tuulit.ui.today.SosSheet
import app.tuulit.domain.Dates

class MainActivity : ComponentActivity() {
    private var pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute.value = intent?.getStringExtra(EXTRA_ROUTE)
        val container = (application as TuulitApp).container
        ProcessLifecycleOwner.get().lifecycle.addObserver(AutoLock(container))
        setContent {
            val snapshot by container.cycleState.snapshot.collectAsState()
            val unlocked by container.unlocked.collectAsState()
            val s = snapshot.settings
            // With app lock on, keep screens out of the recent-apps thumbnail and screenshots.
            LaunchedEffect(s.appLockEnabled) {
                if (s.appLockEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            TuulitTheme(s.themeMode, s.reducedMotion) {
                val dark = app.tuulit.ui.designsystem.Tuulit.colors.isDark
                androidx.compose.runtime.SideEffect {
                    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
                    }
                }
                when {
                    !snapshot.loaded -> Splash()
                    s.appLockEnabled && !unlocked -> LockScreen(onUnlocked = { container.unlocked.value = true })
                    !s.onboardingCompleted -> OnboardingGate()
                    else -> MainShell(pendingRoute.value) { pendingRoute.value = null }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    /** Re-locks the app after it has been in the background for more than a minute. */
    private class AutoLock(private val c: AppContainer) : DefaultLifecycleObserver {
        private var backgroundedAt = 0L
        override fun onStop(owner: LifecycleOwner) { backgroundedAt = System.currentTimeMillis() }
        override fun onStart(owner: LifecycleOwner) {
            if (backgroundedAt > 0 && System.currentTimeMillis() - backgroundedAt > 60_000) c.unlocked.value = false
        }
    }

    companion object { const val EXTRA_ROUTE = "route" }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(Tuulit.colors.cream), contentAlignment = Alignment.Center) { Mascot(MascotMood.WARM, 96.dp) }
}

@Composable
private fun OnboardingGate() {
    var signingIn by remember { mutableStateOf(false) }
    if (signingIn) AuthScreen(AuthMode.SIGN_IN, onDone = { signingIn = false }, onClose = { signingIn = false })
    else OnboardingFlow(onSignIn = { signingIn = true })
}

@Composable
private fun MainShell(pendingRoute: String?, consumeRoute: () -> Unit) {
    val nav = rememberNavController()
    val shell = remember { ShellState() }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) { navigateTo(nav, pendingRoute, shell); consumeRoute() }
    }
    CompositionLocalProvider(LocalShell provides shell) {
        Box(Modifier.fillMaxSize().background(Tuulit.colors.cream)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { TuulitNavGraph(nav) }
                if (isTabRoute(route)) {
                    TabBar(
                        current = Tab.entries.firstOrNull { route?.startsWith(it.route) == true }?.route,
                        onTab = { t -> nav.navigate(t.route) { popUpTo(Tab.TODAY.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        onLog = { shell.openLog(Dates.today()) },
                    )
                }
            }
            ToastHost(shell, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp))
        }
        shell.logDay?.let { day -> LogComposerSheet(day, onDismiss = { shell.logDay = null }) }
        when (shell.sheet) {
            ShellSheet.NEED_SOMETHING -> NeedSomethingSheet(nav) { shell.sheet = null }
            ShellSheet.SOS -> SosSheet(nav) { shell.sheet = null }
            ShellSheet.BANANA_MODE -> BananaModeSheet { shell.sheet = null }
            null -> Unit
        }
    }
}

fun navigateTo(nav: NavHostController, route: String, shell: ShellState) {
    when (route) {
        "log" -> shell.openLog(Dates.today())
        "today", "cycle", "care", "ask" -> nav.navigate(route) { popUpTo("today") { saveState = true }; launchSingleTop = true; restoreState = true }
        else -> nav.navigate(route)
    }
}

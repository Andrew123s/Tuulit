package app.tuulit.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.tuulit.ui.ask.AskHome
import app.tuulit.ui.ask.AskChat
import app.tuulit.ui.auth.AuthMode
import app.tuulit.ui.auth.AuthScreen
import app.tuulit.ui.care.CareScreen
import app.tuulit.ui.care.SessionScreen
import app.tuulit.ui.connect.AcceptInviteScreen
import app.tuulit.ui.connect.ConnectScreen
import app.tuulit.ui.connect.InviteScreen
import app.tuulit.ui.connect.PartnerScreen
import app.tuulit.ui.connect.PersonScreen
import app.tuulit.ui.cycle.CycleScreen
import app.tuulit.ui.cycle.HistoryScreen
import app.tuulit.ui.cycle.InsightsScreen
import app.tuulit.ui.cycle.PainJournalScreen
import app.tuulit.ui.play.GameScreen
import app.tuulit.ui.play.QuizScreen
import app.tuulit.ui.today.TodayScreen
import app.tuulit.ui.you.AccountScreen
import app.tuulit.ui.you.AppearanceScreen
import app.tuulit.ui.you.BananaModeScreen
import app.tuulit.ui.you.CycleSettingsScreen
import app.tuulit.ui.you.HelpScreen
import app.tuulit.ui.you.NotificationsScreen
import app.tuulit.ui.you.PrivacyScreen
import app.tuulit.ui.you.YouScreen

private val tabRoutes = setOf("today", "cycle", "care", "care/{segment}", "ask")
fun isTabRoute(route: String?) = route == null || route in tabRoutes

@Composable
fun TuulitNavGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    val go: (String) -> Unit = { nav.navigate(it) }
    NavHost(nav, startDestination = "today",
        enterTransition = { fadeIn(tween(200)) }, exitTransition = { fadeOut(tween(200)) }) {
        composable("today") { TodayScreen(go) }
        composable("cycle") { CycleScreen(go) }
        composable("care") { CareScreen(0, go) }
        composable("care/{segment}") { e -> CareScreen(when (e.arguments?.getString("segment")) { "play" -> 1; "library" -> 2; else -> 0 }, go) }
        composable("ask") { AskHome(go) }

        composable("insights") { InsightsScreen(back) }
        composable("pain") { PainJournalScreen(back, go) }
        composable("history") { HistoryScreen(back) }
        composable("session/{id}") { e -> SessionScreen(e.arguments?.getString("id").orEmpty(), back) }
        composable("game/{id}") { e -> GameScreen(e.arguments?.getString("id").orEmpty(), back) }
        composable("quiz/{id}") { e -> QuizScreen(e.arguments?.getString("id").orEmpty(), back, go) }
        composable("ask/chat/{tool}?topic={topic}", arguments = listOf(navArgument("topic") { type = NavType.StringType; nullable = true; defaultValue = null })) { e ->
            AskChat(e.arguments?.getString("tool") ?: "chat", e.arguments?.getString("topic"), back, go)
        }

        composable("you") { YouScreen(back, go) }
        composable("you/cycle") { CycleSettingsScreen(back) }
        composable("you/banana") { BananaModeScreen(back) }
        composable("you/privacy") { PrivacyScreen(back, go) }
        composable("you/notifications") { NotificationsScreen(back) }
        composable("you/appearance") { AppearanceScreen(back) }
        composable("you/account") { AccountScreen(back, go) }
        composable("you/help") { HelpScreen(back) }
        composable("auth/{mode}") { e ->
            val mode = when (e.arguments?.getString("mode")) { "register" -> AuthMode.REGISTER; "forgot" -> AuthMode.FORGOT; else -> AuthMode.SIGN_IN }
            AuthScreen(mode, onDone = back, onClose = back)
        }

        composable("connect") { ConnectScreen(back, go) }
        composable("connect/invite") { InviteScreen(back) }
        composable("connect/accept") { AcceptInviteScreen(back, go) }
        composable("connect/person/{id}") { e -> PersonScreen(e.arguments?.getString("id").orEmpty(), back) }
        composable("partner/{id}") { e -> PartnerScreen(e.arguments?.getString("id").orEmpty(), back, go) }
    }
}

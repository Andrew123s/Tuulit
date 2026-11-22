package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.TuulitDatabase
import com.example.data.TuulitRepository
import com.example.data.UserPreferencesRepository
import com.example.ui.TuulitViewModel
import com.example.ui.screens.AiScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CareScreen
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PainJournalScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.PrivateModeLockScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.TuulitTheme
import com.example.ui.theme.TuulitThemeTokens

private enum class TuulitTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    CALENDAR("calendar", "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    CARE("care", "Care", Icons.Filled.Spa, Icons.Outlined.Spa),
    PLAY("play", "Play", Icons.Filled.Games, Icons.Outlined.Games),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
}

private object TuulitRoutes {
    const val AI = "tuulit_ai"
    const val INSIGHTS = "insights"
    const val PAIN_JOURNAL = "pain_journal"
    const val CONNECT = "connect"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = TuulitDatabase.getInstance(applicationContext)
        val prefs = UserPreferencesRepository(applicationContext)
        val repository = TuulitRepository(dao = db.tuulitDao(), preferences = prefs)

        setContent {
            val viewModel: TuulitViewModel = viewModel(factory = TuulitViewModel.Factory(repository))
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val aiState by viewModel.aiState.collectAsStateWithLifecycle()
            val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

            TuulitTheme(darkTheme = uiState.prefs.darkThemeEnabled) {
                if (!uiState.isReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else if (uiState.prefs.privateModePin.isNotBlank() && !isUnlocked) {
                    PrivateModeLockScreen(onSubmitPin = { viewModel.unlockWithPin(it) })
                } else if (!uiState.prefs.onboardingCompleted) {
                    OnboardingScreen(
                        onComplete = { name, lastStart, cycleLen, periodDur, regularity, notif, aiOptIn ->
                            viewModel.completeOnboarding(name, lastStart, cycleLen, periodDur, regularity, notif, aiOptIn)
                        }
                    )
                } else {
                    TuulitMainShell(
                        viewModel = viewModel,
                        uiState = uiState,
                        aiState = aiState,
                        statusBanner = statusBanner
                    )
                }
            }
        }
    }
}

@Composable
private fun TuulitMainShell(
    viewModel: TuulitViewModel,
    uiState: com.example.ui.TuulitUiState,
    aiState: com.example.ui.AiUiState,
    statusBanner: String?,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: TuulitTab.HOME.route
    val isTopLevelTab = TuulitTab.entries.any { it.route == currentRoute }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        if (!statusBanner.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusBanner)
            viewModel.clearBanner()
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val extended = TuulitThemeTokens.extended

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (isTopLevelTab && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        TuulitTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                    indicatorColor = extended.softYellow,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelTab && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TuulitTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                    indicatorColor = extended.softYellow,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = TuulitTab.HOME.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(TuulitTab.HOME.route) {
                        HomeScreen(
                            uiState = uiState,
                            onLogPeriodStart = { viewModel.logPeriodStart(it) },
                            onLogPeriodEnd = { viewModel.logPeriodEnd(it) },
                            onQuickSaveTodayLog = { viewModel.saveDailyLog(it) },
                            onSetBananaMode = { viewModel.setBananaMode(it) },
                            onSetTodayNotTheDay = { viewModel.setTodayIsNotTheDay(it) },
                            onOpenAi = { navController.navigate(TuulitRoutes.AI) },
                            onOpenCalendar = { navigateToTab(TuulitTab.CALENDAR.route) },
                            onOpenCare = { navigateToTab(TuulitTab.CARE.route) },
                            onOpenPlay = { navigateToTab(TuulitTab.PLAY.route) },
                            onOpenInsights = { navController.navigate(TuulitRoutes.INSIGHTS) },
                            onOpenPainJournal = { navController.navigate(TuulitRoutes.PAIN_JOURNAL) },
                            onOpenConnect = { navController.navigate(TuulitRoutes.CONNECT) }
                        )
                    }
                    composable(TuulitTab.CALENDAR.route) {
                        CalendarScreen(
                            uiState = uiState,
                            onLogPeriodStart = { viewModel.logPeriodStart(it) },
                            onLogPeriodEnd = { viewModel.logPeriodEnd(it) },
                            onSaveDailyLog = { viewModel.saveDailyLog(it) },
                            onDeleteDailyLog = { viewModel.deleteDailyLog(it) },
                            onAddCustomMood = { viewModel.addCustomMood(it) },
                            onAddCustomSymptom = { viewModel.addCustomSymptom(it) }
                        )
                    }
                    composable(TuulitTab.CARE.route) {
                        CareScreen(
                            uiState = uiState,
                            onRelaxationCompleted = { viewModel.markRelaxationCompleted() },
                            onSaveSleepNote = { viewModel.saveSleepRoutineNote(it) },
                            onAddSurvivalItem = { t, c, d -> viewModel.addSurvivalKitItem(t, c, d) },
                            onDeleteSurvivalItem = { viewModel.deleteSurvivalKitItem(it) },
                            onToggleChecklistItem = { viewModel.toggleChecklistItem(it) },
                            onAddChecklistItem = { viewModel.addChecklistItem(it) },
                            onDeleteChecklistItem = { viewModel.deleteChecklistItem(it) },
                            onOpenPlay = { navigateToTab(TuulitTab.PLAY.route) },
                            onOpenPainJournal = { navController.navigate(TuulitRoutes.PAIN_JOURNAL) }
                        )
                    }
                    composable(TuulitTab.PLAY.route) {
                        PlayScreen(
                            uiState = uiState,
                            onRecordGameScore = { id, score -> viewModel.recordGameScore(id, score) },
                            onQuizCompleted = { viewModel.markQuizCompleted() }
                        )
                    }
                    composable(TuulitTab.PROFILE.route) {
                        ProfileScreen(
                            uiState = uiState,
                            onUpdateProfile = { n, cl, pd, reg -> viewModel.updateProfilePreferences(n, cl, pd, reg) },
                            onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                            onToggleReducedMotion = { viewModel.setReducedMotion(it) },
                            onToggleAiContext = { viewModel.setAiContextOptIn(it) },
                            onUpdateNotifications = { en, p, ch, care, hide, q ->
                                viewModel.updateNotifications(en, p, ch, care, hide, q)
                            },
                            onSetPrivatePin = { viewModel.setPrivateModePin(it) },
                            onExportCsv = { viewModel.buildExportCsv() },
                            onWipeAllData = { viewModel.wipeAllData() },
                            onOpenInsights = { navController.navigate(TuulitRoutes.INSIGHTS) },
                            onOpenPainJournal = { navController.navigate(TuulitRoutes.PAIN_JOURNAL) },
                            onOpenConnect = { navController.navigate(TuulitRoutes.CONNECT) }
                        )
                    }
                    composable(TuulitRoutes.AI) {
                        AiScreen(
                            uiState = uiState,
                            aiState = aiState,
                            onToggleAiContext = { viewModel.setAiContextOptIn(it) },
                            onSendChat = { viewModel.sendAiMessage(it) },
                            onExplainBody = { t, style -> viewModel.explainBody(t, style) },
                            onCheckMyth = { viewModel.checkHealthMyth(it) },
                            onGenerateAppointmentPrep = { viewModel.generateAppointmentPrep(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(TuulitRoutes.INSIGHTS) {
                        InsightsScreen(
                            uiState = uiState,
                            onInsightsViewed = { viewModel.markInsightsExplored() },
                            onExportCsv = { viewModel.buildExportCsv() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(TuulitRoutes.PAIN_JOURNAL) {
                        PainJournalScreen(
                            uiState = uiState,
                            onSavePainLog = { viewModel.savePainLog(it) },
                            onDeletePainLog = { viewModel.deletePainLog(it) },
                            onExportCsv = { viewModel.buildExportCsv() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(TuulitRoutes.CONNECT) {
                        ConnectScreen(
                            uiState = uiState,
                            onSaveContact = { viewModel.saveTrustedContact(it) },
                            onDeleteContact = { viewModel.deleteTrustedContact(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

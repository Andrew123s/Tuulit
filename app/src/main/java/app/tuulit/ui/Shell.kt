package app.tuulit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tuulit.AppContainer
import app.tuulit.TuulitApp
import app.tuulit.ui.designsystem.T
import app.tuulit.ui.designsystem.Toast
import app.tuulit.ui.designsystem.Tuulit
import app.tuulit.ui.designsystem.TuulitIcons
import kotlinx.coroutines.delay

/** Access to the app container from composables. */
@Composable
fun container(): AppContainer = (LocalContext.current.applicationContext as TuulitApp).container

/** Creates a ViewModel with access to the container. */
@Composable
inline fun <reified VM : ViewModel> tuulitViewModel(key: String? = null, crossinline make: (AppContainer) -> VM): VM {
    val c = container()
    return viewModel(key = key, factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = make(c) as T
    })
}

/** App-wide UI actions: the log composer, toasts and the main sheets live above the nav graph. */
@Stable
class ShellState {
    var logDay by mutableStateOf<Long?>(null)
    var toast by mutableStateOf<ToastMsg?>(null)
    var sheet by mutableStateOf<ShellSheet?>(null)

    fun openLog(day: Long) { logDay = day }
    fun showToast(text: String, undo: (() -> Unit)? = null) { toast = ToastMsg(text, undo, System.nanoTime()) }
}

data class ToastMsg(val text: String, val undo: (() -> Unit)?, val id: Long)
enum class ShellSheet { NEED_SOMETHING, SOS, BANANA_MODE }

val LocalShell = staticCompositionLocalOf { ShellState() }

@Composable
fun ToastHost(shell: ShellState, modifier: Modifier = Modifier) {
    val msg = shell.toast
    LaunchedEffect(msg?.id) { if (msg != null) { delay(4_000); if (shell.toast?.id == msg.id) shell.toast = null } }
    AnimatedVisibility(msg != null, modifier, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
        val m = msg ?: return@AnimatedVisibility
        Toast(m.text, if (m.undo != null) "Undo" else null, m.undo?.let { u -> { u(); shell.toast = null } },
            Modifier.semantics { contentDescription = m.text })
    }
}

/** Scrollable tab/pushed screen body with the 20dp gutter and max width from the design. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, bottomPad: Dp = 32.dp, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Tuulit.colors.cream), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = bottomPad),
            content = content,
        )
    }
}

/** Fixed (non-scrolling) full-height screen, used for onboarding steps, games and sessions. */
@Composable
fun FixedScreen(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(Tuulit.colors.cream), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxSize().statusBarsPadding().windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            content = content,
        )
    }
}

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", TuulitIcons.Today),
    CYCLE("cycle", "Cycle", TuulitIcons.Cycle),
    CARE("care", "Care", TuulitIcons.Care),
    ASK("ask", "Ask", TuulitIcons.Ask),
}

/**
 * Floating tab bar: an ivory pill that sits above the page, with a soft tinted pill behind the
 * active tab and the banana Log button as Tuulit's signature centre action.
 */
@Composable
fun TabBar(current: String?, onTab: (Tab) -> Unit, onLog: () -> Unit) {
    val c = Tuulit.colors
    Box(Modifier.fillMaxWidth().background(c.cream).windowInsetsPadding(WindowInsets.navigationBars)
        .padding(start = 14.dp, end = 14.dp, bottom = 10.dp, top = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().height(68.dp).shadow(14.dp, androidx.compose.foundation.shape.RoundedCornerShape(34.dp), ambientColor = c.period, spotColor = c.period)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(34.dp)).background(c.oat).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tabs = Tab.entries
            tabs.take(2).forEach { TabItem(it, current == it.route) { onTab(it) } }
            Box(Modifier.weight(1f))
            tabs.drop(2).forEach { TabItem(it, current == it.route) { onTab(it) } }
        }
        LogButton(onLog, Modifier.align(Alignment.TopCenter).offset(y = (-18).dp))
    }
}

@Composable
private fun LogButton(onLog: () -> Unit, modifier: Modifier) {
    val c = Tuulit.colors
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.9f else 1f, label = "log")
    val turn by androidx.compose.animation.core.animateFloatAsState(if (pressed) 90f else 0f, label = "turn")
    Box(
        modifier.size(66.dp).scale(scale).shadow(12.dp, CircleShape, ambientColor = c.banana, spotColor = c.banana).clip(CircleShape)
            .background(c.oat).padding(5.dp).clip(CircleShape).background(c.banana)
            .clickable(interaction, null, role = Role.Button, onClickLabel = "Log today", onClick = onLog)
            .semantics { contentDescription = "Log" },
        contentAlignment = Alignment.Center,
    ) { Icon(TuulitIcons.Plus, null, Modifier.size(28.dp).rotate(turn), tint = c.onBanana) }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(tab: Tab, on: Boolean, onClick: () -> Unit) {
    val c = Tuulit.colors
    val bg by androidx.compose.animation.animateColorAsState(if (on) c.peachSoft else c.oat, label = "tab")
    Column(
        Modifier.weight(1f).padding(horizontal = 2.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(bg)
            .clickable(role = Role.Tab, onClick = onClick).semantics { selected = on }.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(tab.icon, null, Modifier.size(24.dp), tint = if (on) c.periodDeep else c.inkMuted)
        T(tab.label, Tuulit.type.tab, if (on) c.ink else c.inkMuted)
    }
}

/** Bottom sheet styled to the design: oat ground, 28dp top corners, tan grab handle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuulitSheet(onDismiss: () -> Unit, skipPartial: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val c = Tuulit.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartial)
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = state, containerColor = c.oat, scrimColor = c.scrim,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 10.dp, bottom = 14.dp).size(36.dp, 4.dp).clip(CircleShape).background(c.tan)) },
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 28.dp), content = content)
    }
}

val NoPadding = PaddingValues(0.dp)

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

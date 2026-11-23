package app.tuulit.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------- Text helpers ----------

@Composable
fun T(text: String, style: TextStyle, color: Color = Tuulit.colors.ink, modifier: Modifier = Modifier, align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE) =
    Text(text, modifier, color = color, style = style, textAlign = align, maxLines = maxLines)

@Composable
fun Overline(text: String, color: Color = Tuulit.colors.inkMuted, modifier: Modifier = Modifier) =
    T(text.uppercase(), Tuulit.type.overline, color, modifier.semantics { heading() })

@Composable
fun ScreenHeader(kicker: String?, title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 14.dp)) {
        if (kicker != null) Overline(kicker)
        T(title, Tuulit.type.display, modifier = Modifier.padding(top = 4.dp).semantics { heading() })
        if (sub != null) T(sub, Tuulit.type.bodySm, Tuulit.colors.inkMuted, Modifier.padding(top = 6.dp))
    }
}

// ---------- Buttons ----------

enum class ButtonKind { PRIMARY, BANANA, QUIET, TEXT, DANGER }

@Composable
fun TButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    small: Boolean = false,
) {
    val c = Tuulit.colors
    val (bg, fg) = when (kind) {
        ButtonKind.PRIMARY -> c.ink to c.onInk
        ButtonKind.BANANA -> c.banana to c.onBanana
        ButtonKind.QUIET -> c.sand to c.ink
        ButtonKind.TEXT -> Color.Transparent to c.toffee
        ButtonKind.DANGER -> c.alert to c.onInk
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "press")
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.38f)
            .scale(scale)
            .defaultMinSize(minHeight = if (small) 36.dp else if (kind == ButtonKind.TEXT) 40.dp else 48.dp)
            .then(if (focused) Modifier.border(2.dp, c.focus, RoundedCornerShape(Radius.pill)) else Modifier)
            .clip(RoundedCornerShape(Radius.pill))
            .background(bg)
            .clickable(interaction, null, enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (kind == ButtonKind.TEXT) 8.dp else if (small) 16.dp else 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp)
        } else {
            if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = fg); Spacer(Modifier.width(8.dp)) }
            T(text, Tuulit.type.label, fg, maxLines = 1)
        }
    }
}

@Composable
fun IconBtn(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false, enabled: Boolean = true) {
    val c = Tuulit.colors
    Box(
        modifier.alpha(if (enabled) 1f else 0.38f).size(44.dp).clip(CircleShape).background(if (filled) c.ink else c.sand)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(20.dp), tint = if (filled) c.onInk else c.ink) }
}

// ---------- Chips & segmented ----------

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, outline: Boolean = false) {
    val c = Tuulit.colors
    val bg by animateColorAsState(if (selected) c.peach else if (outline) Color.Transparent else c.sand, label = "chip")
    Row(
        modifier.height(36.dp).clip(RoundedCornerShape(Radius.pill)).background(bg)
            .then(if (outline) Modifier.border(1.5.dp, c.inkFaint, RoundedCornerShape(Radius.pill)) else Modifier)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(16.dp), tint = c.ink); Spacer(Modifier.width(6.dp)) }
        else if (selected) { Icon(TuulitIcons.Check, null, Modifier.size(16.dp), tint = c.ink); Spacer(Modifier.width(6.dp)) }
        T(text, Tuulit.type.label, c.ink, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Tuulit.colors
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.pill)).background(c.sand).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(Radius.pill)).background(if (on) c.oat else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) }.semantics { stateDescription = if (on) "Selected" else "" },
                contentAlignment = Alignment.Center,
            ) { T(o, Tuulit.type.label, if (on) c.ink else c.inkMuted, maxLines = 1) }
        }
    }
}

// ---------- Fields ----------

@Composable
fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helper: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    actions: KeyboardActions = KeyboardActions.Default,
    visual: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    bare: Boolean = false,
) {
    val c = Tuulit.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier) {
        if (label.isNotEmpty()) T(label, Tuulit.type.bodySm, c.inkMuted, Modifier.padding(bottom = 6.dp))
        val shape = RoundedCornerShape(Radius.sm)
        Row(
            Modifier.fillMaxWidth().heightIn(min = if (singleLine) 52.dp else 96.dp).clip(shape)
                .background(if (bare) androidx.compose.ui.graphics.Color.Transparent else if (focused) c.oat else c.sand)
                .then(
                    when {
                        error != null -> Modifier.border(2.dp, c.alert, shape)
                        focused && !bare -> Modifier.border(2.dp, c.focus, shape)
                        else -> Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = if (singleLine) 0.dp else 14.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) T(placeholder, Tuulit.type.body, c.inkFaint)
                BasicTextField(
                    value, onValueChange, Modifier.fillMaxWidth().semantics { contentDescription = label.ifEmpty { placeholder } },
                    textStyle = Tuulit.type.body.copy(color = c.ink), singleLine = singleLine, minLines = minLines,
                    keyboardOptions = keyboard, keyboardActions = actions, visualTransformation = visual,
                    interactionSource = interaction, cursorBrush = SolidColor(c.ink),
                )
            }
            if (trailing != null) trailing()
        }
        when {
            error != null -> Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(TuulitIcons.Alert, null, Modifier.size(14.dp), tint = c.alert); Spacer(Modifier.width(6.dp))
                T(error, Tuulit.type.caption, c.alert)
            }
            helper != null -> T(helper, Tuulit.type.caption, c.inkFaint, Modifier.padding(top = 6.dp))
        }
    }
}

// ---------- Toggle & rows ----------

@Composable
fun Toggle(on: Boolean, modifier: Modifier = Modifier) {
    val c = Tuulit.colors
    val x by animateFloatAsState(if (on) 20f else 0f, label = "knob")
    Box(
        modifier.size(52.dp, 32.dp).clip(RoundedCornerShape(Radius.pill)).background(if (on) c.period else c.sand)
            .then(if (on) Modifier else Modifier.border(1.5.dp, c.inkFaint, RoundedCornerShape(Radius.pill))),
    ) {
        Box(Modifier.offset(x = (5 + x).dp, y = 5.dp).size(22.dp).clip(CircleShape).background(if (on) c.oat else c.inkMuted))
    }
}

@Composable
fun SettingRow(title: String, sub: String?, on: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier, last: Boolean = false, enabled: Boolean = true) {
    ListRow(
        title = title, sub = sub, last = last, modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        onClick = if (enabled) ({ onToggle(!on) }) else null, role = Role.Switch, state = if (on) "On" else "Off",
        trailing = { Toggle(on) },
    )
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    icon: ImageVector? = null,
    trailingText: String? = null,
    chevron: Boolean = false,
    last: Boolean = false,
    titleColor: Color = Tuulit.colors.ink,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    state: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Tuulit.colors
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(role = role, onClick = onClick) else Modifier)
                .then(if (state != null) Modifier.semantics { stateDescription = state } else Modifier)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leading != null) leading()
            else if (icon != null) {
                val tiles = listOf(c.peachSoft, c.fertileSoft, c.bananaSoft, c.roseSoft, c.periodSoft)
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(tiles[Math.floorMod(title.hashCode(), tiles.size)]),
                    contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = c.ink) }
            }
            Column(Modifier.weight(1f)) {
                T(title, Tuulit.type.subtitle.copy(fontSize = Tuulit.type.body.fontSize), titleColor)
                if (sub != null) T(sub, Tuulit.type.bodySm, c.inkMuted)
            }
            if (trailingText != null) T(trailingText, Tuulit.type.bodySm.copy(fontWeight = Tuulit.type.caption.fontWeight), c.inkFaint)
            if (trailing != null) trailing()
            if (chevron) Icon(TuulitIcons.ChevR, null, Modifier.size(18.dp), tint = c.inkFaint)
        }
        if (!last) Hairline()
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(Tuulit.colors.hairline))

@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier) = Overline(text, modifier = modifier.padding(bottom = 4.dp))

// ---------- Surfaces ----------

@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.lg)).background(Tuulit.colors.oat).padding(padding), content = content)

@Composable
fun BananaField(title: String, sub: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tuulit.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.lg)).background(c.bananaSoft)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) { T(title, Tuulit.type.title); T(sub, Tuulit.type.bodySm, c.inkMuted) }
        Icon(TuulitIcons.ChevR, null, tint = c.ink)
    }
}

enum class NoticeKind { MEDICAL, INFO }

@Composable
fun Notice(title: String, body: String, modifier: Modifier = Modifier, kind: NoticeKind = NoticeKind.INFO, icon: ImageVector? = null, action: (@Composable () -> Unit)? = null) {
    val c = Tuulit.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(if (kind == NoticeKind.MEDICAL) c.alertSoft else c.sand)
            .padding(16.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon ?: if (kind == NoticeKind.MEDICAL) TuulitIcons.Alert else TuulitIcons.Info, null, Modifier.size(20.dp),
            tint = if (kind == NoticeKind.MEDICAL) c.alert else c.toffee)
        Column(Modifier.weight(1f)) {
            T(title, Tuulit.type.bodySm.copy(fontWeight = Tuulit.type.label.fontWeight))
            T(body, Tuulit.type.bodySm)
            if (action != null) { Spacer(Modifier.height(6.dp)); action() }
        }
    }
}

@Composable
fun Tile(title: String, sub: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Tuulit.colors
    Column(
        modifier.heightIn(min = 112.dp).clip(RoundedCornerShape(Radius.md)).background(c.sand)
            .clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = c.toffee)
        T(title, Tuulit.type.title.copy(fontSize = Tuulit.type.subtitle.fontSize, lineHeight = Tuulit.type.subtitle.lineHeight))
        Spacer(Modifier.weight(1f, fill = false))
        T(sub, Tuulit.type.caption, c.inkMuted)
    }
}

@Composable
fun TileGrid(items: List<@Composable (Modifier) -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { it(Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun Avatar(name: String, size: Dp = 40.dp, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = Tuulit.colors
    Box(
        modifier.size(size).clip(CircleShape).background(c.peachSoft)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Open You", onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val n = name.trim()
        if (n.isEmpty() || n == "You") Icon(TuulitIcons.User, null, Modifier.size(size * 0.5f), tint = c.toffee)
        else T(n.take(1).uppercase(), Tuulit.type.label.copy(fontSize = Tuulit.type.subtitle.fontSize * (size.value / 40f) * 0.9f))
    }
}

@Composable
fun Progress(steps: Int, done: Int, modifier: Modifier = Modifier) {
    val c = Tuulit.colors
    Row(modifier.semantics { contentDescription = "Step $done of $steps" }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(steps) { i -> Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i < done) c.period else c.sand)) }
    }
}

@Composable
fun AppBar(title: String = "", onBack: (() -> Unit)? = null, closeIcon: Boolean = false, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconBtn(if (closeIcon) TuulitIcons.Close else TuulitIcons.Back, if (closeIcon) "Close" else "Back", onBack)
        else Spacer(Modifier.width(44.dp))
        T(title, Tuulit.type.subtitle, modifier = Modifier.weight(1f).semantics { heading() }, align = TextAlign.Center, maxLines = 1)
        if (action != null) Row(content = action) else Spacer(Modifier.width(44.dp))
    }
}

@Composable
fun Skeleton(modifier: Modifier) {
    val c = Tuulit.colors
    val alpha = if (Tuulit.reducedMotion) 1f else rememberInfiniteTransition(label = "skel")
        .animateFloat(1f, 0.5f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a").value
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(c.sand.copy(alpha = alpha)))
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, mood: MascotMood? = MascotMood.CHILL, icon: ImageVector? = null, action: (@Composable () -> Unit)? = null) {
    val c = Tuulit.colors
    Column(modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, null, Modifier.size(44.dp), tint = c.inkMuted) else if (mood != null) Mascot(mood, 88.dp)
        T(title, Tuulit.type.title, align = TextAlign.Center)
        T(body, Tuulit.type.bodySm, c.inkMuted, align = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(4.dp)); action() }
    }
}

@Composable
fun Toast(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val c = Tuulit.colors
    Row(
        modifier.clip(RoundedCornerShape(Radius.pill)).background(c.ink).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(TuulitIcons.Check, null, Modifier.size(18.dp), tint = c.onInk)
        T(text, Tuulit.type.label, c.onInk)
        if (actionLabel != null && onAction != null) {
            T(actionLabel, Tuulit.type.label.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), c.onInk,
                Modifier.clickable(role = Role.Button, onClick = onAction).padding(start = 4.dp))
        }
    }
}

@Composable
fun Bordered(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier.border(BorderStroke(1.dp, Tuulit.colors.hairline), RoundedCornerShape(Radius.md))) { content() }

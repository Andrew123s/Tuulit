package app.tuulit.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tuulit.domain.CyclePrediction
import app.tuulit.domain.Dates
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** The state of one day, derived from records and the prediction. Shape differs per state, not just colour. */
data class DayMarks(
    val period: Boolean = false,
    val predicted: Boolean = false,
    val fertile: Boolean = false,
    val ovulation: Boolean = false,
    val logged: Boolean = false,
    val today: Boolean = false,
) {
    fun describe(): String = listOfNotNull(
        "today".takeIf { today }, "period".takeIf { period }, "predicted period".takeIf { predicted && !period },
        "estimated ovulation".takeIf { ovulation }, "fertile window estimate".takeIf { fertile && !ovulation }, "has a log".takeIf { logged },
    ).joinToString(", ")
}

fun marksFor(day: Long, p: CyclePrediction, loggedDays: Set<Long>, today: Long) = DayMarks(
    period = day in p.loggedPeriodDays,
    predicted = day in p.predictedPeriodDays,
    fertile = day in p.estimatedFertileDays || day == p.estimatedOvulationDay,
    ovulation = day == p.estimatedOvulationDay,
    logged = day in loggedDays,
    today = day == today,
)

/** The signature cycle ribbon: one pill per day of the current cycle and the days just ahead. */
@Composable
fun CycleRibbon(days: List<Long>, marks: (Long) -> DayMarks, label: (Long) -> String, onDay: (Long) -> Unit, modifier: Modifier = Modifier) {
    val c = Tuulit.colors
    val state = rememberLazyListState()
    val todayIndex = days.indexOfFirst { marks(it).today }
    LaunchedEffect(todayIndex) { if (todayIndex > 3) state.scrollToItem((todayIndex - 5).coerceAtLeast(0)) }
    LazyRow(modifier.fillMaxWidth().height(100.dp), state = state, horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
        itemsIndexed(days) { _, d ->
            val m = marks(d)
            Column(
                Modifier.width(26.dp).clickable(role = Role.Button) { onDay(d) }
                    .semantics(mergeDescendants = true) { contentDescription = "${Dates.day(d)}, ${m.describe().ifEmpty { "no marks" }}" },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val fill = when { m.today -> c.banana; m.period -> c.period; m.ovulation -> c.fertileSoft; m.predicted -> Color.Transparent; else -> c.sand }
                Box(Modifier.height(56.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.size(22.dp, if (m.today) 56.dp else 44.dp).clip(RoundedCornerShape(Radius.pill)).background(fill)
                            .then(if (m.predicted && !m.period && !m.today) Modifier.border(1.5.dp, c.period, RoundedCornerShape(Radius.pill)) else Modifier),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (m.fertile && !m.today) Box(Modifier.padding(bottom = 8.dp).size(8.dp).clip(CircleShape).background(c.fertile))
                    }
                }
                T(label(d), Tuulit.type.caption.copy(fontWeight = if (m.today) FontWeight.Bold else FontWeight.Medium),
                    if (m.today) c.ink else c.inkFaint, maxLines = 1)
            }
        }
    }
}

@Composable
fun Legend(modifier: Modifier = Modifier) {
    val c = Tuulit.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        @Composable fun item(label: String, dot: @Composable () -> Unit) = Row(verticalAlignment = Alignment.CenterVertically) { dot(); T(" $label", Tuulit.type.caption, c.inkMuted) }
        item("Period") { Box(Modifier.size(10.dp).clip(CircleShape).background(c.period)) }
        item("Predicted") { Box(Modifier.size(10.dp).clip(CircleShape).border(1.5.dp, c.period, CircleShape)) }
        item("Fertile (estimate)") { Box(Modifier.size(10.dp).clip(CircleShape).background(c.fertile)) }
        item("Today") { Box(Modifier.size(10.dp).clip(CircleShape).background(c.banana)) }
    }
}

/** Month grid. Weeks start on the locale's first day. Future days can be disabled. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthGrid(
    month: YearMonth,
    marks: (Long) -> DayMarks,
    selected: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: ((Long) -> Unit)? = null,
    disableAfter: Long? = null,
) {
    val c = Tuulit.colors
    val firstDow = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val days = (0..6).map { firstDow.plus(it.toLong()) }
    val first = month.atDay(1)
    val lead = ((first.dayOfWeek.value - firstDow.value) + 7) % 7
    val cells = (0 until lead).map { first.minusDays((lead - it).toLong()) } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val padded = cells + (0 until (7 - cells.size % 7) % 7).map { month.atEndOfMonth().plusDays((it + 1).toLong()) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            days.forEach { d: DayOfWeek ->
                T(d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), Tuulit.type.caption, c.inkFaint,
                    Modifier.weight(1f).padding(bottom = 6.dp), align = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        padded.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date: LocalDate ->
                    val day = date.toEpochDay()
                    val inMonth = date.month == month.month
                    val m = marks(day)
                    val disabled = disableAfter != null && day > disableAfter
                    val shape = RoundedCornerShape(Radius.sm)
                    val bg = when { m.today -> c.banana; m.period -> c.period; m.ovulation -> c.fertileSoft; else -> Color.Transparent }
                    val fg = when { m.today -> c.onBanana; m.period -> c.onInk; else -> c.ink }
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .then(if (selected == day) Modifier.border(2.dp, c.ink, shape).padding(3.dp) else Modifier)
                            .clip(shape).background(bg)
                            .then(if (m.predicted && !m.period && !m.today) Modifier.border(1.5.dp, c.period, shape) else Modifier)
                            .alpha(if (!inMonth || disabled) 0.4f else 1f)
                            .combinedClickable(enabled = !disabled, role = Role.Button, onClick = { onSelect(day) },
                                onLongClick = onLongPress?.let { f -> { f(day) } })
                            .semantics { contentDescription = "${Dates.long(day)}${if (m.describe().isNotEmpty()) ", " + m.describe() else ""}" },
                        contentAlignment = Alignment.Center,
                    ) {
                        T(date.dayOfMonth.toString(), Tuulit.type.body.copy(fontWeight = if (m.today) FontWeight.Bold else FontWeight.Medium), fg)
                        if (m.fertile) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(5.dp).clip(CircleShape).background(c.fertile))
                        if (m.logged) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(4.dp).clip(CircleShape).background(if (m.period) c.onInk else c.toffee))
                    }
                }
            }
        }
    }
}

@Composable
fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, nextEnabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBtn(TuulitIcons.ChevL, "Previous month", onPrev)
        T(Dates.month(month.atDay(1)), Tuulit.type.headline, modifier = Modifier.weight(1f), align = androidx.compose.ui.text.style.TextAlign.Center)
        IconBtn(TuulitIcons.ChevR, "Next month", onNext, enabled = nextEnabled)
    }
}

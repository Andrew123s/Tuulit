package app.tuulit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tuulit.ui.designsystem.ButtonKind
import app.tuulit.ui.designsystem.Chip
import app.tuulit.ui.designsystem.DayMarks
import app.tuulit.ui.designsystem.Field
import app.tuulit.ui.designsystem.MonthGrid
import app.tuulit.ui.designsystem.Segmented
import app.tuulit.ui.designsystem.TButton
import app.tuulit.ui.designsystem.TuulitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(application = android.app.Application::class, qualifiers = "w360dp-h760dp")
class DesignSystemUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun buttonClicksAndDisabledButtonDoesNot() {
        var clicks = 0
        rule.setContent {
            TuulitTheme {
                androidx.compose.foundation.layout.Column {
                    TButton("Log period", { clicks++ })
                    TButton("Continue", { clicks += 100 }, enabled = false, kind = ButtonKind.BANANA)
                }
            }
        }
        rule.onNodeWithText("Log period").performClick()
        rule.onNodeWithText("Continue").performClick()
        assertEquals(1, clicks)
    }

    @Test fun chipAndSegmentedReportSelection() {
        var on by mutableStateOf(false)
        var seg by mutableIntStateOf(0)
        rule.setContent {
            TuulitTheme {
                androidx.compose.foundation.layout.Column {
                    Chip("Calm", on, { on = !on })
                    Segmented(listOf("Comfort", "Play", "Library"), seg, { seg = it })
                }
            }
        }
        rule.onNodeWithText("Calm").performClick()
        rule.onNodeWithText("Library").performClick()
        assertTrue(on)
        assertEquals(2, seg)
    }

    @Test fun fieldShowsErrorText() {
        rule.setContent { TuulitTheme { Field("Email", "maya@", {}, error = "That doesn't look like an email address.") } }
        rule.onNodeWithText("That doesn't look like an email address.").assertIsDisplayed()
    }

    @Test fun monthGridSelectsDayAndDescribesMarks() {
        val month = YearMonth.of(2026, 11)
        val periodDay = month.atDay(3).toEpochDay()
        var selected by mutableLongStateOf(-1)
        rule.setContent {
            TuulitTheme {
                MonthGrid(month, { d -> DayMarks(period = d == periodDay) }, selected, { selected = it })
            }
        }
        rule.onNodeWithContentDescription("period", substring = true).performClick()
        assertEquals(periodDay, selected)
    }
}

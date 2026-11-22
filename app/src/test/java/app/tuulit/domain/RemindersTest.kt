package app.tuulit.domain

import app.tuulit.data.local.UserSettings
import app.tuulit.notifications.ReminderRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RemindersTest {
    @Test fun quietHoursAcrossMidnight() {
        assertTrue(ReminderRules.inQuietHours(23 * 60, 22 * 60, 8 * 60))
        assertTrue(ReminderRules.inQuietHours(7 * 60, 22 * 60, 8 * 60))
        assertFalse(ReminderRules.inQuietHours(12 * 60, 22 * 60, 8 * 60))
        assertFalse(ReminderRules.inQuietHours(12 * 60, 0, 0))
        assertTrue(ReminderRules.inQuietHours(13 * 60, 12 * 60, 14 * 60))
    }

    @Test fun nextRunIsTodayOrTomorrowAtCheckIn() {
        val s = UserSettings(checkInMinuteOfDay = 20 * 60)
        assertEquals(LocalDateTime.of(2026, 11, 22, 20, 0), ReminderRules.nextRun(LocalDateTime.of(2026, 11, 22, 9, 0), s))
        assertEquals(LocalDateTime.of(2026, 11, 23, 20, 0), ReminderRules.nextRun(LocalDateTime.of(2026, 11, 22, 21, 0), s))
    }

    @Test fun nextRunIsPushedOutOfQuietHours() {
        val s = UserSettings(checkInMinuteOfDay = 23 * 60, quietStartMinute = 22 * 60, quietEndMinute = 8 * 60)
        assertEquals(LocalDateTime.of(2026, 11, 23, 8, 0), ReminderRules.nextRun(LocalDateTime.of(2026, 11, 22, 9, 0), s))
    }

    @Test fun periodHeadsUpOnlyOnLeadDay() {
        assertTrue(ReminderRules.periodHeadsUpDue(110, 108, 2))
        assertFalse(ReminderRules.periodHeadsUpDue(110, 107, 2))
        assertFalse(ReminderRules.periodHeadsUpDue(null, 107, 2))
    }
}

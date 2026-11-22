package app.tuulit.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.tuulit.TuulitApp
import app.tuulit.data.local.UserSettings
import app.tuulit.domain.CycleCalculator
import app.tuulit.domain.Dates
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Pure scheduling rules, unit-tested in RemindersTest. */
object ReminderRules {
    fun inQuietHours(minuteOfDay: Int, start: Int, end: Int): Boolean =
        if (start == end) false else if (start < end) minuteOfDay in start until end else minuteOfDay >= start || minuteOfDay < end

    /** Next time to run: the check-in time, pushed out of quiet hours if needed. */
    fun nextRun(now: LocalDateTime, s: UserSettings): LocalDateTime {
        val at = LocalTime.of(s.checkInMinuteOfDay / 60, s.checkInMinuteOfDay % 60)
        var next = now.toLocalDate().atTime(at)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val minute = next.hour * 60 + next.minute
        if (inQuietHours(minute, s.quietStartMinute, s.quietEndMinute)) {
            next = next.toLocalDate().atTime(s.quietEndMinute / 60, s.quietEndMinute % 60).let { if (it.isAfter(next)) it else it.plusDays(1) }
        }
        return next
    }

    /** True when today is exactly [leadDays] before the predicted start. */
    fun periodHeadsUpDue(nextStart: Long?, today: Long, leadDays: Int): Boolean =
        nextStart != null && nextStart - today == leadDays.toLong()
}

object ReminderScheduler {
    private const val WORK = "tuulit_daily_reminders"

    fun reschedule(context: Context, s: UserSettings) {
        val wm = WorkManager.getInstance(context)
        if (!s.notifyPeriod && !s.notifyCheckIn) { wm.cancelUniqueWork(WORK); return }
        val now = LocalDateTime.now(Dates.clock)
        val delay = Duration.between(now, ReminderRules.nextRun(now, s)).toMillis().coerceAtLeast(60_000)
        wm.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build())
    }
}

/** Runs once a day at the check-in time, posts what's due, then schedules the next run. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as TuulitApp).container
        val s = c.prefs.current()
        if (!s.onboardingCompleted) return Result.success()
        val today = Dates.today()
        if (s.notifyPeriod) {
            val p = CycleCalculator.calculatePrediction(today, c.tracking.cycles.first(), c.tracking.dailyLogs.first(),
                s.cycleLength, s.periodLength, s.regularity)
            if (ReminderRules.periodHeadsUpDue(p.nextPeriodStartEpochDay, today, s.periodLeadDays)) {
                Notifications.post(applicationContext, Notifications.ID_PERIOD, Notifications.CH_REMINDERS,
                    "Your period may start in ${s.periodLeadDays} days", "It's an estimate. Your survival kit is in Care.",
                    s.discreetNotifications, route = "care")
            }
        }
        if (s.notifyCheckIn && c.tracking.dailyLog(today) == null) {
            Notifications.post(applicationContext, Notifications.ID_CHECKIN, Notifications.CH_REMINDERS,
                "How was today?", "One tap to log your mood. Skipped days are fine.", s.discreetNotifications, route = "today")
        }
        ReminderScheduler.reschedule(applicationContext, s)
        return Result.success()
    }
}

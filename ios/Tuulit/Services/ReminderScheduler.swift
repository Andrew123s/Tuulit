import Foundation
import UserNotifications
import TuulitCore

/// One planned local notification.
struct PlannedReminder: Equatable {
    enum Kind: String { case period, checkIn }
    let kind: Kind
    let day: Int
    let minute: Int
    let title: String
    let body: String
    var id: String { "tuulit.\(kind.rawValue).\(day)" }
}

/// Local reminders. Android runs a daily WorkManager job that decides at fire time; iOS can't run
/// code reliably at a set time, so the same rules are applied ahead of time for the next week and
/// re-planned whenever logs or settings change. Copy is discreet by default.
enum ReminderPlanner {
    static let discreetTitle = "Tuulit"
    static let discreetBody = "Take a quick moment for yourself."

    static func plan(settings s: UserSettings, prediction p: CyclePrediction, loggedDays: Set<Int>, today: Int, nowMinute: Int, horizon: Int = 7) -> [PlannedReminder] {
        guard s.onboardingCompleted else { return [] }
        var out: [PlannedReminder] = []
        let slot = ReminderRules.checkInSlot(checkInMinute: s.checkInMinuteOfDay, quietStart: s.quietStartMinute, quietEnd: s.quietEndMinute)
        func copy(_ title: String, _ body: String) -> (String, String) { s.discreetNotifications ? (discreetTitle, discreetBody) : (title, body) }

        if s.notifyPeriod, let next = p.nextPeriodStartEpochDay {
            let day = next - s.periodLeadDays + slot.dayOffset
            if day > today || (day == today && slot.minute > nowMinute) {
                let (t, b) = copy("Your period may start in \(s.periodLeadDays) days", "It's an estimate. Your survival kit is in Care.")
                out.append(PlannedReminder(kind: .period, day: day, minute: slot.minute, title: t, body: b))
            }
        }
        if s.notifyCheckIn {
            for offset in 0..<horizon {
                let base = today + offset
                let day = base + slot.dayOffset
                if loggedDays.contains(base) { continue }          // only if that day isn't logged
                if day == today && slot.minute <= nowMinute { continue }
                let (t, b) = copy("How was today?", "One tap to log your mood. Skipped days are fine.")
                out.append(PlannedReminder(kind: .checkIn, day: day, minute: slot.minute, title: t, body: b))
            }
        }
        return out
    }
}

@MainActor
final class ReminderScheduler {
    private let center = UNUserNotificationCenter.current()

    func requestPermission() async -> Bool {
        (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    func isAllowed() async -> Bool {
        let s = await center.notificationSettings()
        return s.authorizationStatus == .authorized || s.authorizationStatus == .provisional
    }

    func reschedule(_ plan: [PlannedReminder]) async {
        let pending = await center.pendingNotificationRequests().map(\.identifier).filter { $0.hasPrefix("tuulit.") }
        center.removePendingNotificationRequests(withIdentifiers: pending)
        guard await isAllowed() else { return }
        for r in plan {
            let content = UNMutableNotificationContent()
            content.title = r.title
            content.body = r.body
            content.sound = .default
            content.threadIdentifier = "reminders"
            content.userInfo = ["route": r.kind == .period ? "care" : "today"]
            let (y, m, d) = Dates.civil(r.day)
            let comps = DateComponents(calendar: Dates.calendar, timeZone: Dates.calendar.timeZone, year: y, month: m, day: d,
                                       hour: r.minute / 60, minute: r.minute % 60)
            let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
            try? await center.add(UNNotificationRequest(identifier: r.id, content: content, trigger: trigger))
        }
    }

    func cancelAll() {
        center.removeAllPendingNotificationRequests()
    }
}

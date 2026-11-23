import XCTest
import TuulitCore
@testable import Tuulit

/// App-level tests: navigation routes shared with Android, reminder planning, and the app
/// model wired to the in-memory store (no Firebase, no network).
@MainActor
final class AppTests: XCTestCase {

    func testAndroidRouteStringsResolve() {
        XCTAssertEqual(Route.parse("care/play"), .tab(.care, careSegment: 1))
        XCTAssertEqual(Route.parse("care/library"), .tab(.care, careSegment: 2))
        XCTAssertEqual(Route.parse("log"), .openLog)
        XCTAssertEqual(Route.parse("session/heat_and_ease"), .push(.session("heat_and_ease")))
        XCTAssertEqual(Route.parse("ask/chat/explain?topic=follicular"), .push(.askChat(tool: "explain", topic: "follicular")))
        XCTAssertEqual(Route.parse("ask/chat/chat?topic=Why%20am%20I%20tired%3F"), .push(.askChat(tool: "chat", topic: "Why am I tired?")))
        XCTAssertEqual(Route.parse("auth/register"), .push(.auth(.register)))
        XCTAssertEqual(Route.parse("connect/person/a_b"), .push(.connectPerson("a_b")))
        XCTAssertEqual(Route.parse("you/privacy"), .push(.youPrivacy))
        XCTAssertNil(Route.parse("nowhere"))
        // Every suggestion route the shared engine can produce must resolve.
        for r in ["care/play", "ask/chat/explain?topic=follicular", "ask/chat/explain?topic=ovulation", "log"] { XCTAssertNotNil(Route.parse(r), r) }
    }

    func testModelNavigation() {
        let m = AppModel(uiTest: true)
        m.go("care/play")
        XCTAssertEqual(m.tab, .care)
        XCTAssertEqual(m.careSegment, 1)
        m.go("session/box_breathing")
        XCTAssertEqual(m.paths[.care], [.session("box_breathing")])
        m.pop()
        XCTAssertEqual(m.paths[.care], [])
        m.openLog(Dates.today() + 5)
        XCTAssertEqual(m.logDay?.day, Dates.today(), "future days can't be logged")
    }

    func testLoggingUpdatesSnapshot() async throws {
        let m = AppModel(uiTest: true)
        XCTAssertFalse(m.snapshot.prediction.hasRecordedCycles)
        m.tracking.logPeriodStart(Dates.today())
        try await Task.sleep(nanoseconds: 200_000_000)
        XCTAssertTrue(m.snapshot.prediction.hasRecordedCycles)
        XCTAssertTrue(m.snapshot.prediction.isCurrentlyMenstruating)
        XCTAssertEqual(m.snapshot.todayLog?.flow, .MEDIUM)
    }

    func testOnboardingCompletesAndStoresPastPeriod() async throws {
        let m = AppModel(uiTest: true)
        m.tracking.addPastPeriod(start: Dates.today() - 20, typicalLength: 5, today: Dates.today())
        m.settingsStore.completeOnboarding(cycleLength: 30, periodLength: 4, regularity: .REGULAR, reminders: false, discreet: true, aiContext: false)
        try await Task.sleep(nanoseconds: 200_000_000)
        XCTAssertTrue(m.settings.onboardingCompleted)
        XCTAssertEqual(m.snapshot.cycles.count, 1)
        XCTAssertEqual(m.snapshot.prediction.currentCycleDay, 21)
    }

    func testWipeErasesEverything() async throws {
        let m = AppModel(uiTest: true)
        m.tracking.saveDailyLog(DailyLog(epochDay: Dates.today(), moodScore: 4))
        m.settingsStore.setDisplayName("Maya")
        m.wipeThisDevice()
        try await Task.sleep(nanoseconds: 200_000_000)
        XCTAssertTrue(m.snapshot.logs.isEmpty)
        XCTAssertEqual(m.settings.displayName, "")
        XCTAssertEqual(m.carePlay.comfortItems.filter { $0.kind == "kit" }.count, 4, "starter kit re-seeded")
    }

    func testReminderPlanRespectsLogsQuietHoursAndDiscretion() {
        var s = UserSettings()
        s.onboardingCompleted = true
        s.checkInMinuteOfDay = 20 * 60
        let today = 1000
        let p = CycleCalculator.calculatePrediction(todayEpochDay: today, cycles: [CycleRecord(id: "c", startEpochDay: today - 26, endEpochDay: today - 22)],
                                                    dailyLogs: [], fallbackCycleLength: 28, fallbackPeriodDuration: 5, regularity: .REGULAR)
        let plan = ReminderPlanner.plan(settings: s, prediction: p, loggedDays: [today], today: today, nowMinute: 9 * 60)
        let checkIns = plan.filter { $0.kind == .checkIn }
        XCTAssertFalse(checkIns.contains { $0.day == today }, "today is already logged")
        XCTAssertEqual(checkIns.count, 6)
        let period = plan.first { $0.kind == .period }
        XCTAssertEqual(period?.day, p.nextPeriodStartEpochDay! - 2)
        XCTAssertTrue(plan.allSatisfy { $0.title == "Tuulit" && $0.body == ReminderPlanner.discreetBody }, "discreet by default")

        s.discreetNotifications = false
        s.checkInMinuteOfDay = 23 * 60
        let late = ReminderPlanner.plan(settings: s, prediction: p, loggedDays: [], today: today, nowMinute: 9 * 60)
        XCTAssertTrue(late.filter { $0.kind == .checkIn }.allSatisfy { $0.minute == 8 * 60 }, "pushed out of quiet hours")
        XCTAssertTrue(late.contains { $0.title == "How was today?" })

        s.onboardingCompleted = false
        XCTAssertTrue(ReminderPlanner.plan(settings: s, prediction: p, loggedDays: [], today: today, nowMinute: 0).isEmpty)
    }

    func testDesignTokensMatchAndroid() {
        // Spot-check the palette against Android Theme.kt values.
        XCTAssertEqual(UIColor(Palette.light.period).hex, 0xE2725B)
        XCTAssertEqual(UIColor(Palette.light.banana).hex, 0xF7C948)
        XCTAssertEqual(UIColor(Palette.light.fertile).hex, 0x9C88C9)
        XCTAssertEqual(UIColor(Palette.dark.cream).hex, 0x1C1517)
        XCTAssertFalse(SVGPath.path(Icons.drop.parts[0].d).isEmpty)
    }
}

private extension UIColor {
    var hex: Int {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        getRed(&r, green: &g, blue: &b, alpha: &a)
        return Int((r * 255).rounded()) << 16 | Int((g * 255).rounded()) << 8 | Int((b * 255).rounded())
    }
}

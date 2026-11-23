import XCTest

/// End-to-end flows on the simulator with an in-memory store (`-uitest`), run on several iPhone
/// sizes by CI: onboarding, the four tabs, the Log sheet and a game.
final class TuulitUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-uitest"]
        app.launch()
    }

    private func finishOnboarding() {
        app.buttons["onboarding.start"].tap()
        app.buttons["I'm not sure"].tap()
        XCTAssertTrue(app.staticTexts["How long are your cycles, usually?"].waitForExistence(timeout: 5))
        app.buttons["onboarding.continue"].tap()
        XCTAssertTrue(app.staticTexts["Your data stays on this phone."].waitForExistence(timeout: 5))
        // Leave reminders off so no permission prompt interrupts the test.
        let reminders = app.switches["Gentle reminders"]
        if reminders.exists, (reminders.value as? String) == "1" { reminders.tap() }
        app.buttons["onboarding.finish"].tap()
    }

    private func element(containing text: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    func testOnboardingLeadsToToday() {
        finishOnboarding()
        XCTAssertTrue(app.staticTexts["Let's get to know your cycle."].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["today.periodStarted"].exists)
    }

    func testTabsNavigate() {
        finishOnboarding()
        app.buttons["tab.cycle"].tap()
        XCTAssertTrue(app.staticTexts["Let's begin"].waitForExistence(timeout: 5))
        app.buttons["tab.care"].tap()
        XCTAssertTrue(app.staticTexts["Find what you need"].waitForExistence(timeout: 5))
        app.buttons["tab.ask"].tap()
        XCTAssertTrue(app.staticTexts["What's on your mind?"].waitForExistence(timeout: 5))
        app.buttons["tab.today"].tap()
        XCTAssertTrue(app.buttons["today.periodStarted"].waitForExistence(timeout: 5))
    }

    func testLoggingAPeriodFillsTheRing() {
        finishOnboarding()
        app.buttons["today.periodStarted"].tap()
        XCTAssertTrue(element(containing: "Period logged").waitForExistence(timeout: 5))
        XCTAssertTrue(element(containing: "Cycle day 1").waitForExistence(timeout: 5))
    }

    func testLogSheetOpensAndSaves() {
        finishOnboarding()
        app.buttons["tab.log"].tap()
        XCTAssertTrue(app.buttons["log.done"].waitForExistence(timeout: 5))
        XCTAssertTrue(element(containing: "saves as you tap").exists)
        app.buttons["log.done"].tap()
        XCTAssertTrue(app.staticTexts["Let's get to know your cycle."].waitForExistence(timeout: 5))
    }

    func testStressSmashPlaysAndFinishes() {
        finishOnboarding()
        app.buttons["tab.care"].tap()
        app.buttons["Play"].tap()
        let game = app.buttons["game.stress_smash"]
        game.swipeUpUntilHittable(in: app)
        game.tap()
        app.buttons["Tile"].firstMatch.tap()
        app.buttons["I feel better"].tap()
        XCTAssertTrue(app.buttons["Play again"].waitForExistence(timeout: 5))
    }
}

private extension XCUIElement {
    func swipeUpUntilHittable(in app: XCUIApplication, attempts: Int = 6) {
        var n = 0
        while !isHittable && n < attempts { app.swipeUp(); n += 1 }
    }
}

import XCTest
@testable import TuulitCore

/// One-for-one ports of Android `CycleCalculatorTest` and `TodayAndSuggestionTest`: same inputs,
/// same expected numbers, so both platforms are held to identical predictions.
final class CycleCalculatorTests: XCTestCase {

    func testEmptyCyclesReturnsHonestUnknownState() {
        let p = CycleCalculator.calculatePrediction(todayEpochDay: 20_000, cycles: [], dailyLogs: [], fallbackCycleLength: 28,
                                                    fallbackPeriodDuration: 5, regularity: .NOT_SURE)
        XCTAssertFalse(p.hasRecordedCycles)
        XCTAssertEqual(p.currentPhase, .UNKNOWN)
        XCTAssertNil(p.nextPeriodStartEpochDay)
        XCTAssertTrue(p.predictedPeriodDays.isEmpty)
    }

    func testRegularCyclesComputeWeightedAverageAndNextStart() {
        let cycles = [CycleRecord(id: "c1", startEpochDay: 19_910, endEpochDay: 19_914),
                      CycleRecord(id: "c2", startEpochDay: 19_940, endEpochDay: 19_944),
                      CycleRecord(id: "c3", startEpochDay: 19_970, endEpochDay: 19_974)]
        let p = CycleCalculator.calculatePrediction(todayEpochDay: 19_980, cycles: cycles, dailyLogs: [], fallbackCycleLength: 28,
                                                    fallbackPeriodDuration: 5, regularity: .REGULAR)
        XCTAssertTrue(p.hasRecordedCycles)
        XCTAssertEqual(p.averageCycleLength, 30)
        XCTAssertEqual(p.averagePeriodDuration, 5)
        XCTAssertEqual(p.currentCycleDay, 11)
        XCTAssertEqual(p.nextPeriodStartEpochDay, 20_000)
        XCTAssertFalse(p.isIrregularEstimate)
        XCTAssertEqual(p.estimatedOvulationDay, 19_986)
        XCTAssertEqual(p.currentPhase, .FOLLICULAR)
        XCTAssertEqual(p.predictedPeriodDays, Set(20_000...20_004).union(20_030...20_034))
    }

    func testVariableCyclesFlagIrregularEstimateWithRange() {
        let cycles = [CycleRecord(id: "c1", startEpochDay: 19_900, endEpochDay: 19_904),
                      CycleRecord(id: "c2", startEpochDay: 19_924, endEpochDay: 19_928),
                      CycleRecord(id: "c3", startEpochDay: 19_960, endEpochDay: 19_964)]
        let p = CycleCalculator.calculatePrediction(todayEpochDay: 19_970, cycles: cycles, dailyLogs: [], fallbackCycleLength: 28,
                                                    fallbackPeriodDuration: 5, regularity: .SOMEWHAT_VARIABLE)
        XCTAssertTrue(p.isIrregularEstimate)
        XCTAssertEqual(p.variabilityDays, 12)
        XCTAssertNotNil(p.nextPeriodRangeMinDays)
        XCTAssertGreaterThan(p.nextPeriodRangeMaxDays!, p.nextPeriodRangeMinDays!)
        // Weighted average: (24*1 + 36*2) / 3 = 32.
        XCTAssertEqual(p.averageCycleLength, 32)
    }

    func testPainPatternFlagsRecurringHighPainAcrossCycles() {
        let cycles = [CycleRecord(id: "c1", startEpochDay: 19_910, endEpochDay: 19_914),
                      CycleRecord(id: "c2", startEpochDay: 19_940, endEpochDay: 19_944),
                      CycleRecord(id: "c3", startEpochDay: 19_970, endEpochDay: 19_974)]
        let pain = [
            PainLog(id: "p1", epochDay: 19_911, intensity: 8, location: "Lower abdomen", painType: "Cramping", durationHours: 6, medicationTaken: "Heat", impactedWorkOrStudy: true, impactedExercise: true, impactedSleep: false, notes: ""),
            PainLog(id: "p2", epochDay: 19_941, intensity: 8, location: "Lower abdomen", painType: "Cramping", durationHours: 5, medicationTaken: "Heat", impactedWorkOrStudy: true, impactedExercise: false, impactedSleep: true, notes: ""),
            PainLog(id: "p3", epochDay: 19_971, intensity: 9, location: "Lower abdomen", painType: "Cramping", durationHours: 8, medicationTaken: "Heat", impactedWorkOrStudy: true, impactedExercise: true, impactedSleep: true, notes: ""),
        ]
        let alert = CycleCalculator.detectPainPattern(cycles: cycles, painLogs: pain)
        XCTAssertNotNil(alert)
        XCTAssertTrue(alert!.hasRecurringSeverePain)
        XCTAssertEqual(alert!.consecutiveHighPainCycles, 3)
        XCTAssertEqual(alert!.impactedActivitiesCount, 3)
        XCTAssertTrue(alert!.summaryMessage.contains("8.3/10"))
    }

    func testInsightsInsufficientDataGuardPreventsFabricatedTrends() {
        let i = CycleCalculator.computeInsights(cycles: [CycleRecord(id: "c1", startEpochDay: 19_990, endEpochDay: 19_994)],
                                                dailyLogs: [DailyLog(epochDay: 19_991, moods: ["Calm"])], painLogs: [],
                                                fallbackCycleLength: 28, fallbackPeriodDuration: 5)
        XCTAssertFalse(i.hasEnoughDataForTrends)
        XCTAssertTrue(i.dataDrivenNotes.isEmpty)
        XCTAssertEqual(i.phaseMoodMap.first { $0.phase == .MENSTRUAL }?.topMoods, ["Calm"])
    }

    func testInsightsNotesAndFrequency() {
        let cycles = [0, 28, 56, 90].map { CycleRecord(id: "c\($0)", startEpochDay: 20_000 + $0, endEpochDay: 20_004 + $0) }
        let logs = (0..<8).map { DailyLog(epochDay: 20_010 + $0, symptoms: ["Bloating": .MILD], moods: ["Calm"], energyLevel: 3) }
        let i = CycleCalculator.computeInsights(cycles: cycles, dailyLogs: logs, painLogs: [], fallbackCycleLength: 28, fallbackPeriodDuration: 5)
        XCTAssertEqual(i.completedCyclesCount, 3)
        XCTAssertEqual(i.cycleVariabilityDays, 6)
        XCTAssertEqual(i.symptomFrequency.first, NamedCount("Bloating", 8))
        XCTAssertTrue(i.dataDrivenNotes.contains { $0.hasPrefix("Bloating is your most frequently logged symptom (8") })
    }

    func testLoggedFlowWithoutCycleMarksMenstruating() {
        let p = CycleCalculator.calculatePrediction(todayEpochDay: 100, cycles: [], dailyLogs: [DailyLog(epochDay: 100, flow: .LIGHT)],
                                                    fallbackCycleLength: 28, fallbackPeriodDuration: 5, regularity: .REGULAR)
        XCTAssertTrue(p.isCurrentlyMenstruating)
        XCTAssertEqual(p.currentPhase, .MENSTRUAL)
    }

    // MARK: Today hero and suggestions (Android TodayAndSuggestionTest)

    private func prediction(_ today: Int, _ starts: [Int], _ ends: [Int?]) -> CyclePrediction {
        CycleCalculator.calculatePrediction(todayEpochDay: today, cycles: starts.enumerated().map { CycleRecord(id: "c\($0.offset)", startEpochDay: $0.element, endEpochDay: ends[$0.offset]) },
                                            dailyLogs: [], fallbackCycleLength: 28, fallbackPeriodDuration: 5, regularity: .REGULAR)
    }

    func testHeroWithoutDataAsksToLog() {
        let p = CycleCalculator.calculatePrediction(todayEpochDay: 20_000, cycles: [], dailyLogs: [], fallbackCycleLength: 28, fallbackPeriodDuration: 5, regularity: .NOT_SURE)
        let h = TodayHeroBuilder.build(p, today: 20_000) { String($0) }
        XCTAssertNil(h.number)
        XCTAssertEqual(h.lead, "Let's get to know your cycle.")
    }

    func testHeroCountsDownToNextPeriod() {
        let p = prediction(20_023, [19_944, 19_972, 20_000], [19_948, 19_976, 20_004])
        let h = TodayHeroBuilder.build(p, today: 20_023) { "d\($0)" }
        XCTAssertEqual(h.lead, "Your period is likely in")
        XCTAssertEqual(h.number, "5")
        XCTAssertTrue(h.caption.contains("estimate"))
    }

    func testHeroDuringPeriodShowsPeriodDay() {
        let p = prediction(20_001, [19_944, 19_972, 20_000], [19_948, 19_976, nil])
        let h = TodayHeroBuilder.build(p, today: 20_001) { "d\($0)" }
        XCTAssertEqual(h.lead, "Your period")
        XCTAssertEqual(h.number, "2")
        XCTAssertEqual(h.unit, "nd day")
    }

    func testCrampsSuggestHeatSession() {
        let p = prediction(20_010, [20_000], [20_004])
        let s = SuggestionEngine.forToday(p, DailyLog(epochDay: 20_010, symptoms: ["Cramps": .MODERATE]), hourOfDay: 10)
        XCTAssertEqual(s.sessionId, CareContent.heatAndEase.id)
    }

    func testCareHeroFollowsLog() {
        XCTAssertEqual(CareContent.heroFor(DailyLog(epochDay: 1, sleepQuality: 1), .LUTEAL), CareContent.sleepWindDown)
        XCTAssertEqual(CareContent.heroFor(DailyLog(epochDay: 1, moods: ["Anxious"]), .FOLLICULAR), CareContent.boxBreathing)
        XCTAssertEqual(CareContent.heroFor(nil, .MENSTRUAL), CareContent.heatAndEase)
    }

    func testContentIsConsistent() {
        XCTAssertEqual(PlayContent.games.count, 8)
        XCTAssertTrue(PlayContent.words.allSatisfy { (4...7).contains($0.word.count) && $0.word == $0.word.uppercased() })
        for q in PlayContent.quizzes { for x in q.questions { XCTAssertLessThan(x.correctIndex, x.options.count) } }
        XCTAssertTrue(CareContent.all.allSatisfy { $0.totalSeconds > 0 })
        XCTAssertEqual(PlayContent.dailyPick(today: 20_415).id, PlayContent.games[20_415 % 8].id)
    }
}

final class SafetyAndValidationTests: XCTestCase {
    func testEmergencyPhrases() {
        for s in ["I fainted this morning", "sharp pain on my right side only, one-sided pain", "I'm pregnant and bleeding", "I want to end my life", "chest pain and cramps"] {
            XCTAssertEqual(MedicalSafetyGuard.assess(s).level, .EMERGENCY, s)
        }
    }

    func testUrgentPhrases() {
        for s in ["soaking through a pad every hour", "I have a fever of 39 c", "worst pain of my life", "vomiting and can't stop all day"] {
            XCTAssertEqual(MedicalSafetyGuard.assess(s).level, .URGENT, s)
        }
    }

    func testOrdinaryQuestionsAreNotFlagged() {
        for s in ["Why do I feel bloated before my period?", "Is a 31 day cycle normal?", "What is the luteal phase?", "Biology 101 question about hormones", ""] {
            XCTAssertEqual(MedicalSafetyGuard.assess(s).level, .NONE, s)
        }
    }

    func testLegacyHelper() {
        XCTAssertNotNil(MedicalSafetyGuard.inspectForUrgentWarning("feeling faint and soaking through"))
        XCTAssertNil(MedicalSafetyGuard.inspectForUrgentWarning("no period for 3 months and bleeding after sex"))
        XCTAssertNotNil(MedicalSafetyGuard.inspectForUrgentWarning("I am soaking through a pad every hour and feeling faint"))
        XCTAssertNil(MedicalSafetyGuard.inspectForUrgentWarning("Why do I feel slightly bloated before my period?"))
    }

    func testValidation() {
        XCTAssertNil(Validation.email("maya@example.com"))
        XCTAssertNotNil(Validation.email("")); XCTAssertNotNil(Validation.email("maya@")); XCTAssertNotNil(Validation.email("maya example.com"))
        XCTAssertNil(Validation.password("bananas2026"))
        XCTAssertNotNil(Validation.password("short1")); XCTAssertNotNil(Validation.password("onlyletterslong")); XCTAssertNotNil(Validation.password("1234567890"))
        XCTAssertNil(Validation.pin("4821")); XCTAssertNil(Validation.pin("482193"))
        XCTAssertNotNil(Validation.pin("482")); XCTAssertNotNil(Validation.pin("1111")); XCTAssertNotNil(Validation.pin("48a1"))
        XCTAssertNil(Validation.inviteCode("4K8P2Q")); XCTAssertNotNil(Validation.inviteCode("4K8P")); XCTAssertNotNil(Validation.inviteCode("4K8P2-"))
        XCTAssertNil(Validation.cycleLength(28)); XCTAssertNotNil(Validation.cycleLength(70))
        XCTAssertNil(Validation.periodLength(5)); XCTAssertNotNil(Validation.periodLength(0))
    }

    func testPBKDF2KnownVectors() {
        // RFC 7914 / widely published PBKDF2-HMAC-SHA256 vectors.
        func hex(_ b: [UInt8]) -> String { b.map { String(format: "%02x", $0) }.joined() }
        XCTAssertEqual(hex(SoftCrypto.pbkdf2(password: Array("password".utf8), salt: Array("salt".utf8), iterations: 1, keyLength: 32)),
                       "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b")
        XCTAssertEqual(hex(SoftCrypto.pbkdf2(password: Array("password".utf8), salt: Array("salt".utf8), iterations: 2, keyLength: 32)),
                       "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43")
        XCTAssertEqual(hex(PinHasher.pbkdf2SHA256(password: Array("password".utf8), salt: Array("salt".utf8), iterations: 2, keyLength: 32)),
                       "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43")
    }

    func testPinHashRoundTripAndNoPlaintext() {
        let saved = PinHasher.iterations
        PinHasher.iterations = 1_000
        defer { PinHasher.iterations = saved }
        let h = PinHasher.hash("4821")
        XCTAssertFalse(h.contains("4821"))
        XCTAssertTrue(PinHasher.verify("4821", h))
        XCTAssertFalse(PinHasher.verify("4822", h))
        XCTAssertFalse(PinHasher.verify("4821", "garbage"))
        XCTAssertNotEqual(PinHasher.hash("4821"), h)
    }

    func testRemindersRules() {
        XCTAssertTrue(ReminderRules.inQuietHours(minuteOfDay: 23 * 60, start: 22 * 60, end: 8 * 60))
        XCTAssertTrue(ReminderRules.inQuietHours(minuteOfDay: 7 * 60, start: 22 * 60, end: 8 * 60))
        XCTAssertFalse(ReminderRules.inQuietHours(minuteOfDay: 12 * 60, start: 22 * 60, end: 8 * 60))
        XCTAssertFalse(ReminderRules.inQuietHours(minuteOfDay: 12 * 60, start: 0, end: 0))
        XCTAssertTrue(ReminderRules.inQuietHours(minuteOfDay: 13 * 60, start: 12 * 60, end: 14 * 60))
        // Android nextRun cases.
        XCTAssertTrue(ReminderRules.nextRun(nowMinuteOfDay: 9 * 60, checkInMinute: 20 * 60, quietStart: 22 * 60, quietEnd: 8 * 60) == (0, 20 * 60))
        XCTAssertTrue(ReminderRules.nextRun(nowMinuteOfDay: 21 * 60, checkInMinute: 20 * 60, quietStart: 22 * 60, quietEnd: 8 * 60) == (1, 20 * 60))
        XCTAssertTrue(ReminderRules.nextRun(nowMinuteOfDay: 9 * 60, checkInMinute: 23 * 60, quietStart: 22 * 60, quietEnd: 8 * 60) == (1, 8 * 60))
        XCTAssertTrue(ReminderRules.periodHeadsUpDue(nextStart: 110, today: 108, leadDays: 2))
        XCTAssertFalse(ReminderRules.periodHeadsUpDue(nextStart: 110, today: 107, leadDays: 2))
        XCTAssertFalse(ReminderRules.periodHeadsUpDue(nextStart: nil, today: 107, leadDays: 2))
    }
}

final class DatesTests: XCTestCase {
    func testEpochDayMatchesJavaLocalDate() {
        // Values from java.time.LocalDate.toEpochDay().
        XCTAssertEqual(Dates.epochDay(year: 1970, month: 1, day: 1), 0)
        XCTAssertEqual(Dates.epochDay(year: 2000, month: 3, day: 1), 11_017)
        XCTAssertEqual(Dates.epochDay(year: 2026, month: 11, day: 23), 20_780)
        XCTAssertEqual(Dates.epochDay(year: 1969, month: 12, day: 31), -1)
        for d in stride(from: -800, through: 40_000, by: 37) {
            let c = Dates.civil(d)
            XCTAssertEqual(Dates.epochDay(year: c.year, month: c.month, day: c.day), d)
        }
    }

    func testLocalDateNotUTC() {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "Pacific/Auckland")!
        let saved = Dates.calendar
        Dates.calendar = cal
        defer { Dates.calendar = saved }
        // 2026-11-23 06:00 in Auckland is still 22 Nov in UTC; the local date must win.
        let date = cal.date(from: DateComponents(year: 2026, month: 11, day: 23, hour: 6))!
        XCTAssertEqual(Dates.epochDay(of: date), 20_780)
        XCTAssertEqual(Dates.iso(20_780), "2026-11-23")
        XCTAssertEqual(Dates.isoWeekdayIndex(20_780), 0) // Monday
    }

    func testYearMonth() {
        let ym = YearMonth(year: 2026, month: 12)
        XCTAssertEqual(ym.plus(months: 1), YearMonth(year: 2027, month: 1))
        XCTAssertEqual(ym.plus(months: -12), YearMonth(year: 2025, month: 12))
        XCTAssertEqual(YearMonth(year: 2028, month: 2).length, 29)
    }
}

import Foundation

/// Everything the tracking screens derive from: real records plus calculated predictions.
/// Same shape as Android's `CycleSnapshot`, so every screen agrees on today's numbers.
public struct CycleSnapshot: Equatable, Sendable {
    public var today: Int
    public var settings: UserSettings
    public var cycles: [CycleRecord]
    public var logs: [DailyLog]
    public var painLogs: [PainLog]
    public var prediction: CyclePrediction
    public var insights: InsightsData

    public init(today: Int, settings: UserSettings, cycles: [CycleRecord], logs: [DailyLog], painLogs: [PainLog]) {
        self.today = today; self.settings = settings; self.cycles = cycles; self.logs = logs; self.painLogs = painLogs
        prediction = CycleCalculator.calculatePrediction(todayEpochDay: today, cycles: cycles, dailyLogs: logs,
                                                         fallbackCycleLength: settings.cycleLength, fallbackPeriodDuration: settings.periodLength,
                                                         regularity: settings.regularity)
        insights = CycleCalculator.computeInsights(cycles: cycles, dailyLogs: logs, painLogs: painLogs,
                                                   fallbackCycleLength: settings.cycleLength, fallbackPeriodDuration: settings.periodLength)
    }

    public var todayLog: DailyLog? { logs.first { $0.epochDay == today } }
    public func log(_ day: Int) -> DailyLog? { logs.first { $0.epochDay == day } }
    public var loggedDays: Set<Int> { Set(logs.map(\.epochDay)) }

    /// Start of the cycle the ring draws (latest start on or before today).
    public var currentCycleStart: Int? { cycles.last { $0.startEpochDay <= today }?.startEpochDay }
}

/// Minimal, consented summary of the person's data for Ask (never notes, journal text or dates).
public struct AiContext: Equatable, Sendable {
    public let cycleDay: Int?
    public let phase: String
    public let avgCycleLength: Int?
    public let avgPeriodLength: Int?
    public let variabilityDays: Int?
    public let topSymptoms: [String]
    public let severePainCycles: Int

    public static func from(_ p: CyclePrediction, _ i: InsightsData) -> AiContext {
        AiContext(cycleDay: p.currentCycleDay, phase: p.currentPhase.title, avgCycleLength: i.averageCycleLength,
                  avgPeriodLength: i.averagePeriodDuration, variabilityDays: i.cycleVariabilityDays,
                  topSymptoms: i.symptomFrequency.prefix(4).map(\.name), severePainCycles: i.painPatternAlert?.consecutiveHighPainCycles ?? 0)
    }

    public var json: [String: Any] {
        ["cycleDay": cycleDay as Any? ?? NSNull(), "phase": phase, "avgCycleLength": avgCycleLength as Any? ?? NSNull(),
         "avgPeriodLength": avgPeriodLength as Any? ?? NSNull(), "variabilityDays": variabilityDays as Any? ?? NSNull(),
         "topSymptoms": topSymptoms, "severePainCycles": severePainCycles]
    }
}

/// Connect: what a trusted person may see. Everything starts off.
public struct SharePermissions: Equatable, Sendable {
    public var periodSoon = false, periodStarted = false, roughDay = false, phase = false
    public init(periodSoon: Bool = false, periodStarted: Bool = false, roughDay: Bool = false, phase: Bool = false) {
        self.periodSoon = periodSoon; self.periodStarted = periodStarted; self.roughDay = roughDay; self.phase = phase
    }
    public var map: [String: Bool] { ["periodSoon": periodSoon, "periodStarted": periodStarted, "roughDay": roughDay, "phase": phase] }
    public static func from(_ m: [String: Any]?) -> SharePermissions {
        SharePermissions(periodSoon: m?["periodSoon"] as? Bool == true, periodStarted: m?["periodStarted"] as? Bool == true,
                         roughDay: m?["roughDay"] as? Bool == true, phase: m?["phase"] as? Bool == true)
    }
}

public enum ConnectRules {
    /// Member-visible status fields, only for what the owner permitted (Android `publishStatus`).
    public static func statusFields(permissions p: SharePermissions, prediction: CyclePrediction?, today: Int) -> [String: Any] {
        var data: [String: Any] = [:]
        if p.periodStarted, let pr = prediction, pr.isCurrentlyMenstruating {
            data["periodStartedOn"] = TodayHeroBuilder.periodStart(loggedPeriodDays: Set(pr.loggedPeriodDays.filter { $0 <= today }), today: today)
        } else { data["periodStartedOn"] = NSNull() }
        if p.periodSoon, let next = prediction?.nextPeriodStartEpochDay, (0...2).contains(next - today) {
            data["periodSoonOn"] = next
        } else { data["periodSoonOn"] = NSNull() }
        data["phase"] = p.phase ? (prediction?.currentPhase.title as Any? ?? NSNull()) : NSNull()
        return data
    }

    /// Six characters from an alphabet without look-alikes (matches the Firestore rule).
    public static func newInviteCode() -> String {
        let alphabet = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")
        var g = SystemRandomNumberGenerator()
        return String((0..<6).map { _ in alphabet[Int.random(in: 0..<alphabet.count, using: &g)] })
    }
}

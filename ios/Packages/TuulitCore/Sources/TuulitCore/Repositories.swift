import Foundation

/// Cycles, daily logs and pain entries. Port of Android `TrackingRepository`: every write stamps
/// `updatedAt`, deletes become tombstones, and the cycle table follows the flow that was logged.
public final class TrackingRepository: @unchecked Sendable {
    private let store: TuulitStore
    private let onChange: () -> Void
    private let now: () -> Int64

    public init(store: TuulitStore, onChange: @escaping () -> Void = {}, now: @escaping () -> Int64 = { Dates.nowMillis() }) {
        self.store = store; self.onChange = onChange; self.now = now
    }

    public var cycles: [CycleRecord] { store.read { $0.liveCycles.map(\.domain) } }
    public var dailyLogs: [DailyLog] { store.read { $0.liveDays.map(\.domain) } }
    public var painLogs: [PainLog] { store.read { $0.livePain.map(\.domain) } }

    public func dailyLog(_ day: Int) -> DailyLog? { store.read { $0.day(day).flatMap { $0.deleted ? nil : $0.domain } } }

    /// Saves a day's log and keeps the cycle table consistent with the flow that was logged.
    public func saveDailyLog(_ log: DailyLog) {
        store.write { d in
            let previous = d.day(log.epochDay).flatMap { $0.deleted ? nil : $0.domain }
            var clean = log
            var seen = Set<String>()
            clean.moods = log.moods.map(\.trimmed).filter { !$0.isEmpty && seen.insert($0).inserted }
            clean.energyLevel = log.energyLevel.clamped(0, 5)
            clean.sleepQuality = log.sleepQuality.clamped(0, 5)
            clean.moodScore = log.moodScore.clamped(0, 5)
            clean.waterGlasses = log.waterGlasses.clamped(0, 30)
            clean.journalText = log.journalText.take(4000)
            if clean.isEmpty {
                if var existing = d.day(log.epochDay) { existing.deleted = true; existing.updatedAt = now(); d.upsertDay(existing) }
            } else {
                d.upsertDay(DayRow.from(clean, now: now()))
            }
            let wasFlow = (previous?.flow.level ?? 0) >= FlowIntensity.LIGHT.level
            let isFlow = clean.flow.level >= FlowIntensity.LIGHT.level
            if isFlow && !wasFlow { reconcileFlowAdded(&d, day: log.epochDay) }
            if !isFlow && wasFlow { reconcileFlowRemoved(&d, day: log.epochDay) }
        }
        onChange()
    }

    public func deleteDailyLog(_ day: Int) {
        let changed: Bool = store.write { d in
            guard var existing = d.day(day) else { return false }
            let hadFlow = FlowIntensity.fromName(existing.flow).level >= FlowIntensity.LIGHT.level
            existing.deleted = true; existing.updatedAt = now()
            d.upsertDay(existing)
            if hadFlow { reconcileFlowRemoved(&d, day: day) }
            return true
        }
        if changed { onChange() }
    }

    /// "My period started" from Today or onboarding. Duplicate-safe.
    public func logPeriodStart(_ day: Int) {
        let existing = dailyLog(day) ?? DailyLog(epochDay: day)
        if existing.flow.level < FlowIntensity.LIGHT.level {
            var l = existing; l.flow = .MEDIUM
            saveDailyLog(l)
        } else {
            store.write { reconcileFlowAdded(&$0, day: day) }
            onChange()
        }
    }

    /// Records a past period from onboarding without inventing any daily flow entries.
    public func addPastPeriod(start: Int, typicalLength: Int, today: Int) {
        if start > today { return }
        let added: Bool = store.write { d in
            if d.liveCycles.contains(where: { abs($0.startEpochDay - start) < 10 }) { return false }
            let end = start + typicalLength.clamped(1, 14) - 1
            let tomb = d.cycleStarting(on: start)
            d.upsertCycle(CycleRow(id: tomb?.id ?? newId(), startEpochDay: start, endEpochDay: end < today ? end : nil, updatedAt: now()))
            return true
        }
        if added { onChange() }
    }

    public func logPeriodEnd(_ day: Int) {
        let changed: Bool = store.write { d in
            guard var c = d.liveCycles.last(where: { $0.startEpochDay <= day }) else { return false }
            c.endEpochDay = max(day, c.startEpochDay); c.updatedAt = now()
            d.upsertCycle(c)
            return true
        }
        if changed { onChange() }
    }

    /// Edits a cycle from History. Returns an error message when the edit is invalid.
    public func editCycle(id: String, start: Int, end: Int?, today: Int = Dates.today()) -> String? {
        if let e = end, e < start { return "The end can't be before the start." }
        if let e = end, e - start > 14 { return "A period longer than 15 days isn't supported. Check the dates." }
        if start > today { return "The start can't be in the future." }
        let result: String? = store.write { d in
            if d.liveCycles.contains(where: { $0.id != id && abs($0.startEpochDay - start) < 10 }) {
                return "Another period starts within 10 days of that date."
            }
            guard var c = d.cycles[id] else { return "That cycle no longer exists." }
            c.startEpochDay = start; c.endEpochDay = end; c.updatedAt = now()
            d.upsertCycle(c)
            return nil
        }
        if result == nil { onChange() }
        return result
    }

    public func deleteCycle(id: String) {
        let changed: Bool = store.write { d in
            guard var c = d.cycles[id] else { return false }
            c.deleted = true; c.updatedAt = now()
            d.upsertCycle(c)
            return true
        }
        if changed { onChange() }
    }

    public func savePainLog(_ p: PainLog) {
        let row = PainRow.from(p, now: now())
        store.write { $0.pain[row.id] = row }
        onChange()
    }

    public func deletePainLog(id: String) {
        let changed: Bool = store.write { d in
            guard var e = d.pain[id] else { return false }
            e.deleted = true; e.updatedAt = now()
            d.pain[id] = e
            return true
        }
        if changed { onChange() }
    }

    // MARK: cycle reconciliation (identical rules to Android)

    private func reconcileFlowAdded(_ d: inout TuulitData, day: Int) {
        let cycles = d.liveCycles
        // Inside or right after an existing period: extend it.
        if var current = cycles.last(where: { $0.startEpochDay <= day && day - $0.startEpochDay < 12 }) {
            if let end = current.endEpochDay, day > end { current.endEpochDay = day; current.updatedAt = now(); d.upsertCycle(current) }
            return
        }
        // Logged a day or three before a period that already exists: move its start earlier.
        if var following = cycles.first(where: { ((day + 1)...(day + 3)).contains($0.startEpochDay) }) {
            following.startEpochDay = day; following.updatedAt = now()
            d.upsertCycle(following)
            return
        }
        if var tomb = d.cycleStarting(on: day) {
            tomb.deleted = false; tomb.endEpochDay = nil; tomb.updatedAt = now()
            d.upsertCycle(tomb)
        } else {
            d.upsertCycle(CycleRow(startEpochDay: day, updatedAt: now()))
        }
    }

    private func reconcileFlowRemoved(_ d: inout TuulitData, day: Int) {
        guard var cycle = d.liveCycles.last(where: { $0.startEpochDay <= day && day - $0.startEpochDay < 12 }) else { return }
        let flowDays = (cycle.startEpochDay...(cycle.startEpochDay + 11)).filter { x in
            guard x != day, let r = d.day(x), !r.deleted else { return false }
            return FlowIntensity.fromName(r.flow).level >= FlowIntensity.LIGHT.level
        }
        if flowDays.isEmpty {
            cycle.deleted = true; cycle.updatedAt = now(); d.upsertCycle(cycle)
        } else if day == cycle.startEpochDay {
            cycle.startEpochDay = flowDays.first!; cycle.updatedAt = now(); d.upsertCycle(cycle)
        } else if cycle.endEpochDay == day {
            cycle.endEpochDay = flowDays.last!; cycle.updatedAt = now(); d.upsertCycle(cycle)
        }
    }

    // MARK: export

    /// CSV of every log and pain entry, same columns and escaping as Android.
    public func exportCSV() -> String {
        var lines = ["type,date,flow,mood_score,moods,symptoms,energy,sleep,water,medication,notes,pain_intensity,pain_location,pain_type,pain_hours,impact"]
        let (days, pains) = store.read { d in
            (d.liveDays.sorted { $0.epochDay < $1.epochDay }.map(\.domain), d.livePain.sorted { $0.epochDay < $1.epochDay }.map(\.domain))
        }
        for l in days {
            lines.append([
                "day", Dates.iso(l.epochDay), l.flow.label, String(l.moodScore), l.moods.joined(separator: ";"),
                l.symptoms.keys.sorted().map { "\($0) (\(l.symptoms[$0]!.label))" }.joined(separator: ";"),
                String(l.energyLevel), String(l.sleepQuality), String(l.waterGlasses), l.medicationNote, l.journalText, "", "", "", "", "",
            ].map(TrackingRepository.csv).joined(separator: ","))
        }
        for p in pains {
            let impact = [p.impactedWorkOrStudy ? "work/study" : nil, p.impactedExercise ? "exercise" : nil, p.impactedSleep ? "sleep" : nil].compactMap { $0 }
            lines.append([
                "pain", Dates.iso(p.epochDay), "", "", "", "", "", "", "", p.medicationTaken, p.notes,
                String(p.intensity), p.location, p.painType, String(p.durationHours), impact.joined(separator: ";"),
            ].map(TrackingRepository.csv).joined(separator: ","))
        }
        return lines.joined(separator: "\n") + "\n"
    }

    static func csv(_ v: String) -> String {
        let safe = ["=", "+", "-", "@"].contains(where: { v.hasPrefix($0) }) ? "'" + v : v
        return safe.contains(where: { $0 == "," || $0 == "\"" || $0 == "\n" }) ? "\"" + safe.replacingOccurrences(of: "\"", with: "\"\"") + "\"" : safe
    }
}

public struct GameScore: Equatable, Sendable { public let gameId: String; public let best: Int; public let plays: Int }
public struct UnlockedAchievement: Equatable, Sendable { public let achievement: Achievement; public let at: Int64 }

/// Comfort Library, session completions, game scores and achievements (Android `CarePlayRepository`).
public final class CarePlayRepository: @unchecked Sendable {
    private let store: TuulitStore
    private let onChange: () -> Void
    private let now: () -> Int64

    /// Fixed IDs so a second device never duplicates the starter kit (see IOS_DATABASE_AND_SYNC.md).
    public static let seedKit = ["Pads or tampons", "Spare underwear", "Painkillers", "Wipes"]
    public static func seedId(_ i: Int) -> String { "seed-kit-\(i)" }

    public init(store: TuulitStore, onChange: @escaping () -> Void = {}, now: @escaping () -> Int64 = { Dates.nowMillis() }) {
        self.store = store; self.onChange = onChange; self.now = now
    }

    public var comfortItems: [ComfortRow] { store.read { $0.liveComfort } }
    public var gameScores: [String: GameScore] {
        store.read { d in d.games.mapValues { GameScore(gameId: $0.gameId, best: $0.bestScore, plays: $0.plays) } }
    }
    public var achievements: [UnlockedAchievement] {
        store.read { d in
            d.achievements.compactMap { code, at in Achievement(rawValue: code).map { UnlockedAchievement(achievement: $0, at: at) } }
                .sorted { $0.at > $1.at }
        }
    }

    /// First-run library: a starting bag checklist the person can edit or delete. Only seeded once.
    public func seedLibraryIfEmpty() {
        store.write { d in
            guard d.comfort.isEmpty else { return }
            for (i, t) in CarePlayRepository.seedKit.enumerated() {
                d.comfort[CarePlayRepository.seedId(i)] = ComfortRow(id: CarePlayRepository.seedId(i), kind: "kit", title: t, icon: "check",
                                                                     position: i, updatedAt: 0)
            }
        }
    }

    public func addComfortItem(kind: String, title: String, detail: String, icon: String) {
        let row = ComfortRow(kind: kind, title: title.trimmed.take(80), detail: detail.trimmed.take(160), icon: icon, updatedAt: now())
        store.write { $0.comfort[row.id] = row }
        onChange()
    }

    public func updateComfortItem(_ item: ComfortRow) {
        var i = item; i.updatedAt = now()
        store.write { $0.comfort[i.id] = i }
        onChange()
    }

    public func toggleKitItem(id: String) {
        let changed: Bool = store.write { d in
            guard var i = d.comfort[id] else { return false }
            i.checked.toggle(); i.updatedAt = now(); d.comfort[id] = i
            return true
        }
        if changed { onChange() }
    }

    public func deleteComfortItem(id: String) {
        let changed: Bool = store.write { d in
            guard var i = d.comfort[id] else { return false }
            i.deleted = true; i.updatedAt = now(); d.comfort[id] = i
            return true
        }
        if changed { onChange() }
    }

    /// Records a finished (or ended-early) session and credits the matching library item.
    @discardableResult
    public func completeSession(_ sessionId: String, fully: Bool) -> [Achievement] {
        store.write { $0.completions.append(CompletionRow(sessionId: sessionId, completedAt: now(), fullyCompleted: fully)) }
        if !fully { return [] }
        store.write { d in
            if var item = d.comfort.values.first(where: { $0.sessionId == sessionId && !$0.deleted }) {
                item.helpedCount += 1; item.updatedAt = now(); d.comfort[item.id] = item
            } else if let s = CareContent.byId(sessionId) {
                let row = ComfortRow(kind: "comfort", title: s.title, detail: s.summary, icon: s.icon, helpedCount: 1, sessionId: sessionId, updatedAt: now())
                d.comfort[row.id] = row
            }
        }
        onChange()
        var unlocked: [Achievement] = []
        if let a = unlock(.FIRST_SESSION) { unlocked.append(a) }
        let fullCount = store.read { $0.completions.filter(\.fullyCompleted).count }
        if fullCount >= 5, let a = unlock(.FIVE_SESSIONS) { unlocked.append(a) }
        return unlocked
    }

    /// Saves a finished game. Returns true when it's a new personal best.
    @discardableResult
    public func recordGame(_ gameId: String, score: Int) -> Bool {
        let prev = store.read { $0.games[gameId] }
        let best = max(prev?.bestScore ?? 0, score)
        store.write { $0.games[gameId] = GameScoreRow(gameId: gameId, bestScore: best, plays: (prev?.plays ?? 0) + 1, updatedAt: now()) }
        unlock(.FIRST_GAME)
        onChange()
        if let p = prev { return score > p.bestScore }
        return score > 0
    }

    public func bestScore(_ gameId: String) -> Int { store.read { $0.games[gameId]?.bestScore ?? 0 } }

    /// Unlocks once. Returns the achievement when it was newly unlocked.
    @discardableResult
    public func unlock(_ a: Achievement) -> Achievement? {
        let inserted: Bool = store.write { d in
            guard d.achievements[a.rawValue] == nil else { return false }
            d.achievements[a.rawValue] = now()
            return true
        }
        if inserted { onChange() }
        return inserted ? a : nil
    }
}

/// Ask history, kept on the device only.
public final class ConversationRepository: @unchecked Sendable {
    private let store: TuulitStore
    public init(store: TuulitStore) { self.store = store }

    public func conversation(_ id: String) -> [AiMessageRow] {
        store.read { $0.aiMessages.filter { $0.conversationId == id }.sorted { $0.createdAt < $1.createdAt } }
    }
    public func append(_ m: AiMessageRow) { store.write { $0.aiMessages.append(m) } }
    public func clear(_ id: String) { store.write { $0.aiMessages.removeAll { $0.conversationId == id } } }
}

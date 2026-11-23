import Foundation

// Stored rows. Every synced table carries `updatedAt` (epoch millis) and a `deleted` tombstone,
// exactly like Android's Room entities, so changes and deletions merge last-write-wins.

public func newId() -> String { UUID().uuidString.lowercased() }

public struct CycleRow: Codable, Equatable, Sendable {
    public var id: String
    public var startEpochDay: Int
    public var endEpochDay: Int?
    public var updatedAt: Int64
    public var deleted: Bool = false
    public init(id: String = newId(), startEpochDay: Int, endEpochDay: Int? = nil, updatedAt: Int64, deleted: Bool = false) {
        self.id = id; self.startEpochDay = startEpochDay; self.endEpochDay = endEpochDay; self.updatedAt = updatedAt; self.deleted = deleted
    }
    public var domain: CycleRecord { CycleRecord(id: id, startEpochDay: startEpochDay, endEpochDay: endEpochDay) }
}

public struct DayRow: Codable, Equatable, Sendable {
    public var epochDay: Int
    public var flow: String = FlowIntensity.NONE.rawValue
    /// "Cramps:2|Bloating:1"
    public var symptoms: String = ""
    /// "Calm|Tired"
    public var moods: String = ""
    public var moodScore = 0, energyLevel = 0, sleepQuality = 0, waterGlasses = 0
    public var exerciseNote = "", medicationNote = "", journalText = ""
    public var voiceNoteKey: String? = nil
    public var updatedAt: Int64
    public var deleted = false

    public init(epochDay: Int, flow: String = FlowIntensity.NONE.rawValue, symptoms: String = "", moods: String = "", moodScore: Int = 0,
                energyLevel: Int = 0, sleepQuality: Int = 0, waterGlasses: Int = 0, exerciseNote: String = "", medicationNote: String = "",
                journalText: String = "", voiceNoteKey: String? = nil, updatedAt: Int64, deleted: Bool = false) {
        self.epochDay = epochDay; self.flow = flow; self.symptoms = symptoms; self.moods = moods; self.moodScore = moodScore
        self.energyLevel = energyLevel; self.sleepQuality = sleepQuality; self.waterGlasses = waterGlasses; self.exerciseNote = exerciseNote
        self.medicationNote = medicationNote; self.journalText = journalText; self.voiceNoteKey = voiceNoteKey; self.updatedAt = updatedAt
        self.deleted = deleted
    }

    public var domain: DailyLog {
        DailyLog(epochDay: epochDay, flow: .fromName(flow), symptoms: DayRow.decodeSymptoms(symptoms),
                 moods: moods.split(separator: "|", omittingEmptySubsequences: false).map { String($0).trimmed }.filter { !$0.isEmpty },
                 moodScore: moodScore, energyLevel: energyLevel, sleepQuality: sleepQuality, waterGlasses: waterGlasses,
                 exerciseNote: exerciseNote, medicationNote: medicationNote, journalText: journalText, voiceNoteKey: voiceNoteKey,
                 updatedAtMillis: updatedAt)
    }

    public static func from(_ log: DailyLog, now: Int64) -> DayRow {
        DayRow(epochDay: log.epochDay, flow: log.flow.rawValue, symptoms: encodeSymptoms(log.symptoms),
               moods: log.moods.map { $0.replacingOccurrences(of: "|", with: " ").trimmed }.joined(separator: "|"),
               moodScore: log.moodScore, energyLevel: log.energyLevel, sleepQuality: log.sleepQuality, waterGlasses: log.waterGlasses,
               exerciseNote: log.exerciseNote, medicationNote: log.medicationNote, journalText: log.journalText,
               voiceNoteKey: log.voiceNoteKey, updatedAt: now)
    }

    /// Same format as Android `DailyLogEntity.encodeSymptoms`. Keys are sorted so the string is stable.
    public static func encodeSymptoms(_ map: [String: SymptomSeverity]) -> String {
        map.keys.sorted().map { k in
            "\(k.replacingOccurrences(of: "|", with: " ").replacingOccurrences(of: ":", with: " ").trimmed):\(map[k]!.level)"
        }.joined(separator: "|")
    }

    public static func decodeSymptoms(_ raw: String) -> [String: SymptomSeverity] {
        var out: [String: SymptomSeverity] = [:]
        for entry in raw.split(separator: "|", omittingEmptySubsequences: false) {
            let parts = entry.split(separator: ":", omittingEmptySubsequences: false)
            let name = parts.first.map { String($0).trimmed } ?? ""
            if name.isEmpty { continue }
            let level = parts.count > 1 ? Int(parts[1]) ?? 1 : 1
            out[name] = .fromLevel(level)
        }
        return out
    }
}

public struct PainRow: Codable, Equatable, Sendable {
    public var id: String
    public var epochDay: Int
    public var intensity: Int
    public var location: String, painType: String
    public var durationHours: Int
    public var medicationTaken: String
    public var impactedWorkOrStudy: Bool, impactedExercise: Bool, impactedSleep: Bool
    public var notes: String
    public var createdAt: Int64, updatedAt: Int64
    public var deleted = false

    public var domain: PainLog {
        PainLog(id: id, epochDay: epochDay, intensity: intensity, location: location, painType: painType, durationHours: durationHours,
                medicationTaken: medicationTaken, impactedWorkOrStudy: impactedWorkOrStudy, impactedExercise: impactedExercise,
                impactedSleep: impactedSleep, notes: notes, createdAtMillis: createdAt)
    }

    public static func from(_ p: PainLog, now: Int64) -> PainRow {
        PainRow(id: p.id.isEmpty ? newId() : p.id, epochDay: p.epochDay, intensity: p.intensity.clamped(1, 10), location: p.location,
                painType: p.painType, durationHours: p.durationHours.clamped(0, 72), medicationTaken: p.medicationTaken,
                impactedWorkOrStudy: p.impactedWorkOrStudy, impactedExercise: p.impactedExercise, impactedSleep: p.impactedSleep,
                notes: p.notes, createdAt: p.createdAtMillis > 0 ? p.createdAtMillis : now, updatedAt: now)
    }
}

/// Comfort Library item. kind is "comfort" (what works for me) or "kit" (period bag checklist).
public struct ComfortRow: Codable, Equatable, Sendable, Identifiable {
    public var id: String = newId()
    public var kind: String
    public var title: String
    public var detail: String = ""
    public var icon: String = "care"
    public var checked = false
    public var helpedCount = 0
    public var sessionId: String? = nil
    public var position = 0
    public var updatedAt: Int64
    public var deleted = false

    public init(id: String = newId(), kind: String, title: String, detail: String = "", icon: String = "care", checked: Bool = false,
                helpedCount: Int = 0, sessionId: String? = nil, position: Int = 0, updatedAt: Int64, deleted: Bool = false) {
        self.id = id; self.kind = kind; self.title = title; self.detail = detail; self.icon = icon; self.checked = checked
        self.helpedCount = helpedCount; self.sessionId = sessionId; self.position = position; self.updatedAt = updatedAt; self.deleted = deleted
    }
}

public struct CompletionRow: Codable, Equatable, Sendable {
    public var id: String = newId()
    public var sessionId: String
    public var completedAt: Int64
    public var fullyCompleted: Bool
}

public struct GameScoreRow: Codable, Equatable, Sendable {
    public var gameId: String
    public var bestScore: Int
    public var plays: Int
    public var updatedAt: Int64
}

/// Ask conversations stay on the device. Only the question being asked leaves it.
public struct AiMessageRow: Codable, Equatable, Sendable, Identifiable {
    public var id: String = newId()
    public var conversationId: String
    public var role: String
    public var text: String
    public var tool: String = "chat"
    public var urgency: String = UrgencyLevel.NONE.rawValue
    public var usedContext = false
    public var failed = false
    public var createdAt: Int64
    public init(id: String = newId(), conversationId: String, role: String, text: String, tool: String = "chat",
                urgency: String = UrgencyLevel.NONE.rawValue, usedContext: Bool = false, failed: Bool = false, createdAt: Int64) {
        self.id = id; self.conversationId = conversationId; self.role = role; self.text = text; self.tool = tool
        self.urgency = urgency; self.usedContext = usedContext; self.failed = failed; self.createdAt = createdAt
    }
}

/// Everything the app stores locally, in one versioned document.
public struct TuulitData: Codable, Equatable, Sendable {
    public static let currentSchema = 1
    public var schemaVersion = TuulitData.currentSchema
    public var cycles: [String: CycleRow] = [:]
    public var days: [String: DayRow] = [:]
    public var pain: [String: PainRow] = [:]
    public var comfort: [String: ComfortRow] = [:]
    public var completions: [CompletionRow] = []
    public var achievements: [String: Int64] = [:]
    public var games: [String: GameScoreRow] = [:]
    public var aiMessages: [AiMessageRow] = []
    public init() {}
}

// MARK: - Persistence backends

public protocol StoreBackend: AnyObject {
    func load() -> Data?
    func save(_ data: Data) throws
    func erase()
}

public final class MemoryBackend: StoreBackend {
    public private(set) var data: Data?
    public init(_ data: Data? = nil) { self.data = data }
    public func load() -> Data? { data }
    public func save(_ data: Data) throws { self.data = data }
    public func erase() { data = nil }
}

/// Atomic file store. On iOS the file uses complete Data Protection (encrypted with a key tied to
/// the passcode, unreadable while locked) and is excluded from iCloud/iTunes backups.
public final class FileBackend: StoreBackend {
    public let url: URL
    public init(url: URL) { self.url = url }

    public func load() -> Data? { try? Data(contentsOf: url) }

    public func save(_ data: Data) throws {
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        #if os(iOS)
        try data.write(to: url, options: [.atomic, .completeFileProtection])
        #else
        try data.write(to: url, options: [.atomic])
        #endif
        #if os(iOS) || os(macOS)
        var u = url
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try? u.setResourceValues(values)
        #endif
    }

    public func erase() { try? FileManager.default.removeItem(at: url) }
}

// MARK: - Store

/// Thread-safe local database. The source of truth on the phone; sync reads and merges into it.
public final class TuulitStore: @unchecked Sendable {
    private let backend: StoreBackend
    private let lock = NSLock()
    private var data: TuulitData
    private var observers: [UUID: () -> Void] = [:]
    private let writeQueue = DispatchQueue(label: "app.tuulit.store")

    public init(backend: StoreBackend) {
        self.backend = backend
        if let raw = backend.load(), let decoded = try? JSONDecoder().decode(TuulitData.self, from: raw) {
            data = TuulitStore.migrate(decoded)
        } else {
            data = TuulitData()
        }
    }

    public static func inMemory() -> TuulitStore { TuulitStore(backend: MemoryBackend()) }

    static func migrate(_ d: TuulitData) -> TuulitData {
        var m = d
        // Future schema changes go here, one version at a time.
        m.schemaVersion = TuulitData.currentSchema
        return m
    }

    /// Reads a consistent copy.
    public func read<T>(_ body: (TuulitData) -> T) -> T {
        lock.lock(); defer { lock.unlock() }
        return body(data)
    }

    /// Mutates, persists and notifies observers.
    @discardableResult
    public func write<T>(_ body: (inout TuulitData) -> T) -> T {
        lock.lock()
        let result = body(&data)
        let snapshot = data
        let callbacks = Array(observers.values)
        lock.unlock()
        persist(snapshot)
        callbacks.forEach { $0() }
        return result
    }

    private func persist(_ snapshot: TuulitData) {
        guard let encoded = try? JSONEncoder().encode(snapshot) else { return }
        writeQueue.async { [backend] in try? backend.save(encoded) }
    }

    /// Waits for pending disk writes (tests, app backgrounding).
    public func flush() { writeQueue.sync {} }

    @discardableResult
    public func observe(_ callback: @escaping () -> Void) -> UUID {
        let id = UUID()
        lock.lock(); observers[id] = callback; lock.unlock()
        return id
    }

    public func removeObserver(_ id: UUID) { lock.lock(); observers[id] = nil; lock.unlock() }

    /// Erases everything (Wipe this device / account deletion).
    public func wipe() {
        lock.lock(); data = TuulitData(); let callbacks = Array(observers.values); lock.unlock()
        writeQueue.sync { backend.erase() }
        callbacks.forEach { $0() }
    }
}

// MARK: - Table helpers matching the Room DAO (REPLACE semantics, unique cycle start)

extension TuulitData {
    /// Cycles that aren't deleted, oldest first (`observeCycles`).
    public var liveCycles: [CycleRow] { cycles.values.filter { !$0.deleted }.sorted { $0.startEpochDay < $1.startEpochDay } }
    public func cycleStarting(on day: Int) -> CycleRow? { cycles.values.first { $0.startEpochDay == day } }

    /// Room `@Insert(onConflict = REPLACE)` with a unique index on startEpochDay: any *other* row
    /// that already holds this start date is removed before the insert.
    public mutating func upsertCycle(_ c: CycleRow) {
        for (id, other) in cycles where id != c.id && other.startEpochDay == c.startEpochDay { cycles[id] = nil }
        cycles[c.id] = c
    }

    public func day(_ epochDay: Int) -> DayRow? { days[String(epochDay)] }
    public mutating func upsertDay(_ d: DayRow) { days[String(d.epochDay)] = d }
    /// Newest first (`observeDailyLogs`).
    public var liveDays: [DayRow] { days.values.filter { !$0.deleted }.sorted { $0.epochDay > $1.epochDay } }

    /// Newest first (`observePainLogs`: epochDay DESC, createdAt DESC).
    public var livePain: [PainRow] {
        pain.values.filter { !$0.deleted }.sorted { ($0.epochDay, $0.createdAt) > ($1.epochDay, $1.createdAt) }
    }

    /// `ORDER BY kind, position, updatedAt`.
    public var liveComfort: [ComfortRow] {
        comfort.values.filter { !$0.deleted }.sorted { ($0.kind, $0.position, $0.updatedAt) < ($1.kind, $1.position, $1.updatedAt) }
    }
}

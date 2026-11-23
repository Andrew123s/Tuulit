import Foundation

/// Reads an integer from a Firestore/JSON value, whatever numeric type it arrived as.
public func anyInt64(_ v: Any?) -> Int64? {
    switch v {
    case let x as Int64: return x
    case let x as Int: return Int64(x)
    case let x as Int32: return Int64(x)
    case let x as UInt32: return Int64(x)
    case let x as Double: return Int64(x)
    case let x as NSNumber: return x.int64Value
    default: return nil
    }
}

public enum ThemeMode: String, CaseIterable, Codable, Sendable { case SYSTEM, LIGHT, DARK }

/// Port of Android `UserSettings`, same defaults (Light theme, discreet notifications on).
public struct UserSettings: Codable, Equatable, Sendable {
    public var onboardingCompleted = false
    public var displayName = ""
    public var cycleLength = 28
    public var periodLength = 5
    public var regularity: CycleRegularity = .NOT_SURE
    public var themeMode: ThemeMode = .LIGHT
    public var reducedMotion = false
    public var bananaMode: BananaModePersona = .NEUTRAL
    /// Epoch day on which Today Is Not the Day is active; it switches off at midnight.
    public var notTheDayOn = -1
    public var aiContextOptIn = false
    public var notifyPeriod = true
    public var periodLeadDays = 2
    public var notifyCheckIn = true
    public var checkInMinuteOfDay = 20 * 60
    public var notifyCare = false
    public var discreetNotifications = true
    public var quietStartMinute = 22 * 60
    public var quietEndMinute = 8 * 60
    public var pinHash = ""
    public var customMoods: [String] = []
    public var customSymptoms: [String] = []
    public var sleepNote = ""
    public var sosMessages: [String] = UserSettings.defaultSOS
    public var lastSyncAt: Int64 = 0
    public var suggestionDismissedOn = -1
    public var accountPromptDismissed = false
    /// When the synced subset last changed on this device (for settings sync, last-write-wins).
    public var syncedSettingsUpdatedAt: Int64 = 0

    public var appLockEnabled: Bool { !pinHash.isEmpty }

    public static let defaultSOS = [
        "Rough period day. Could you check in on me?",
        "Cramps are bad. Could you bring painkillers?",
        "Could you pick up pads on your way home?",
        "I need a quiet evening. Nothing's wrong.",
    ]

    public init() {}

    // Decoding tolerates missing keys so new settings never wipe older saved ones.
    public init(from decoder: Decoder) throws {
        self.init()
        let c = try decoder.container(keyedBy: CodingKeys.self)
        func v<T: Decodable>(_ k: CodingKeys, _ d: T) -> T {
            if let value = try? c.decodeIfPresent(T.self, forKey: k) { return value }
            return d
        }
        onboardingCompleted = v(.onboardingCompleted, onboardingCompleted); displayName = v(.displayName, displayName)
        cycleLength = v(.cycleLength, cycleLength); periodLength = v(.periodLength, periodLength); regularity = v(.regularity, regularity)
        themeMode = v(.themeMode, themeMode); reducedMotion = v(.reducedMotion, reducedMotion); bananaMode = v(.bananaMode, bananaMode)
        notTheDayOn = v(.notTheDayOn, notTheDayOn); aiContextOptIn = v(.aiContextOptIn, aiContextOptIn); notifyPeriod = v(.notifyPeriod, notifyPeriod)
        periodLeadDays = v(.periodLeadDays, periodLeadDays); notifyCheckIn = v(.notifyCheckIn, notifyCheckIn)
        checkInMinuteOfDay = v(.checkInMinuteOfDay, checkInMinuteOfDay); notifyCare = v(.notifyCare, notifyCare)
        discreetNotifications = v(.discreetNotifications, discreetNotifications); quietStartMinute = v(.quietStartMinute, quietStartMinute)
        quietEndMinute = v(.quietEndMinute, quietEndMinute); pinHash = v(.pinHash, pinHash); customMoods = v(.customMoods, customMoods)
        customSymptoms = v(.customSymptoms, customSymptoms); sleepNote = v(.sleepNote, sleepNote); sosMessages = v(.sosMessages, sosMessages)
        lastSyncAt = v(.lastSyncAt, lastSyncAt); suggestionDismissedOn = v(.suggestionDismissedOn, suggestionDismissedOn)
        accountPromptDismissed = v(.accountPromptDismissed, accountPromptDismissed)
        syncedSettingsUpdatedAt = v(.syncedSettingsUpdatedAt, syncedSettingsUpdatedAt)
    }

    // MARK: cross-device settings (users/{uid}.settings)

    /// The settings that follow the person between devices. Device-only settings (theme, PIN,
    /// reminders, quiet hours, discreet mode) are deliberately excluded.
    public var syncedMap: [String: Any] {
        ["displayName": displayName, "cycleLength": cycleLength, "periodLength": periodLength, "regularity": regularity.rawValue,
         "bananaMode": bananaMode.rawValue, "customMoods": customMoods, "customSymptoms": customSymptoms,
         "sosMessages": sosMessages, "sleepNote": sleepNote, "aiContextOptIn": aiContextOptIn, "updatedAt": syncedSettingsUpdatedAt]
    }

    /// Applies a remote settings map when it is newer than the local copy. Returns true if applied.
    @discardableResult
    public mutating func applyRemote(_ m: [String: Any]) -> Bool {
        let remoteAt = anyInt64(m["updatedAt"]) ?? 0
        guard remoteAt > syncedSettingsUpdatedAt else { return false }
        if let s = m["displayName"] as? String { displayName = s.take(40) }
        if let n = anyInt64(m["cycleLength"]).map { Int($0) } { cycleLength = n.clamped(15, 60) }
        if let n = anyInt64(m["periodLength"]).map { Int($0) } { periodLength = n.clamped(1, 14) }
        if let s = m["regularity"] as? String { regularity = .fromName(s) }
        if let s = m["bananaMode"] as? String { bananaMode = .fromName(s) }
        if let a = m["customMoods"] as? [String] { customMoods = a }
        if let a = m["customSymptoms"] as? [String] { customSymptoms = a }
        if let a = m["sosMessages"] as? [String], !a.isEmpty { sosMessages = a }
        if let s = m["sleepNote"] as? String { sleepNote = s.take(500) }
        if let b = m["aiContextOptIn"] as? Bool { aiContextOptIn = b }
        syncedSettingsUpdatedAt = remoteAt
        return true
    }
}

/// Settings persistence. The app uses `UserDefaultsSettingsStore`; tests use the in-memory one.
public protocol SettingsStore: AnyObject {
    var current: UserSettings { get }
    func update(_ change: (inout UserSettings) -> Void)
    func observe(_ callback: @escaping (UserSettings) -> Void) -> UUID
    func clear()
}

open class MemorySettingsStore: SettingsStore {
    public private(set) var current: UserSettings
    private var observers: [UUID: (UserSettings) -> Void] = [:]
    private let lock = NSLock()

    public init(_ initial: UserSettings = UserSettings()) { current = initial }

    public func update(_ change: (inout UserSettings) -> Void) {
        lock.lock()
        var s = current
        change(&s)
        current = s
        let cbs = Array(observers.values)
        lock.unlock()
        persist(s)
        cbs.forEach { $0(s) }
    }

    open func persist(_ s: UserSettings) {}

    @discardableResult
    public func observe(_ callback: @escaping (UserSettings) -> Void) -> UUID {
        let id = UUID(); lock.lock(); observers[id] = callback; lock.unlock(); return id
    }

    public func clear() { update { $0 = UserSettings() } }
}

public final class UserDefaultsSettingsStore: MemorySettingsStore {
    private let defaults: UserDefaults
    private static let key = "tuulit_settings_v1"

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        let saved = defaults.data(forKey: UserDefaultsSettingsStore.key).flatMap { try? JSONDecoder().decode(UserSettings.self, from: $0) }
        super.init(saved ?? UserSettings())
    }

    public override func persist(_ s: UserSettings) {
        if let d = try? JSONEncoder().encode(s) { defaults.set(d, forKey: UserDefaultsSettingsStore.key) }
    }
}

/// Settings operations with Android's validation and limits.
public extension SettingsStore {
    private func synced(_ change: (inout UserSettings) -> Void) {
        update { s in change(&s); s.syncedSettingsUpdatedAt = Dates.nowMillis() }
    }

    func completeOnboarding(cycleLength: Int, periodLength: Int, regularity: CycleRegularity, reminders: Bool, discreet: Bool, aiContext: Bool) {
        synced { s in
            s.onboardingCompleted = true
            s.cycleLength = cycleLength.clamped(15, 60); s.periodLength = periodLength.clamped(1, 14); s.regularity = regularity
            s.notifyPeriod = reminders; s.notifyCheckIn = reminders; s.discreetNotifications = discreet; s.aiContextOptIn = aiContext
        }
    }
    func setCycleDefaults(_ cycle: Int, _ period: Int, _ regularity: CycleRegularity) {
        synced { $0.cycleLength = cycle.clamped(15, 60); $0.periodLength = period.clamped(1, 14); $0.regularity = regularity }
    }
    func setDisplayName(_ name: String) { synced { $0.displayName = name.trimmed.take(40) } }
    func setTheme(_ m: ThemeMode) { update { $0.themeMode = m } }
    func setReducedMotion(_ on: Bool) { update { $0.reducedMotion = on } }
    func setBananaMode(_ p: BananaModePersona) { synced { $0.bananaMode = p } }
    func setNotTheDay(_ day: Int?) { update { $0.notTheDayOn = day ?? -1 } }
    func setAiContext(_ on: Bool) { synced { $0.aiContextOptIn = on } }
    func setNotifyPeriod(_ on: Bool, leadDays: Int) { update { $0.notifyPeriod = on; $0.periodLeadDays = leadDays.clamped(1, 5) } }
    func setNotifyCheckIn(_ on: Bool, minuteOfDay: Int) { update { $0.notifyCheckIn = on; $0.checkInMinuteOfDay = minuteOfDay.clamped(0, 1439) } }
    func setDiscreet(_ on: Bool) { update { $0.discreetNotifications = on } }
    func setQuietHours(_ start: Int, _ end: Int) { update { $0.quietStartMinute = start.clamped(0, 1439); $0.quietEndMinute = end.clamped(0, 1439) } }
    func setPin(_ pin: String?) { update { $0.pinHash = (pin?.isEmpty ?? true) ? "" : PinHasher.hash(pin!) } }
    func verifyPin(_ pin: String) -> Bool { let h = current.pinHash; return !h.isEmpty && PinHasher.verify(pin, h) }
    func addCustomMood(_ m: String) { synced { s in let v = m.trimmed.take(24); if !v.isEmpty && !s.customMoods.contains(v) { s.customMoods.append(v) } } }
    func addCustomSymptom(_ x: String) { synced { s in let v = x.trimmed.take(24); if !v.isEmpty && !s.customSymptoms.contains(v) { s.customSymptoms.append(v) } } }
    func setSleepNote(_ note: String) { synced { $0.sleepNote = note.take(500) } }
    func setSosMessages(_ list: [String]) {
        synced { $0.sosMessages = list.map { $0.replacingOccurrences(of: "|", with: " ").take(140) }.filter { !$0.isBlank } }
        if current.sosMessages.isEmpty { synced { $0.sosMessages = UserSettings.defaultSOS } }
    }
    func setLastSync(_ at: Int64) { update { $0.lastSyncAt = at } }
    func dismissSuggestion(_ day: Int) { update { $0.suggestionDismissedOn = day } }
    func dismissAccountPrompt() { update { $0.accountPromptDismissed = true } }
}

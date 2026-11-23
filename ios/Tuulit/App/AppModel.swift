import SwiftUI
import FirebaseFirestore
import TuulitCore

struct Toast: Identifiable, Equatable {
    let id = UUID()
    let text: String
    var actionLabel: String? = nil
    static func == (a: Toast, b: Toast) -> Bool { a.id == b.id }
}

/// The app's dependency container and shared UI state, the iOS counterpart of Android's
/// AppContainer + CycleState + shell state. Injected through the SwiftUI environment.
@MainActor
@Observable
final class AppModel {
    // Data
    let store: TuulitStore
    let settingsStore: SettingsStore
    let tracking: TrackingRepository
    let carePlay: CarePlayRepository
    let conversations: ConversationRepository
    // Backend
    let firebase: FirebaseService
    let api: APIClient
    let auth: AuthService
    let sync: SyncService
    let connect: ConnectService
    let ai: AIService
    let media: MediaService
    let reminders = ReminderScheduler()

    private(set) var settings: UserSettings
    private(set) var snapshot: CycleSnapshot
    /// Bumped on every store change so views that read repositories re-render.
    private(set) var dataVersion = 0

    // Shell state
    var tab: Tab = .today
    var paths: [Tab: [Route]] = [.today: [], .cycle: [], .care: [], .ask: []]
    var careSegment = 0
    var sheet: ShellSheet? = nil
    var logDay: LogDay? = nil
    var toast: Toast? = nil
    private var toastAction: (() -> Void)? = nil
    var unlocked = false
    var backgroundedAt: Date? = nil

    struct LogDay: Identifiable, Equatable { let day: Int; var id: Int { day } }

    init(uiTest: Bool = ProcessInfo.processInfo.arguments.contains("-uitest")) {
        if uiTest {
            store = .inMemory()
            settingsStore = MemorySettingsStore()
        } else {
            let url = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent("tuulit.store")
            store = TuulitStore(backend: FileBackend(url: url))
            settingsStore = UserDefaultsSettingsStore()
        }
        firebase = FirebaseService(enabled: !uiTest)
        api = APIClient()
        auth = AuthService(firebase: firebase, api: api)
        conversations = ConversationRepository(store: store)
        settings = settingsStore.current
        snapshot = CycleSnapshot(today: Dates.today(), settings: settingsStore.current, cycles: [], logs: [], painLogs: [])
        sync = SyncService(firebase: firebase, auth: auth, store: store, settings: settingsStore)
        connect = ConnectService(firebase: firebase, auth: auth, api: api)
        ai = AIService(api: api, conversations: conversations)
        media = MediaService(api: api)
        let syncRef = sync
        tracking = TrackingRepository(store: store, onChange: { Task { @MainActor in syncRef.scheduleSync() } })
        carePlay = CarePlayRepository(store: store, onChange: { Task { @MainActor in syncRef.scheduleSync() } })
        carePlay.seedLibraryIfEmpty()
        refresh()
        store.observe { [weak self] in Task { @MainActor in self?.refresh() } }
        settingsStore.observe { [weak self] s in Task { @MainActor in self?.settingsChanged(s) } }
        scheduleMidnightTick()
    }

    // MARK: derived state

    func refresh() {
        settings = settingsStore.current
        snapshot = CycleSnapshot(today: Dates.today(), settings: settings, cycles: tracking.cycles, logs: tracking.dailyLogs, painLogs: tracking.painLogs)
        dataVersion &+= 1
        replanReminders()
        publishSharedStatus()
    }

    private func settingsChanged(_ s: UserSettings) {
        let syncedChanged = s.syncedSettingsUpdatedAt != settings.syncedSettingsUpdatedAt
        settings = s
        UserDefaults(suiteName: "group.app.tuulit")?.set(s.discreetNotifications, forKey: "discreet")
        refresh()
        if syncedChanged { sync.scheduleSync() }
    }

    private func scheduleMidnightTick() {
        Task { [weak self] in
            while true {
                let now = Date()
                let next = Dates.calendar.nextDate(after: now, matching: DateComponents(hour: 0, minute: 0, second: 1), matchingPolicy: .nextTime) ?? now.addingTimeInterval(3600)
                try? await Task.sleep(nanoseconds: UInt64(max(1, next.timeIntervalSince(now)) * 1_000_000_000))
                guard let self else { return }
                self.refresh()
            }
        }
    }

    private var replanTask: Task<Void, Never>?
    private func replanReminders() {
        replanTask?.cancel()
        let s = settings, snap = snapshot
        replanTask = Task { [reminders] in
            try? await Task.sleep(nanoseconds: 800_000_000)
            guard !Task.isCancelled else { return }
            let minute = Dates.calendar.component(.hour, from: Date()) * 60 + Dates.calendar.component(.minute, from: Date())
            let plan = ReminderPlanner.plan(settings: s, prediction: snap.prediction, loggedDays: snap.loggedDays, today: snap.today, nowMinute: minute)
            await reminders.reschedule(plan)
        }
    }

    private var publishTask: Task<Void, Never>?
    private var lastPublished: [String] = []
    private func publishSharedStatus() {
        let p = snapshot.prediction
        let key = [String(snapshot.today), String(p.isCurrentlyMenstruating), String(p.nextPeriodStartEpochDay ?? -1), p.currentPhase.rawValue,
                   connect.myPeople.map { "\($0.id)\($0.permissions.map)" }.joined()]
        guard key != lastPublished, auth.state.isSignedIn else { return }
        lastPublished = key
        publishTask?.cancel()
        publishTask = Task { [connect] in
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            guard !Task.isCancelled else { return }
            await connect.publishAll(prediction: p)
        }
    }

    // MARK: auth lifecycle

    /// Called when the signed-in user changes and whenever the app returns to the foreground.
    func onAuthOrForeground() async {
        connect.listen(uid: auth.state.uid)
        guard auth.state.isSignedIn else { return }
        await sync.sync(fullPull: true)
        refresh()
        if let token = PushTokens.fcmToken { await connect.registerDevice(token: token) }
    }

    // MARK: navigation

    var name: String {
        let n = settings.displayName
        if !n.isBlank { return n }
        if case let .signedIn(_, _, _, displayName) = auth.state { return displayName }
        return ""
    }

    func push(_ r: Route) { paths[tab, default: []].append(r) }
    func pop() { if !(paths[tab]?.isEmpty ?? true) { paths[tab]?.removeLast() } }

    /// Follows an Android-style route string from shared content.
    func go(_ raw: String) {
        guard let target = Route.parse(raw) else { return }
        switch target {
        case .push(let r): push(r)
        case .openLog: openLog(snapshot.today)
        case .tab(let t, let seg):
            tab = t
            paths[t] = []
            if let seg { careSegment = seg }
        }
    }

    func openLog(_ day: Int) { logDay = LogDay(day: min(day, Dates.today())) }

    func showToast(_ text: String, actionLabel: String? = nil, action: (() -> Void)? = nil) {
        toastAction = action
        toast = Toast(text: text, actionLabel: action == nil ? nil : (actionLabel ?? "Undo"))
        let id = toast?.id
        Task { [weak self] in
            try? await Task.sleep(nanoseconds: 4_000_000_000)
            if self?.toast?.id == id { self?.toast = nil }
        }
    }

    func runToastAction() { toastAction?(); toastAction = nil; toast = nil }

    // MARK: app lock

    func didEnterBackground() { backgroundedAt = Date(); store.flush() }

    func willEnterForeground() {
        if let t = backgroundedAt, Date().timeIntervalSince(t) > 60 { unlocked = false }
        backgroundedAt = nil
        if Dates.today() != snapshot.today { refresh() }
    }

    // MARK: account

    /// Erases everything on this phone: database, voice notes, settings and pending reminders.
    func wipeThisDevice() {
        store.wipe()
        media.deleteAllLocal()
        settingsStore.clear()
        reminders.cancelAll()
        carePlay.seedLibraryIfEmpty()
        unlocked = false
        refresh()
    }

    /// Deletes the account everywhere: through the Worker when deployed, else what the rules allow.
    func deleteAccount(password: String) async -> String? {
        guard firebase.isConfigured else { return FirebaseService.notConfiguredMessage }
        if let e = await auth.reauthenticate(password: password) { return e }
        var viaWorker = true
        do { _ = try await api.post("/v1/account/delete", [:]) }
        catch APIError.notConfigured { viaWorker = false }
        catch { return (error as? APIError)?.errorDescription ?? "Couldn't delete the account. Please try again." }
        if !viaWorker {
            await connect.deleteAllMineClientSide()
            if !(await sync.deleteCloudData()) { return "Couldn't remove your cloud backup. Please try again." }
            do { try await firebase.auth?.currentUser?.delete() } catch { return AuthService.friendly(error) }
        }
        auth.signOut()
        wipeThisDevice()
        return nil
    }
}

/// The latest FCM registration token (set by the AppDelegate).
enum PushTokens { @MainActor static var fcmToken: String? }

import Foundation
import FirebaseFirestore
import Network
import TuulitCore

enum SyncResult: Equatable { case skipped, done(pushed: Int, pulled: Int), failed(String) }

/// Backs up and restores users/{uid}/... in the shared Firestore project, with the same document
/// IDs, fields and last-write-wins rules as Android (see TuulitCore.SyncMerge). Only that user can
/// read or write it (firebase/firestore.rules).
@MainActor
@Observable
final class SyncService {
    private let firebase: FirebaseService
    private let auth: AuthService
    private let store: TuulitStore
    private let settings: SettingsStore
    private var running = false
    private var pending: Task<Void, Never>?
    private let monitor = NWPathMonitor()
    private(set) var isOnline = true
    private(set) var lastResult: SyncResult = .skipped

    init(firebase: FirebaseService, auth: AuthService, store: TuulitStore, settings: SettingsStore) {
        self.firebase = firebase; self.auth = auth; self.store = store; self.settings = settings
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in
                let online = path.status == .satisfied
                let cameBack = online && self?.isOnline == false
                self?.isOnline = online
                if cameBack { self?.scheduleSync() }
            }
        }
        monitor.start(queue: DispatchQueue(label: "app.tuulit.network"))
    }

    /// Debounced push: many quick taps in the log composer become one Firestore batch.
    func scheduleSync() {
        guard auth.currentUid != nil else { return }
        pending?.cancel()
        pending = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 4_000_000_000)
            guard !Task.isCancelled else { return }
            _ = await self?.sync()
        }
    }

    @discardableResult
    func sync(fullPull: Bool = false) async -> SyncResult {
        guard !running else { return .skipped }
        guard let db = firebase.firestore, let uid = auth.currentUid else { return .skipped }
        running = true
        defer { running = false }
        do {
            let startedAt = Dates.nowMillis()
            let last = settings.current.lastSyncAt
            var pulled = 0
            if fullPull || last == 0 { pulled = try await pull(db, uid) }
            let pushed = try await push(db, uid, since: fullPull ? 0 : last)
            settings.setLastSync(startedAt)
            lastResult = .done(pushed: pushed, pulled: pulled)
        } catch {
            lastResult = .failed(isOnline ? "Sync failed. Your logs are safe on this phone." : APIError.offline.errorDescription!)
        }
        return lastResult
    }

    private func root(_ db: Firestore, _ uid: String) -> DocumentReference { db.collection("users").document(uid) }

    private func pull(_ db: Firestore, _ uid: String) async throws -> Int {
        let r = root(db, uid)
        var snap = RemoteSnapshot()
        func docs(_ name: String) async throws -> [RemoteDoc] {
            try await r.collection(name).getDocuments(source: .server).documents.map { RemoteDoc(id: $0.documentID, fields: $0.data()) }
        }
        snap.cycles = try await docs("cycles")
        snap.dailyLogs = try await docs("dailyLogs")
        snap.painLogs = try await docs("painLogs")
        snap.comfort = try await docs("comfort")
        snap.games = try await docs("games")
        snap.achievements = try await docs("achievements")
        let n = store.write { SyncMerge.merge(snap, into: &$0) }
        // Cross-device settings (users/{uid}.settings), last write wins.
        if let remote = try await r.getDocument(source: .server).data()?["settings"] as? [String: Any] {
            settings.update { _ = $0.applyRemote(remote) }
        }
        return n
    }

    private func push(_ db: Firestore, _ uid: String, since: Int64) async throws -> Int {
        let r = root(db, uid)
        let writes = store.read { SyncMerge.pushWrites($0, since: since) }
        var i = 0
        while i < writes.count {
            let batch = db.batch()
            for w in writes[i..<min(i + 400, writes.count)] {
                batch.setData(w.fields, forDocument: r.collection(w.collection).document(w.docId))
            }
            try await batch.commit()
            i += 400
        }
        var rootFields: [String: Any] = ["lastBackupAt": FieldValue.serverTimestamp()]
        let s = settings.current
        if s.syncedSettingsUpdatedAt > since || since == 0 { rootFields["settings"] = s.syncedMap }
        if !writes.isEmpty || rootFields["settings"] != nil {
            try await r.setData(rootFields, merge: true)
        }
        return writes.count
    }

    /// Deletes the user's cloud backup (before account deletion, and "Delete cloud backup").
    func deleteCloudData() async -> Bool {
        guard let db = firebase.firestore, let uid = auth.currentUid else { return false }
        let r = root(db, uid)
        do {
            for c in SyncMerge.collections + ["devices"] {
                var docs = try await r.collection(c).limit(to: 400).getDocuments().documents
                while !docs.isEmpty {
                    let batch = db.batch()
                    docs.forEach { batch.deleteDocument($0.reference) }
                    try await batch.commit()
                    docs = try await r.collection(c).limit(to: 400).getDocuments().documents
                }
            }
            try await r.delete()
            settings.setLastSync(0)
            return true
        } catch {
            return false
        }
    }
}

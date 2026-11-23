import Foundation

/// One Firestore document: its ID and raw fields (as delivered by the Firebase SDK).
public struct RemoteDoc {
    public let id: String
    public let fields: [String: Any]
    public init(id: String, fields: [String: Any]) { self.id = id; self.fields = fields }
}

/// Everything under users/{uid}/ that sync reads.
public struct RemoteSnapshot {
    public var cycles: [RemoteDoc] = []
    public var dailyLogs: [RemoteDoc] = []
    public var painLogs: [RemoteDoc] = []
    public var comfort: [RemoteDoc] = []
    public var games: [RemoteDoc] = []
    public var achievements: [RemoteDoc] = []
    public init() {}
}

/// A write for the push batch: users/{uid}/{collection}/{docId}.
public struct PushWrite {
    public let collection: String
    public let docId: String
    public let fields: [String: Any]
}

/// Pure merge and push rules, ported from Android `SyncRepository` so both platforms resolve
/// conflicts identically. The Firebase calls live in the app's SyncService.
public enum SyncMerge {
    public static let collections = ["cycles", "dailyLogs", "painLogs", "comfort", "games", "achievements"]

    private static func int(_ f: [String: Any], _ k: String) -> Int { Int(anyInt64(f[k]) ?? 0) }
    private static func long(_ f: [String: Any], _ k: String) -> Int64 { anyInt64(f[k]) ?? 0 }
    private static func optInt(_ f: [String: Any], _ k: String) -> Int? { anyInt64(f[k]).map { Int($0) } }
    private static func str(_ f: [String: Any], _ k: String) -> String { f[k] as? String ?? "" }
    private static func optStr(_ f: [String: Any], _ k: String) -> String? { f[k] as? String }
    private static func bool(_ f: [String: Any], _ k: String) -> Bool { f[k] as? Bool ?? false }
    private static func orNull(_ v: Any?) -> Any { v ?? NSNull() }

    /// Rows changed since `since` (exclusive), as Firestore writes. Same fields as Android.
    public static func pushWrites(_ d: TuulitData, since: Int64) -> [PushWrite] {
        var w: [PushWrite] = []
        for c in d.cycles.values where c.updatedAt > since {
            w.append(PushWrite(collection: "cycles", docId: c.id, fields: [
                "startEpochDay": c.startEpochDay, "endEpochDay": orNull(c.endEpochDay), "updatedAt": c.updatedAt, "deleted": c.deleted]))
        }
        for l in d.days.values where l.updatedAt > since {
            w.append(PushWrite(collection: "dailyLogs", docId: String(l.epochDay), fields: [
                "epochDay": l.epochDay, "flow": l.flow, "symptoms": l.symptoms, "moods": l.moods, "moodScore": l.moodScore,
                "energyLevel": l.energyLevel, "sleepQuality": l.sleepQuality, "waterGlasses": l.waterGlasses,
                "exerciseNote": l.exerciseNote, "medicationNote": l.medicationNote, "journalText": l.journalText,
                "voiceNoteKey": orNull(l.voiceNoteKey), "updatedAt": l.updatedAt, "deleted": l.deleted]))
        }
        for p in d.pain.values where p.updatedAt > since {
            w.append(PushWrite(collection: "painLogs", docId: p.id, fields: [
                "epochDay": p.epochDay, "intensity": p.intensity, "location": p.location, "painType": p.painType,
                "durationHours": p.durationHours, "medicationTaken": p.medicationTaken, "impactedWorkOrStudy": p.impactedWorkOrStudy,
                "impactedExercise": p.impactedExercise, "impactedSleep": p.impactedSleep, "notes": p.notes,
                "createdAt": p.createdAt, "updatedAt": p.updatedAt, "deleted": p.deleted]))
        }
        for i in d.comfort.values where i.updatedAt > since {
            w.append(PushWrite(collection: "comfort", docId: i.id, fields: [
                "kind": i.kind, "title": i.title, "detail": i.detail, "icon": i.icon, "checked": i.checked, "helpedCount": i.helpedCount,
                "sessionId": orNull(i.sessionId), "position": i.position, "updatedAt": i.updatedAt, "deleted": i.deleted]))
        }
        for g in d.games.values where g.updatedAt > since {
            w.append(PushWrite(collection: "games", docId: g.gameId, fields: ["bestScore": g.bestScore, "plays": g.plays, "updatedAt": g.updatedAt]))
        }
        for (code, at) in d.achievements where at > since {
            w.append(PushWrite(collection: "achievements", docId: code, fields: ["unlockedAt": at]))
        }
        return w
    }

    /// Merges the cloud copy into the local data, keeping whichever row changed most recently.
    /// Returns how many local rows were replaced.
    @discardableResult
    public static func merge(_ r: RemoteSnapshot, into d: inout TuulitData) -> Int {
        var n = 0
        for doc in r.cycles {
            let f = doc.fields
            let remote = CycleRow(id: doc.id, startEpochDay: int(f, "startEpochDay"), endEpochDay: optInt(f, "endEpochDay"),
                                  updatedAt: long(f, "updatedAt"), deleted: bool(f, "deleted"))
            let local = d.cycles[doc.id]
            guard local == nil || local!.updatedAt < remote.updatedAt else { continue }
            // A different local cycle may already own this start date: the newer one wins.
            let sameStart = d.cycleStarting(on: remote.startEpochDay)
            if sameStart == nil || sameStart!.id == remote.id || sameStart!.updatedAt < remote.updatedAt {
                if var loser = sameStart, loser.id != remote.id {
                    loser.deleted = true; loser.startEpochDay = -loser.startEpochDay - 1; loser.updatedAt = remote.updatedAt
                    d.upsertCycle(loser)
                }
                d.upsertCycle(remote); n += 1
            }
        }
        for doc in r.dailyLogs {
            let f = doc.fields
            let remote = DayRow(epochDay: int(f, "epochDay"), flow: str(f, "flow"), symptoms: str(f, "symptoms"), moods: str(f, "moods"),
                                moodScore: int(f, "moodScore"), energyLevel: int(f, "energyLevel"), sleepQuality: int(f, "sleepQuality"),
                                waterGlasses: int(f, "waterGlasses"), exerciseNote: str(f, "exerciseNote"), medicationNote: str(f, "medicationNote"),
                                journalText: str(f, "journalText"), voiceNoteKey: optStr(f, "voiceNoteKey"), updatedAt: long(f, "updatedAt"),
                                deleted: bool(f, "deleted"))
            let local = d.day(remote.epochDay)
            if local == nil || local!.updatedAt < remote.updatedAt { d.upsertDay(remote); n += 1 }
        }
        for doc in r.painLogs {
            let f = doc.fields
            let remote = PainRow(id: doc.id, epochDay: int(f, "epochDay"), intensity: int(f, "intensity"), location: str(f, "location"),
                                 painType: str(f, "painType"), durationHours: int(f, "durationHours"), medicationTaken: str(f, "medicationTaken"),
                                 impactedWorkOrStudy: bool(f, "impactedWorkOrStudy"), impactedExercise: bool(f, "impactedExercise"),
                                 impactedSleep: bool(f, "impactedSleep"), notes: str(f, "notes"), createdAt: long(f, "createdAt"),
                                 updatedAt: long(f, "updatedAt"), deleted: bool(f, "deleted"))
            let local = d.pain[doc.id]
            if local == nil || local!.updatedAt < remote.updatedAt { d.pain[doc.id] = remote; n += 1 }
        }
        var remoteHasKit = false
        for doc in r.comfort {
            let f = doc.fields
            let icon = str(f, "icon")
            let remote = ComfortRow(id: doc.id, kind: str(f, "kind"), title: str(f, "title"), detail: str(f, "detail"),
                                    icon: icon.isEmpty ? "care" : icon, checked: bool(f, "checked"), helpedCount: int(f, "helpedCount"),
                                    sessionId: optStr(f, "sessionId"), position: int(f, "position"), updatedAt: long(f, "updatedAt"),
                                    deleted: bool(f, "deleted"))
            if remote.kind == "kit" && !remote.deleted { remoteHasKit = true }
            let local = d.comfort[doc.id]
            if local == nil || local!.updatedAt < remote.updatedAt { d.comfort[doc.id] = remote; n += 1 }
        }
        // The cloud already has a kit (e.g. from Android): drop this device's untouched starter seed.
        if remoteHasKit {
            for i in CarePlayRepository.seedKit.indices {
                let id = CarePlayRepository.seedId(i)
                if let seed = d.comfort[id], seed.updatedAt == 0, !r.comfort.contains(where: { $0.id == id }) { d.comfort[id] = nil }
            }
        }
        for doc in r.games {
            let f = doc.fields
            let local = d.games[doc.id]
            d.games[doc.id] = GameScoreRow(gameId: doc.id, bestScore: max(local?.bestScore ?? 0, int(f, "bestScore")),
                                           plays: max(local?.plays ?? 0, int(f, "plays")), updatedAt: max(local?.updatedAt ?? 0, long(f, "updatedAt")))
        }
        for doc in r.achievements where d.achievements[doc.id] == nil {
            d.achievements[doc.id] = long(doc.fields, "unlockedAt")
        }
        return n
    }
}

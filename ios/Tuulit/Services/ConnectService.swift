import Foundation
import FirebaseFirestore
import TuulitCore

struct Connection: Identifiable, Equatable {
    let id: String
    let ownerUid: String
    let memberUid: String
    let ownerName: String
    let memberName: String
    let relationship: String
    let permissions: SharePermissions
}

/// What the member sees for a connection: only the fields the owner allowed.
struct SharedStatus: Equatable {
    let ownerName: String
    let periodStartedOn: Int?
    let periodSoonOn: Int?
    let phase: String?
    let roughDayAt: Int64?
    let lastNote: String?
}

struct Invite: Equatable { let code: String; let expiresAt: Int64 }

struct ConnectError: LocalizedError { let message: String; var errorDescription: String? { message } }

/// Trusted-person connections on the shared Firestore collections (invites, connections, shared),
/// protected by the same security rules as Android. Pushes go through the Worker.
@MainActor
@Observable
final class ConnectService {
    private let firebase: FirebaseService
    private let auth: AuthService
    private let api: APIClient
    private(set) var myPeople: [Connection] = []
    private(set) var sharingWithMe: [Connection] = []
    private(set) var loaded = false
    private var listeners: [ListenerRegistration] = []
    private var listeningFor: String?

    init(firebase: FirebaseService, auth: AuthService, api: APIClient) {
        self.firebase = firebase; self.auth = auth; self.api = api
    }

    private var db: Firestore? { firebase.firestore }

    /// Starts or stops the live connection lists when the signed-in user changes.
    func listen(uid: String?) {
        guard uid != listeningFor else { return }
        listeners.forEach { $0.remove() }; listeners = []
        myPeople = []; sharingWithMe = []; loaded = false
        listeningFor = uid
        guard let uid, let db else { loaded = true; return }
        for field in ["ownerUid", "memberUid"] {
            let l = db.collection("connections").whereField(field, isEqualTo: uid).addSnapshotListener { [weak self] snap, _ in
                let list = (snap?.documents ?? []).map(ConnectService.connection)
                Task { @MainActor in
                    if field == "ownerUid" { self?.myPeople = list } else { self?.sharingWithMe = list }
                    self?.loaded = true
                }
            }
            listeners.append(l)
        }
    }

    nonisolated private static func connection(_ d: QueryDocumentSnapshot) -> Connection {
        let f = d.data()
        return Connection(id: d.documentID, ownerUid: f["ownerUid"] as? String ?? "", memberUid: f["memberUid"] as? String ?? "",
                          ownerName: f["ownerName"] as? String ?? "", memberName: f["memberName"] as? String ?? "",
                          relationship: f["relationship"] as? String ?? "", permissions: .from(f["permissions"] as? [String: Any]))
    }

    /// Live status for a connection shared with me.
    func sharedStatus(_ connectionId: String, onChange: @escaping (SharedStatus?) -> Void) -> ListenerRegistration? {
        db?.collection("shared").document(connectionId).addSnapshotListener { d, _ in
            guard let d, d.exists, let f = d.data() else { onChange(nil); return }
            onChange(SharedStatus(ownerName: f["ownerName"] as? String ?? "", periodStartedOn: anyInt64(f["periodStartedOn"]).map { Int($0) },
                                  periodSoonOn: anyInt64(f["periodSoonOn"]).map { Int($0) }, phase: f["phase"] as? String,
                                  roughDayAt: anyInt64(f["roughDayAt"]), lastNote: f["lastNote"] as? String))
        }
    }

    /// Creates a one-time invite code valid for 24 hours.
    func createInvite(ownerName: String) async throws -> Invite {
        guard let db else { throw ConnectError(message: FirebaseService.notConfiguredMessage) }
        guard let uid = auth.currentUid else { throw ConnectError(message: "Sign in to invite someone.") }
        let code = ConnectRules.newInviteCode()
        let expires = Dates.nowMillis() + 24 * 3_600_000
        try await db.collection("invites").document(code).setData([
            "ownerUid": uid, "ownerName": (ownerName.isBlank ? "Your person" : ownerName).take(40), "status": "open",
            "createdAt": FieldValue.serverTimestamp(), "expiresAt": expires,
        ])
        return Invite(code: code, expiresAt: expires)
    }

    /// The invited person enters the code. Creates the connection with every permission off.
    func acceptInvite(_ raw: String, myName: String, relationship: String) async throws {
        guard let db else { throw ConnectError(message: FirebaseService.notConfiguredMessage) }
        guard let uid = auth.currentUid else { throw ConnectError(message: "Sign in to accept an invite.") }
        let code = raw.trimmed.uppercased()
        if let e = Validation.inviteCode(code) { throw ConnectError(message: e) }
        let invite = try await db.collection("invites").document(code).getDocument()
        guard invite.exists, let f = invite.data() else { throw ConnectError(message: "That code doesn't exist. Check it with the person who sent it.") }
        if f["status"] as? String != "open" { throw ConnectError(message: "That code has already been used.") }
        if (anyInt64(f["expiresAt"]) ?? 0) < Dates.nowMillis() { throw ConnectError(message: "That code has expired. Ask for a new one.") }
        let owner = f["ownerUid"] as? String ?? ""
        if owner == uid { throw ConnectError(message: "That's your own invite code.") }
        let connId = "\(owner)_\(uid)"
        let batch = db.batch()
        batch.setData([
            "ownerUid": owner, "memberUid": uid, "ownerName": f["ownerName"] as? String ?? "",
            "memberName": (myName.isBlank ? "Trusted person" : myName).take(40), "relationship": relationship.take(30),
            "inviteCode": code, "permissions": SharePermissions().map, "createdAt": FieldValue.serverTimestamp(),
        ], forDocument: db.collection("connections").document(connId))
        batch.updateData(["status": "accepted", "acceptedBy": uid], forDocument: db.collection("invites").document(code))
        try await batch.commit()
        _ = try? await api.post("/v1/connect/notify", ["connectionId": connId, "type": "accepted"])
    }

    func updatePermissions(_ c: Connection, _ p: SharePermissions, prediction: CyclePrediction?) async throws {
        guard let db else { throw ConnectError(message: FirebaseService.notConfiguredMessage) }
        try await db.collection("connections").document(c.id).updateData(["permissions": p.map])
        try await publishStatus(Connection(id: c.id, ownerUid: c.ownerUid, memberUid: c.memberUid, ownerName: c.ownerName,
                                           memberName: c.memberName, relationship: c.relationship, permissions: p), prediction: prediction)
    }

    /// Either side can end a connection. The shared status document goes with it.
    func disconnect(_ c: Connection) async throws {
        guard let db else { throw ConnectError(message: FirebaseService.notConfiguredMessage) }
        let batch = db.batch()
        batch.deleteDocument(db.collection("shared").document(c.id))
        batch.deleteDocument(db.collection("connections").document(c.id))
        try await batch.commit()
    }

    /// Writes the member-visible status for one connection, containing only permitted fields.
    func publishStatus(_ c: Connection, prediction: CyclePrediction?, roughDayAt: Int64? = nil, note: String? = nil) async throws {
        guard let db else { return }
        var data = ConnectRules.statusFields(permissions: c.permissions, prediction: prediction, today: Dates.today())
        data["ownerUid"] = c.ownerUid; data["memberUid"] = c.memberUid; data["ownerName"] = c.ownerName
        data["updatedAt"] = FieldValue.serverTimestamp()
        if let roughDayAt, c.permissions.roughDay { data["roughDayAt"] = roughDayAt }
        if let note { data["lastNote"] = note.take(140) }
        try await db.collection("shared").document(c.id).setData(data, merge: true)
    }

    /// Keeps every trusted person's status in step with the latest prediction (permitted fields only).
    func publishAll(prediction: CyclePrediction) async {
        for p in myPeople { try? await publishStatus(p, prediction: prediction) }
    }

    /// SOS: records a rough-day signal and asks the Worker to push it to the person's devices.
    func sendSos(_ c: Connection, message: String, prediction: CyclePrediction?) async throws {
        if !c.permissions.roughDay { throw ConnectError(message: "Turn on \"Rough-day signal\" for \(c.memberName) first.") }
        try await publishStatus(c, prediction: prediction, roughDayAt: Dates.nowMillis(), note: message)
        _ = try await api.post("/v1/connect/notify", ["connectionId": c.id, "type": "sos", "message": message.take(140)])
    }

    /// A member's one-tap reply ("Thinking of you").
    func sendReply(_ c: Connection, message: String) async throws {
        _ = try await api.post("/v1/connect/notify", ["connectionId": c.id, "type": "reply", "message": message.take(140)])
    }

    /// Stores this device's FCM token so the Worker can reach it. `platform: ios` tells the Worker
    /// to add an APNs alert (iOS doesn't display data-only messages).
    func registerDevice(token: String) async {
        guard let db, let uid = auth.currentUid else { return }
        try? await db.collection("users").document(uid).collection("devices").document(String(token.suffix(64)))
            .setData(["token": token, "platform": "ios", "updatedAt": FieldValue.serverTimestamp()])
    }

    /// Client-side cleanup used when the Worker isn't deployed (account deletion fallback).
    func deleteAllMineClientSide() async {
        guard let db, let uid = auth.currentUid else { return }
        for field in ["ownerUid", "memberUid"] {
            let docs = (try? await db.collection("connections").whereField(field, isEqualTo: uid).getDocuments().documents) ?? []
            for d in docs {
                try? await db.collection("shared").document(d.documentID).delete()
                try? await d.reference.delete()
            }
        }
        let invites = (try? await db.collection("invites").whereField("ownerUid", isEqualTo: uid).getDocuments().documents) ?? []
        for d in invites { try? await d.reference.delete() }
    }
}

package app.tuulit.data.remote

import app.tuulit.domain.CyclePrediction
import app.tuulit.domain.Dates
import app.tuulit.domain.Validation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.security.SecureRandom

/** What a trusted person may see. Everything starts off. Symptoms, moods and notes are never shareable. */
data class SharePermissions(
    val periodSoon: Boolean = false,
    val periodStarted: Boolean = false,
    val roughDay: Boolean = false,
    val phase: Boolean = false,
) {
    fun toMap() = mapOf("periodSoon" to periodSoon, "periodStarted" to periodStarted, "roughDay" to roughDay, "phase" to phase)
}

data class Connection(
    val id: String,
    val ownerUid: String,
    val memberUid: String,
    val ownerName: String,
    val memberName: String,
    val relationship: String,
    val permissions: SharePermissions,
)

/** What the member sees for a connection: only the fields the owner allowed. */
data class SharedStatus(
    val connectionId: String,
    val ownerName: String,
    val periodStartedOn: Long?,
    val periodSoonOn: Long?,
    val phase: String?,
    val roughDayAt: Long?,
    val lastNote: String?,
)

data class Invite(val code: String, val expiresAt: Long)

/**
 * Trusted-person connections, built only on Firestore + security rules (no Cloud Functions, so it
 * runs on the Spark plan). Push notifications between people go through the Worker, which checks
 * that an active connection exists before sending anything.
 */
class ConnectRepository(
    private val firebase: Firebase,
    private val auth: AuthRepository,
    private val api: ApiClient,
) {
    private val db: FirebaseFirestore? get() = firebase.firestore

    /** Connections where I am the owner (people who can see my updates). */
    val myPeople: Flow<List<Connection>> = query("ownerUid")

    /** Connections where I am the member (people sharing with me). */
    val sharingWithMe: Flow<List<Connection>> = query("memberUid")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun query(field: String): Flow<List<Connection>> = auth.state.flatMapLatest { st ->
        val uid = (st as? AuthState.SignedIn)?.uid
        if (uid == null || db == null) flowOf(emptyList()) else listen(field, uid)
    }

    private fun listen(field: String, uid: String): Flow<List<Connection>> {
        val store = db ?: return flowOf(emptyList())
        return callbackFlow {
            val reg: ListenerRegistration = store.collection("connections").whereEqualTo(field, uid)
                .addSnapshotListener { snap, _ ->
                    trySend(snap?.documents.orEmpty().map { d ->
                        @Suppress("UNCHECKED_CAST")
                        val p = d.get("permissions") as? Map<String, Any?> ?: emptyMap()
                        Connection(
                            d.id, d.getString("ownerUid").orEmpty(), d.getString("memberUid").orEmpty(),
                            d.getString("ownerName").orEmpty(), d.getString("memberName").orEmpty(), d.getString("relationship").orEmpty(),
                            SharePermissions(p["periodSoon"] == true, p["periodStarted"] == true, p["roughDay"] == true, p["phase"] == true),
                        )
                    })
                }
            awaitClose { reg.remove() }
        }
    }

    fun sharedStatus(connectionId: String): Flow<SharedStatus?> {
        val store = db ?: return flowOf(null)
        return callbackFlow {
            val reg = store.collection("shared").document(connectionId).addSnapshotListener { d, _ ->
                trySend(d?.takeIf { it.exists() }?.let {
                    SharedStatus(connectionId, it.getString("ownerName").orEmpty(), it.getLong("periodStartedOn"),
                        it.getLong("periodSoonOn"), it.getString("phase"), it.getLong("roughDayAt"), it.getString("lastNote"))
                })
            }
            awaitClose { reg.remove() }
        }
    }

    /** Creates a one-time invite code valid for 24 hours. */
    suspend fun createInvite(ownerName: String): Result<Invite> = runCatching {
        val store = db ?: error(Firebase.notConfiguredMessage)
        val uid = auth.currentUid ?: error("Sign in to invite someone.")
        val code = newCode()
        val expires = Dates.nowMillis() + 24 * 3600_000L
        store.collection("invites").document(code).set(mapOf(
            "ownerUid" to uid, "ownerName" to ownerName.ifBlank { "Your person" }.take(40), "status" to "open",
            "createdAt" to FieldValue.serverTimestamp(), "expiresAt" to expires,
        )).await()
        Invite(code, expires)
    }

    /** The invited person enters the code. Creates the connection with every permission off. */
    suspend fun acceptInvite(rawCode: String, myName: String, relationship: String): Result<Unit> = runCatching {
        val store = db ?: error(Firebase.notConfiguredMessage)
        val uid = auth.currentUid ?: error("Sign in to accept an invite.")
        val code = rawCode.trim().uppercase()
        Validation.inviteCode(code)?.let { error(it) }
        val invite = store.collection("invites").document(code).get().await()
        if (!invite.exists()) error("That code doesn't exist. Check it with the person who sent it.")
        if (invite.getString("status") != "open") error("That code has already been used.")
        if ((invite.getLong("expiresAt") ?: 0) < Dates.nowMillis()) error("That code has expired. Ask for a new one.")
        val owner = invite.getString("ownerUid").orEmpty()
        if (owner == uid) error("That's your own invite code.")
        val connId = "${owner}_$uid"
        store.runBatch { b ->
            b.set(store.collection("connections").document(connId), mapOf(
                "ownerUid" to owner, "memberUid" to uid, "ownerName" to invite.getString("ownerName").orEmpty(),
                "memberName" to myName.ifBlank { "Trusted person" }.take(40), "relationship" to relationship.take(30),
                "inviteCode" to code, "permissions" to SharePermissions().toMap(), "createdAt" to FieldValue.serverTimestamp(),
            ))
            b.update(store.collection("invites").document(code), mapOf("status" to "accepted", "acceptedBy" to uid))
        }.await()
        runCatching { api.post("/v1/connect/notify", JSONObject().put("connectionId", connId).put("type", "accepted")) }
    }

    suspend fun updatePermissions(c: Connection, p: SharePermissions, prediction: CyclePrediction?): Result<Unit> = runCatching {
        val store = db ?: error(Firebase.notConfiguredMessage)
        store.collection("connections").document(c.id).update("permissions", p.toMap()).await()
        publishStatus(c.copy(permissions = p), prediction)
    }

    suspend fun updateRelationship(c: Connection, relationship: String): Result<Unit> = runCatching {
        db!!.collection("connections").document(c.id).update("relationship", relationship.take(30)).await()
    }

    /** Either side can end a connection. The shared status document is deleted with it. */
    suspend fun disconnect(c: Connection): Result<Unit> = runCatching {
        val store = db ?: error(Firebase.notConfiguredMessage)
        store.runBatch { b ->
            b.delete(store.collection("shared").document(c.id))
            b.delete(store.collection("connections").document(c.id))
        }.await()
    }

    /**
     * Writes the member-visible status for one connection, containing only permitted fields.
     * Called whenever predictions change and when permissions change.
     */
    suspend fun publishStatus(c: Connection, prediction: CyclePrediction?, roughDayAt: Long? = null, note: String? = null) {
        val store = db ?: return
        val p = c.permissions
        val today = Dates.today()
        val data = mutableMapOf<String, Any?>(
            "ownerUid" to c.ownerUid, "memberUid" to c.memberUid, "ownerName" to c.ownerName,
            "updatedAt" to FieldValue.serverTimestamp(),
            "periodStartedOn" to if (p.periodStarted && prediction?.isCurrentlyMenstruating == true)
                prediction.loggedPeriodDays.filter { it <= today }.let { days -> var d = today; while (days.contains(d - 1)) d--; d } else null,
            "periodSoonOn" to if (p.periodSoon && prediction?.nextPeriodStartEpochDay != null &&
                (prediction.nextPeriodStartEpochDay - today) in 0..2) prediction.nextPeriodStartEpochDay else null,
            "phase" to if (p.phase) prediction?.currentPhase?.title else null,
        )
        if (roughDayAt != null && p.roughDay) data["roughDayAt"] = roughDayAt
        if (note != null) data["lastNote"] = note.take(140)
        store.collection("shared").document(c.id).set(data, SetOptions.merge()).await()
    }

    /** SOS: records a rough-day signal and asks the Worker to push it to the person's devices. */
    suspend fun sendSos(c: Connection, message: String, prediction: CyclePrediction?): Result<Unit> = runCatching {
        if (!c.permissions.roughDay) error("Turn on \"Rough-day signal\" for ${c.memberName} first.")
        publishStatus(c, prediction, roughDayAt = Dates.nowMillis(), note = message)
        api.post("/v1/connect/notify", JSONObject().put("connectionId", c.id).put("type", "sos").put("message", message.take(140)))
    }

    /** A member's one-tap reply ("Thinking of you"). */
    suspend fun sendReply(c: Connection, message: String): Result<Unit> = runCatching {
        api.post("/v1/connect/notify", JSONObject().put("connectionId", c.id).put("type", "reply").put("message", message.take(140)))
    }

    /** Stores this device's FCM token so the Worker can reach it. */
    suspend fun registerDevice(token: String) {
        val store = db ?: return
        val uid = auth.currentUid ?: return
        store.collection("users").document(uid).collection("devices").document(token.takeLast(64))
            .set(mapOf("token" to token, "platform" to "android", "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    /** Removes this phone's push token from the account so a signed-out phone gets no Connect alerts. */
    suspend fun unregisterDevice(token: String) {
        val store = db ?: return
        val uid = auth.currentUid ?: return
        store.collection("users").document(uid).collection("devices").document(token.takeLast(64)).delete().await()
    }

    private fun newCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val r = SecureRandom()
        return (1..6).map { alphabet[r.nextInt(alphabet.length)] }.joinToString("")
    }
}

package app.tuulit.data

import android.content.Context
import app.tuulit.data.local.PreferencesRepository
import app.tuulit.data.local.TuulitDao
import app.tuulit.data.remote.ApiClient
import app.tuulit.data.remote.ApiError
import app.tuulit.data.remote.AuthRepository
import app.tuulit.data.remote.AuthResult
import app.tuulit.data.remote.Firebase
import app.tuulit.data.remote.MediaRepository
import app.tuulit.data.remote.SyncRepository
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/** Account deletion and local wipes. */
class AccountRepository(
    private val context: Context,
    private val firebase: Firebase,
    private val auth: AuthRepository,
    private val api: ApiClient,
    private val sync: SyncRepository,
    private val media: MediaRepository,
    private val dao: TuulitDao,
    private val prefs: PreferencesRepository,
) {
    /** Erases everything on this phone: database, voice notes and settings. */
    suspend fun wipeThisDevice() {
        dao.wipe()
        media.deleteAllLocal()
        prefs.clear()
    }

    /**
     * Deletes the account everywhere. The Worker removes Firestore data, connections, invites,
     * R2 objects and the Firebase Auth user with admin rights. If the Worker isn't deployed,
     * the app deletes what the security rules let the owner delete, then the auth user itself.
     */
    suspend fun deleteAccount(password: String): AuthResult {
        if (!firebase.isConfigured) return AuthResult.Failed(Firebase.notConfiguredMessage)
        val re = auth.reauthenticate(password)
        if (re is AuthResult.Failed) return re
        val viaWorker = try {
            api.post("/v1/account/delete", JSONObject()); true
        } catch (e: ApiError.NotConfigured) {
            false
        } catch (e: ApiError) {
            return AuthResult.Failed(e.message ?: "Couldn't delete the account. Please try again.")
        }
        if (!viaWorker) {
            runCatching { deleteConnectionsClientSide() }
            if (!sync.deleteCloudData()) return AuthResult.Failed("Couldn't remove your cloud backup. Please try again.")
            runCatching { firebase.auth?.currentUser?.delete()?.await() }
                .onFailure { return AuthResult.Failed(it.message ?: "Couldn't delete the account.") }
        }
        auth.signOut()
        wipeThisDevice()
        return AuthResult.Ok
    }

    private suspend fun deleteConnectionsClientSide() {
        val db = firebase.firestore ?: return
        val uid = auth.currentUid ?: return
        for (field in listOf("ownerUid", "memberUid")) {
            db.collection("connections").whereEqualTo(field, uid).get().await().documents.forEach { d ->
                db.collection("shared").document(d.id).delete().await()
                d.reference.delete().await()
            }
        }
        db.collection("invites").whereEqualTo("ownerUid", uid).get().await().documents.forEach { it.reference.delete().await() }
    }

    fun clearCache() { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
}

package app.tuulit.data.remote

import android.content.Context
import android.util.Log
import app.tuulit.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Firebase is optional at runtime. Without app/google-services.json the app runs in
 * local-only mode, and every feature that needs Firebase reports [Firebase.notConfiguredMessage].
 */
class Firebase(context: Context) {
    val app: FirebaseApp? = runCatching {
        FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
    }.onFailure { Log.w(TAG, "Firebase unavailable", it) }.getOrNull()

    val isConfigured: Boolean get() = app != null
    val auth: FirebaseAuth? get() = app?.let { FirebaseAuth.getInstance(it) }
    val firestore: FirebaseFirestore? get() = app?.let { FirebaseFirestore.getInstance(it) }
    val messaging: FirebaseMessaging? get() = if (app != null) runCatching { FirebaseMessaging.getInstance() }.getOrNull() else null

    init {
        app?.let { a ->
            runCatching {
                val appCheck = FirebaseAppCheck.getInstance(a)
                if (BuildConfig.DEBUG) {
                    val factory = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                        .getMethod("getInstance").invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
                    appCheck.installAppCheckProviderFactory(factory)
                } else {
                    appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
                }
            }.onFailure { Log.w(TAG, "App Check not installed", it) }
        }
    }

    /** App Check token for calls to the Worker, or null when App Check isn't available. */
    suspend fun appCheckToken(): String? = app?.let { a ->
        runCatching { FirebaseAppCheck.getInstance(a).getAppCheckToken(false).await().token }.getOrNull()
    }

    companion object {
        private const val TAG = "TuulitFirebase"
        const val notConfiguredMessage =
            "Accounts aren't set up in this build yet. Add the Firebase configuration (see docs/FIREBASE_SETUP.md)."
    }
}

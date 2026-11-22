package app.tuulit.data.remote

import app.tuulit.domain.Validation
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

sealed interface AuthState {
    /** Firebase isn't configured in this build. */
    object Unavailable : AuthState
    object SignedOut : AuthState
    data class SignedIn(val uid: String, val email: String, val emailVerified: Boolean, val displayName: String) : AuthState
}

/** Result of an auth action: either success or a message ready to show. */
sealed interface AuthResult {
    object Ok : AuthResult
    data class Failed(val message: String) : AuthResult
}

class AuthRepository(private val firebase: Firebase, private val api: ApiClient) : TokenSource {

    val state: Flow<AuthState> = firebase.auth?.let { auth ->
        callbackFlow {
            val l = com.google.firebase.auth.FirebaseAuth.AuthStateListener { a -> trySend(a.currentUser.toState()) }
            auth.addAuthStateListener(l)
            awaitClose { auth.removeAuthStateListener(l) }
        }
    } ?: flowOf(AuthState.Unavailable)

    val currentUid: String? get() = firebase.auth?.currentUser?.uid
    val current: AuthState get() = firebase.auth?.currentUser.toState().takeIf { firebase.isConfigured } ?: AuthState.Unavailable

    override suspend fun idToken(): String? = firebase.auth?.currentUser?.getIdToken(false)?.await()?.token
    override suspend fun appCheckToken(): String? = firebase.appCheckToken()

    suspend fun register(email: String, password: String, displayName: String): AuthResult {
        val auth = firebase.auth ?: return AuthResult.Failed(Firebase.notConfiguredMessage)
        Validation.email(email)?.let { return AuthResult.Failed(it) }
        Validation.password(password)?.let { return AuthResult.Failed(it) }
        return guard {
            val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user ?: error("No user")
            if (displayName.isNotBlank()) {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build()).await()
            }
            sendVerification()
            // Welcome email is best-effort through the Worker + Brevo; registration never fails because of it.
            runCatching { api.post("/v1/email/welcome", JSONObject().put("displayName", displayName.trim())) }
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val auth = firebase.auth ?: return AuthResult.Failed(Firebase.notConfiguredMessage)
        Validation.email(email)?.let { return AuthResult.Failed(it) }
        if (password.isEmpty()) return AuthResult.Failed("Enter your password.")
        return guard { auth.signInWithEmailAndPassword(email.trim(), password).await() }
    }

    fun signOut() { firebase.auth?.signOut() }

    /** Uses Firebase's own reset email: it is free, rate-limited by Google and needs no secret. */
    suspend fun sendPasswordReset(email: String): AuthResult {
        val auth = firebase.auth ?: return AuthResult.Failed(Firebase.notConfiguredMessage)
        Validation.email(email)?.let { return AuthResult.Failed(it) }
        return guard { auth.sendPasswordResetEmail(email.trim()).await() }
    }

    /**
     * Sends the verification email. The branded Brevo email is tried first through the Worker
     * (which generates the Firebase link server-side); Firebase's built-in email is the fallback.
     */
    suspend fun sendVerification(): AuthResult {
        val user = firebase.auth?.currentUser ?: return AuthResult.Failed("Sign in first.")
        val viaBrevo = runCatching { api.post("/v1/email/verification", JSONObject()) }.isSuccess
        return if (viaBrevo) AuthResult.Ok else guard { user.sendEmailVerification().await() }
    }

    suspend fun refresh(): AuthState {
        firebase.auth?.currentUser?.reload()?.await()
        firebase.auth?.currentUser?.getIdToken(true)?.await()
        return current
    }

    suspend fun updateDisplayName(name: String): AuthResult {
        val user = firebase.auth?.currentUser ?: return AuthResult.Ok
        return guard { user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await() }
    }

    /** Re-checks the password before a destructive action such as account deletion. */
    suspend fun reauthenticate(password: String): AuthResult {
        val user = firebase.auth?.currentUser ?: return AuthResult.Failed("Sign in first.")
        val email = user.email ?: return AuthResult.Failed("This account has no email.")
        return guard {
            user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)).await()
        }
    }

    private inline fun guard(block: () -> Unit): AuthResult = try {
        block(); AuthResult.Ok
    } catch (e: Exception) {
        AuthResult.Failed(friendly(e))
    }

    private fun friendly(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "That password is too easy to guess. Try a longer one."
        is FirebaseAuthUserCollisionException -> "There's already an account with that email. Try signing in."
        is FirebaseAuthInvalidUserException -> "We couldn't find an account with that email."
        is FirebaseAuthInvalidCredentialsException -> "That email and password don't match."
        is FirebaseAuthRecentLoginRequiredException -> "For your security, please enter your password again."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a few minutes."
        is FirebaseNetworkException -> "You're offline. Check your connection and try again."
        else -> e.message ?: "Something went wrong. Please try again."
    }

    private fun FirebaseUser?.toState(): AuthState =
        if (this == null) AuthState.SignedOut else AuthState.SignedIn(uid, email.orEmpty(), isEmailVerified, displayName.orEmpty())
}

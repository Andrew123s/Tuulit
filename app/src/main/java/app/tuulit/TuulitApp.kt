package app.tuulit

import android.app.Application
import android.content.Context
import app.tuulit.data.AccountRepository
import app.tuulit.data.CarePlayRepository
import app.tuulit.data.ChangeListener
import app.tuulit.data.CycleState
import app.tuulit.data.remote.TokenSource
import app.tuulit.data.TrackingRepository
import app.tuulit.data.local.PreferencesRepository
import app.tuulit.data.local.TuulitDatabase
import app.tuulit.data.remote.AiRepository
import app.tuulit.data.remote.ApiClient
import app.tuulit.data.remote.AuthRepository
import app.tuulit.data.remote.AuthState
import app.tuulit.data.remote.ConnectRepository
import app.tuulit.data.remote.Firebase
import app.tuulit.data.remote.MediaRepository
import app.tuulit.data.remote.SyncRepository
import app.tuulit.notifications.Notifications
import app.tuulit.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TuulitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.start()
    }
}

/** Manual dependency container. One instance per process. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val appContext = context.applicationContext

    val prefs = PreferencesRepository(appContext)
    val firebase = Firebase(appContext)
    private val db: TuulitDatabase by lazy { TuulitDatabase.open(appContext) }
    private val dao get() = db.dao()

    private val syncTrigger = ChangeListener { scheduleSync() }

    val tracking by lazy { TrackingRepository(dao, syncTrigger) }
    val carePlay by lazy { CarePlayRepository(dao, syncTrigger) }
    val auth by lazy { AuthRepository(firebase, ApiClient(LazyTokens())) }
    val api by lazy { ApiClient(auth) }
    val sync by lazy { SyncRepository(firebase, auth, dao, prefs) }
    val ai by lazy { AiRepository(api, dao) }
    val connect by lazy { ConnectRepository(firebase, auth, api) }
    val media by lazy { MediaRepository(appContext, api) }
    val account by lazy { AccountRepository(appContext, firebase, auth, api, sync, media, dao, prefs) }
    val cycleState by lazy { CycleState(appScope, tracking, prefs) }

    /** True once the app-lock PIN has been entered in this process. */
    val unlocked = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** Breaks the auth <-> api cycle: the API client asks auth for tokens lazily. */
    private inner class LazyTokens : TokenSource {
        override suspend fun idToken() = auth.idToken()
        override suspend fun appCheckToken() = firebase.appCheckToken()
    }

    private var syncJob: Job? = null

    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch { carePlay.seedLibraryIfEmpty() }
        // Reschedule local reminders whenever their settings change.
        appScope.launch {
            prefs.settings.map { listOf(it.onboardingCompleted, it.notifyPeriod, it.notifyCheckIn, it.checkInMinuteOfDay, it.periodLeadDays, it.quietStartMinute, it.quietEndMinute) }
                .distinctUntilChanged()
                .collect { if (prefs.current().onboardingCompleted) ReminderScheduler.reschedule(appContext, prefs.current()) }
        }
        publishSharedStatus()
        // Settings that follow the person between devices are pushed like any other change.
        appScope.launch {
            prefs.settings.map { it.syncedSettingsUpdatedAt }.distinctUntilChanged().drop(1).collect { scheduleSync() }
        }
        // Every change of account or verification state goes through onAuthChanged (one at a time).
        appScope.launch {
            auth.state.map { st -> (st as? AuthState.SignedIn)?.let { it.uid to it.emailVerified } }
                .distinctUntilChanged()
                .collect { onAuthChanged(it?.first, it?.second == true) }
        }
    }

    /** Where the signed-in, verified account is in getting ready to use the app. */
    sealed interface Session {
        /** Not signed in, not verified yet, or still deciding. */
        object Idle : Session
        /** First sign-in on this phone: restoring the cloud backup before showing the app. */
        object Restoring : Session
        object Ready : Session
        data class Failed(val message: String) : Session
    }

    val session = kotlinx.coroutines.flow.MutableStateFlow<Session>(Session.Idle)

    /**
     * Keeps local data isolated per account. The phone's data belongs to one Firebase uid (stored in prefs).
     * - Data from before accounts were required has no owner and is adopted by the first verified account.
     * - When a different verified account signs in, the previous account's local data is erased first.
     * - Unverified accounts never claim or see data: they stay on the verify-email screen.
     */
    private suspend fun onAuthChanged(uid: String?, verified: Boolean) {
        if (uid == null || !verified) { session.value = Session.Idle; return }
        val owner = prefs.sessionUid()
        if (owner.isNotEmpty() && owner != uid) account.wipeUserData()
        prefs.setSessionUid(uid)
        carePlay.seedLibraryIfEmpty()
        if (owner == uid && prefs.current().onboardingCompleted) {
            // Returning user on this phone: open straight away and merge the cloud copy in the background.
            session.value = Session.Ready
            sync.sync(fullPull = true)
        } else {
            restore()
        }
        runCatching { firebase.messaging?.token?.await()?.let { connect.registerDevice(it) } }
    }

    /** Pulls the cloud backup. A returning account with data in the cloud skips onboarding. */
    suspend fun restore() {
        session.value = Session.Restoring
        when (val r = sync.sync(fullPull = true)) {
            is app.tuulit.data.remote.SyncResult.Done -> {
                if (r.cloudHadData) prefs.markOnboarded()
                session.value = Session.Ready
            }
            is app.tuulit.data.remote.SyncResult.Failed -> session.value = Session.Failed(r.message)
            app.tuulit.data.remote.SyncResult.Skipped -> session.value = Session.Ready
        }
    }

    fun retryRestore() { appScope.launch { restore() } }

    /** Lets the person use the app without the restore (offline); the next sync merges the backup in. */
    fun continueWithoutRestore() { session.value = Session.Ready }

    /** Full sign-out: back up, unregister push, sign out, erase this account's data from the phone. */
    suspend fun signOut(force: Boolean = false): app.tuulit.data.SignOutResult {
        syncJob?.cancel()
        val r = account.signOut(force) { connect.unregisterDevice(it) }
        if (r is app.tuulit.data.SignOutResult.Done) {
            unlocked.value = false
            session.value = Session.Idle
            carePlay.seedLibraryIfEmpty()
        }
        return r
    }

    /** Keeps each trusted person's shared status in step with the latest prediction (permitted fields only). */
    @OptIn(FlowPreview::class)
    private fun publishSharedStatus() = appScope.launch {
        kotlinx.coroutines.flow.combine(
            connect.myPeople,
            cycleState.snapshot.map { s -> listOf(s.today, s.prediction.isCurrentlyMenstruating, s.prediction.nextPeriodStartEpochDay, s.prediction.currentPhase) }
                .distinctUntilChanged(),
        ) { people, _ -> people }
            .debounce(2_000)
            .collect { people -> people.forEach { p -> runCatching { connect.publishStatus(p, cycleState.snapshot.value.prediction) } } }
    }

    /** Debounced push: many quick taps in the log composer become one Firestore batch. */
    /** Full pull + push, used when the app returns to the foreground while signed in. */
    fun syncOnForeground() {
        if (auth.currentUid == null) return
        // Picks up a confirmed email or a disabled/deleted account when the app comes back.
        if (!auth.isVerified) { appScope.launch { runCatching { auth.refresh() } }; return }
        appScope.launch { sync.sync(fullPull = true) }
    }

    fun scheduleSync() {
        if (auth.currentUid == null || !auth.isVerified || session.value != Session.Ready) return
        syncJob?.cancel()
        syncJob = appScope.launch {
            delay(4_000)
            sync.sync()
        }
    }
}

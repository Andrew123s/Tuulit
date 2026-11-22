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
        // On sign-in: restore and merge the cloud backup, then register this device for Connect pushes.
        appScope.launch {
            auth.state.map { (it as? AuthState.SignedIn)?.uid }.distinctUntilChanged().collect { uid ->
                if (uid != null) {
                    sync.sync(fullPull = true)
                    runCatching { firebase.messaging?.token?.await()?.let { connect.registerDevice(it) } }
                }
            }
        }
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
    fun scheduleSync() {
        if (auth.currentUid == null) return
        syncJob?.cancel()
        syncJob = appScope.launch {
            delay(4_000)
            sync.sync()
        }
    }
}

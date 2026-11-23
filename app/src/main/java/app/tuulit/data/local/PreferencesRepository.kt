package app.tuulit.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.tuulit.domain.BananaModePersona
import app.tuulit.domain.CycleRegularity
import app.tuulit.domain.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tuulitPrefs: DataStore<Preferences> by preferencesDataStore(name = "tuulit_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val displayName: String = "",
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val regularity: CycleRegularity = CycleRegularity.NOT_SURE,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val reducedMotion: Boolean = false,
    val bananaMode: BananaModePersona = BananaModePersona.NEUTRAL,
    /** Epoch day on which Today Is Not the Day is active; it switches off at midnight. */
    val notTheDayOn: Long = -1,
    val aiContextOptIn: Boolean = false,
    val notifyPeriod: Boolean = true,
    val periodLeadDays: Int = 2,
    val notifyCheckIn: Boolean = true,
    val checkInMinuteOfDay: Int = 20 * 60,
    val notifyCare: Boolean = false,
    val discreetNotifications: Boolean = true,
    val quietStartMinute: Int = 22 * 60,
    val quietEndMinute: Int = 8 * 60,
    val pinHash: String = "",
    val customMoods: List<String> = emptyList(),
    val customSymptoms: List<String> = emptyList(),
    val sleepNote: String = "",
    val sosMessages: List<String> = DEFAULT_SOS,
    val lastSyncAt: Long = 0,
    val ribbonLegendSeen: Boolean = false,
    val suggestionDismissedOn: Long = -1,
    val accountPromptDismissed: Boolean = false,
) {
    val appLockEnabled: Boolean get() = pinHash.isNotEmpty()

    companion object {
        val DEFAULT_SOS = listOf(
            "Rough period day. Could you check in on me?",
            "Cramps are bad. Could you bring painkillers?",
            "Could you pick up pads on your way home?",
            "I need a quiet evening. Nothing's wrong.",
        )
    }
}

class PreferencesRepository(private val context: Context) {
    private object K {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val CYCLE = intPreferencesKey("cycle_length")
        val PERIOD = intPreferencesKey("period_length")
        val REGULARITY = stringPreferencesKey("regularity")
        val THEME = stringPreferencesKey("theme")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val BANANA = stringPreferencesKey("banana_mode")
        val NOT_THE_DAY = longPreferencesKey("not_the_day_on")
        val AI_CONTEXT = booleanPreferencesKey("ai_context")
        val N_PERIOD = booleanPreferencesKey("n_period")
        val N_LEAD = intPreferencesKey("n_period_lead")
        val N_CHECKIN = booleanPreferencesKey("n_checkin")
        val N_CHECKIN_AT = intPreferencesKey("n_checkin_at")
        val N_CARE = booleanPreferencesKey("n_care")
        val DISCREET = booleanPreferencesKey("discreet")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")
        val PIN = stringPreferencesKey("pin_hash")
        val MOODS = stringPreferencesKey("custom_moods")
        val SYMPTOMS = stringPreferencesKey("custom_symptoms")
        val SLEEP = stringPreferencesKey("sleep_note")
        val SOS = stringPreferencesKey("sos_messages")
        val LAST_SYNC = longPreferencesKey("last_sync")
        val LEGEND = booleanPreferencesKey("ribbon_legend")
        val SUGG_DISMISSED = longPreferencesKey("suggestion_dismissed")
        val ACCOUNT_PROMPT = booleanPreferencesKey("account_prompt_dismissed")
    }

    private val store get() = context.tuulitPrefs

    val settings: Flow<UserSettings> = store.data.map { p ->
        UserSettings(
            onboardingCompleted = p[K.ONBOARDED] ?: false,
            displayName = p[K.NAME] ?: "",
            cycleLength = p[K.CYCLE] ?: 28,
            periodLength = p[K.PERIOD] ?: 5,
            regularity = CycleRegularity.fromName(p[K.REGULARITY]),
            themeMode = runCatching { ThemeMode.valueOf(p[K.THEME] ?: "LIGHT") }.getOrDefault(ThemeMode.LIGHT),
            reducedMotion = p[K.REDUCED_MOTION] ?: false,
            bananaMode = BananaModePersona.fromName(p[K.BANANA]),
            notTheDayOn = p[K.NOT_THE_DAY] ?: -1,
            aiContextOptIn = p[K.AI_CONTEXT] ?: false,
            notifyPeriod = p[K.N_PERIOD] ?: true,
            periodLeadDays = p[K.N_LEAD] ?: 2,
            notifyCheckIn = p[K.N_CHECKIN] ?: true,
            checkInMinuteOfDay = p[K.N_CHECKIN_AT] ?: (20 * 60),
            notifyCare = p[K.N_CARE] ?: false,
            discreetNotifications = p[K.DISCREET] ?: true,
            quietStartMinute = p[K.QUIET_START] ?: (22 * 60),
            quietEndMinute = p[K.QUIET_END] ?: (8 * 60),
            pinHash = p[K.PIN] ?: "",
            customMoods = split(p[K.MOODS]),
            customSymptoms = split(p[K.SYMPTOMS]),
            sleepNote = p[K.SLEEP] ?: "",
            sosMessages = split(p[K.SOS]).ifEmpty { UserSettings.DEFAULT_SOS },
            lastSyncAt = p[K.LAST_SYNC] ?: 0,
            ribbonLegendSeen = p[K.LEGEND] ?: false,
            suggestionDismissedOn = p[K.SUGG_DISMISSED] ?: -1,
            accountPromptDismissed = p[K.ACCOUNT_PROMPT] ?: false,
        )
    }

    suspend fun current(): UserSettings = settings.first()

    private suspend fun edit(block: (MutablePreferences) -> Unit) { store.edit { block(it) } }

    suspend fun completeOnboarding(cycleLength: Int, periodLength: Int, regularity: CycleRegularity,
                                   reminders: Boolean, discreet: Boolean, aiContext: Boolean) = edit {
        it[K.ONBOARDED] = true
        it[K.CYCLE] = cycleLength.coerceIn(15, 60)
        it[K.PERIOD] = periodLength.coerceIn(1, 14)
        it[K.REGULARITY] = regularity.name
        it[K.N_PERIOD] = reminders
        it[K.N_CHECKIN] = reminders
        it[K.DISCREET] = discreet
        it[K.AI_CONTEXT] = aiContext
    }

    suspend fun setCycleDefaults(cycleLength: Int, periodLength: Int, regularity: CycleRegularity) = edit {
        it[K.CYCLE] = cycleLength.coerceIn(15, 60); it[K.PERIOD] = periodLength.coerceIn(1, 14); it[K.REGULARITY] = regularity.name
    }
    suspend fun setDisplayName(name: String) = edit { it[K.NAME] = name.trim().take(40) }
    suspend fun setTheme(mode: ThemeMode) = edit { it[K.THEME] = mode.name }
    suspend fun setReducedMotion(on: Boolean) = edit { it[K.REDUCED_MOTION] = on }
    suspend fun setBananaMode(p: BananaModePersona) = edit { it[K.BANANA] = p.name }
    suspend fun setNotTheDay(onDay: Long?) = edit { it[K.NOT_THE_DAY] = onDay ?: -1 }
    suspend fun setAiContext(on: Boolean) = edit { it[K.AI_CONTEXT] = on }
    suspend fun setNotifyPeriod(on: Boolean, leadDays: Int) = edit { it[K.N_PERIOD] = on; it[K.N_LEAD] = leadDays.coerceIn(1, 5) }
    suspend fun setNotifyCheckIn(on: Boolean, minuteOfDay: Int) = edit { it[K.N_CHECKIN] = on; it[K.N_CHECKIN_AT] = minuteOfDay.coerceIn(0, 1439) }
    suspend fun setNotifyCare(on: Boolean) = edit { it[K.N_CARE] = on }
    suspend fun setDiscreet(on: Boolean) = edit { it[K.DISCREET] = on }
    suspend fun setQuietHours(start: Int, end: Int) = edit { it[K.QUIET_START] = start.coerceIn(0, 1439); it[K.QUIET_END] = end.coerceIn(0, 1439) }
    suspend fun setPin(pin: String?) = edit { it[K.PIN] = if (pin.isNullOrEmpty()) "" else PinHasher.hash(pin) }
    suspend fun verifyPin(pin: String): Boolean = current().pinHash.let { it.isNotEmpty() && PinHasher.verify(pin, it) }
    suspend fun addCustomMood(m: String) = edit { it[K.MOODS] = (split(it[K.MOODS]) + m.trim().take(24)).distinct().joinToString("|") }
    suspend fun addCustomSymptom(s: String) = edit { it[K.SYMPTOMS] = (split(it[K.SYMPTOMS]) + s.trim().take(24)).distinct().joinToString("|") }
    suspend fun setSleepNote(note: String) = edit { it[K.SLEEP] = note.take(500) }
    suspend fun setSosMessages(list: List<String>) = edit { it[K.SOS] = list.map { m -> m.replace("|", " ").take(140) }.filter { m -> m.isNotBlank() }.joinToString("|") }
    suspend fun setLastSync(at: Long) = edit { it[K.LAST_SYNC] = at }
    suspend fun setRibbonLegendSeen() = edit { it[K.LEGEND] = true }
    suspend fun dismissSuggestion(day: Long) = edit { it[K.SUGG_DISMISSED] = day }
    suspend fun dismissAccountPrompt() = edit { it[K.ACCOUNT_PROMPT] = true }
    suspend fun clear() = store.edit { it.clear() }

    private fun split(raw: String?) = raw.orEmpty().split("|").map { it.trim() }.filter { it.isNotEmpty() }
}

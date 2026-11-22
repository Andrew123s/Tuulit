package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.BananaModePersona
import com.example.domain.CycleRegularity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tuulitDataStore: DataStore<Preferences> by preferencesDataStore(name = "tuulit_preferences")

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val displayName: String = "",
    val defaultCycleLength: Int = 28,
    val defaultPeriodDuration: Int = 5,
    val cycleRegularity: CycleRegularity = CycleRegularity.NOT_SURE,
    val darkThemeEnabled: Boolean = false,
    val reducedMotionEnabled: Boolean = false,
    val bananaMode: BananaModePersona = BananaModePersona.NEUTRAL,
    val todayIsNotTheDayMode: Boolean = false,
    val aiContextOptIn: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val notifyPeriodApproaching: Boolean = true,
    val notifyDailyCheckIn: Boolean = true,
    val notifyCareReminders: Boolean = false,
    val hideLockScreenDetails: Boolean = true,
    val quietHoursLabel: String = "22:00 – 08:00",
    val privateModePin: String = "", // empty means unlocked / disabled
    val customMoods: List<String> = emptyList(),
    val customSymptoms: List<String> = emptyList(),
    val sleepRoutineNote: String = "",
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val DEFAULT_CYCLE_LENGTH = intPreferencesKey("default_cycle_length")
        val DEFAULT_PERIOD_DURATION = intPreferencesKey("default_period_duration")
        val CYCLE_REGULARITY = stringPreferencesKey("cycle_regularity")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val BANANA_MODE = stringPreferencesKey("banana_mode")
        val TODAY_NOT_THE_DAY = booleanPreferencesKey("today_not_the_day")
        val AI_CONTEXT_OPT_IN = booleanPreferencesKey("ai_context_opt_in")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_PERIOD = booleanPreferencesKey("notify_period")
        val NOTIFY_CHECKIN = booleanPreferencesKey("notify_checkin")
        val NOTIFY_CARE = booleanPreferencesKey("notify_care")
        val HIDE_LOCK_SCREEN = booleanPreferencesKey("hide_lock_screen")
        val QUIET_HOURS = stringPreferencesKey("quiet_hours")
        val PRIVATE_PIN = stringPreferencesKey("private_pin")
        val CUSTOM_MOODS = stringPreferencesKey("custom_moods")
        val CUSTOM_SYMPTOMS = stringPreferencesKey("custom_symptoms")
        val SLEEP_ROUTINE_NOTE = stringPreferencesKey("sleep_routine_note")
    }

    val preferencesFlow: Flow<UserPreferences> = context.tuulitDataStore.data.map { prefs ->
        UserPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            defaultCycleLength = prefs[Keys.DEFAULT_CYCLE_LENGTH] ?: 28,
            defaultPeriodDuration = prefs[Keys.DEFAULT_PERIOD_DURATION] ?: 5,
            cycleRegularity = CycleRegularity.fromName(prefs[Keys.CYCLE_REGULARITY]),
            darkThemeEnabled = prefs[Keys.DARK_THEME] ?: false,
            reducedMotionEnabled = prefs[Keys.REDUCED_MOTION] ?: false,
            bananaMode = BananaModePersona.fromName(prefs[Keys.BANANA_MODE]),
            todayIsNotTheDayMode = prefs[Keys.TODAY_NOT_THE_DAY] ?: false,
            aiContextOptIn = prefs[Keys.AI_CONTEXT_OPT_IN] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            notifyPeriodApproaching = prefs[Keys.NOTIFY_PERIOD] ?: true,
            notifyDailyCheckIn = prefs[Keys.NOTIFY_CHECKIN] ?: true,
            notifyCareReminders = prefs[Keys.NOTIFY_CARE] ?: false,
            hideLockScreenDetails = prefs[Keys.HIDE_LOCK_SCREEN] ?: true,
            quietHoursLabel = prefs[Keys.QUIET_HOURS] ?: "22:00 – 08:00",
            privateModePin = prefs[Keys.PRIVATE_PIN] ?: "",
            customMoods = (prefs[Keys.CUSTOM_MOODS] ?: "").split("|").filter { it.isNotBlank() },
            customSymptoms = (prefs[Keys.CUSTOM_SYMPTOMS] ?: "").split("|").filter { it.isNotBlank() },
            sleepRoutineNote = prefs[Keys.SLEEP_ROUTINE_NOTE] ?: "",
        )
    }

    suspend fun completeOnboarding(
        displayName: String,
        cycleLength: Int,
        periodDuration: Int,
        regularity: CycleRegularity,
        notificationsEnabled: Boolean,
        aiContextOptIn: Boolean,
    ) {
        context.tuulitDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
            prefs[Keys.DISPLAY_NAME] = displayName.trim()
            prefs[Keys.DEFAULT_CYCLE_LENGTH] = cycleLength.coerceIn(21, 45)
            prefs[Keys.DEFAULT_PERIOD_DURATION] = periodDuration.coerceIn(2, 10)
            prefs[Keys.CYCLE_REGULARITY] = regularity.name
            prefs[Keys.NOTIFICATIONS_ENABLED] = notificationsEnabled
            prefs[Keys.AI_CONTEXT_OPT_IN] = aiContextOptIn
        }
    }

    suspend fun updateProfilePreferences(
        displayName: String,
        cycleLength: Int,
        periodDuration: Int,
        regularity: CycleRegularity
    ) {
        context.tuulitDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = displayName.trim()
            prefs[Keys.DEFAULT_CYCLE_LENGTH] = cycleLength.coerceIn(21, 45)
            prefs[Keys.DEFAULT_PERIOD_DURATION] = periodDuration.coerceIn(2, 10)
            prefs[Keys.CYCLE_REGULARITY] = regularity.name
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.tuulitDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.tuulitDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setBananaMode(persona: BananaModePersona) {
        context.tuulitDataStore.edit { it[Keys.BANANA_MODE] = persona.name }
    }

    suspend fun setTodayIsNotTheDay(enabled: Boolean) {
        context.tuulitDataStore.edit { it[Keys.TODAY_NOT_THE_DAY] = enabled }
    }

    suspend fun setAiContextOptIn(enabled: Boolean) {
        context.tuulitDataStore.edit { it[Keys.AI_CONTEXT_OPT_IN] = enabled }
    }

    suspend fun updateNotificationSettings(
        enabled: Boolean,
        periodApproaching: Boolean,
        dailyCheckIn: Boolean,
        careReminders: Boolean,
        hideLockScreen: Boolean,
        quietHours: String,
    ) {
        context.tuulitDataStore.edit {
            it[Keys.NOTIFICATIONS_ENABLED] = enabled
            it[Keys.NOTIFY_PERIOD] = periodApproaching
            it[Keys.NOTIFY_CHECKIN] = dailyCheckIn
            it[Keys.NOTIFY_CARE] = careReminders
            it[Keys.HIDE_LOCK_SCREEN] = hideLockScreen
            it[Keys.QUIET_HOURS] = quietHours
        }
    }

    suspend fun setPrivateModePin(pin: String) {
        context.tuulitDataStore.edit { it[Keys.PRIVATE_PIN] = pin.trim() }
    }

    suspend fun addCustomMood(mood: String) {
        val clean = mood.trim().replace("|", "")
        if (clean.isBlank()) return
        context.tuulitDataStore.edit { prefs ->
            val current = (prefs[Keys.CUSTOM_MOODS] ?: "").split("|").filter { it.isNotBlank() }.toMutableList()
            if (current.none { it.equals(clean, ignoreCase = true) }) {
                current.add(clean)
                prefs[Keys.CUSTOM_MOODS] = current.joinToString("|")
            }
        }
    }

    suspend fun addCustomSymptom(symptom: String) {
        val clean = symptom.trim().replace("|", "").replace(":", "")
        if (clean.isBlank()) return
        context.tuulitDataStore.edit { prefs ->
            val current = (prefs[Keys.CUSTOM_SYMPTOMS] ?: "").split("|").filter { it.isNotBlank() }.toMutableList()
            if (current.none { it.equals(clean, ignoreCase = true) }) {
                current.add(clean)
                prefs[Keys.CUSTOM_SYMPTOMS] = current.joinToString("|")
            }
        }
    }

    suspend fun saveSleepRoutineNote(note: String) {
        context.tuulitDataStore.edit { it[Keys.SLEEP_ROUTINE_NOTE] = note }
    }

    suspend fun clearAllPreferences() {
        context.tuulitDataStore.edit { it.clear() }
    }
}

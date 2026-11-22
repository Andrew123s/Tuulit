package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AchievementEntity
import com.example.data.EmergencyChecklistItemEntity
import com.example.data.GameScoreEntity
import com.example.data.SurvivalKitItemEntity
import com.example.data.TrustedContactEntity
import com.example.data.TuulitRepository
import com.example.data.UserPreferences
import com.example.domain.AiChatMessage
import com.example.domain.AiContextSnapshot
import com.example.domain.AppointmentPrepSummary
import com.example.domain.BananaModePersona
import com.example.domain.CycleCalculator
import com.example.domain.CyclePhase
import com.example.domain.CyclePrediction
import com.example.domain.CycleRecord
import com.example.domain.CycleRegularity
import com.example.domain.DailyLog
import com.example.domain.FlowIntensity
import com.example.domain.InsightsData
import com.example.domain.MythCheckResult
import com.example.domain.PainLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TuulitUiState(
    val isReady: Boolean = false,
    val todayEpochDay: Long = TuulitRepository.todayEpochDay(),
    val prefs: UserPreferences = UserPreferences(),
    val cycles: List<CycleRecord> = emptyList(),
    val dailyLogs: List<DailyLog> = emptyList(),
    val painLogs: List<PainLog> = emptyList(),
    val survivalKit: List<SurvivalKitItemEntity> = emptyList(),
    val checklist: List<EmergencyChecklistItemEntity> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList(),
    val trustedContacts: List<TrustedContactEntity> = emptyList(),
    val gameScores: Map<String, GameScoreEntity> = emptyMap(),
    val prediction: CyclePrediction = CycleCalculator.calculatePrediction(
        todayEpochDay = TuulitRepository.todayEpochDay(),
        cycles = emptyList(),
        dailyLogs = emptyList(),
        fallbackCycleLength = 28,
        fallbackPeriodDuration = 5,
        regularity = CycleRegularity.NOT_SURE
    ),
    val insights: InsightsData = CycleCalculator.computeInsights(
        cycles = emptyList(),
        dailyLogs = emptyList(),
        painLogs = emptyList(),
        fallbackCycleLength = 28,
        fallbackPeriodDuration = 5
    ),
) {
    val todayLog: DailyLog? get() = dailyLogs.firstOrNull { it.epochDay == todayEpochDay }
}

data class AiUiState(
    val messages: List<AiChatMessage> = listOf(
        AiChatMessage(
            id = "welcome_ai",
            isUser = false,
            text = "Hi, I am Tuulit. Ask anything about periods, cramps, cycle phases, sleep, or preparing for a doctor visit. I share educational health info and never diagnose.",
            followUpPrompts = listOf(
                "Why do I feel bloated before my period?",
                "How do cramps work?",
                "Why am I so tired this week?",
                "Is an irregular cycle normal?"
            ),
            isSampleResponse = true
        )
    ),
    val isThinking: Boolean = false,
    val explainResponse: AiChatMessage? = null,
    val mythResult: MythCheckResult? = null,
    val appointmentPrep: AppointmentPrepSummary? = null,
    val isLiveCloudAiEnabled: Boolean = false,
)

private data class CoreTrackingBundle(
    val prefs: UserPreferences,
    val cycles: List<CycleRecord>,
    val dailyLogs: List<DailyLog>,
    val painLogs: List<PainLog>,
)

private data class ExtrasBundle(
    val survivalKit: List<SurvivalKitItemEntity>,
    val checklist: List<EmergencyChecklistItemEntity>,
    val achievements: List<AchievementEntity>,
    val contacts: List<TrustedContactEntity>,
    val gameScores: List<GameScoreEntity>,
)

class TuulitViewModel(
    private val repository: TuulitRepository
) : ViewModel() {

    private val _aiState = MutableStateFlow(
        AiUiState(isLiveCloudAiEnabled = repository.aiProvider.isLiveCloudAiEnabled)
    )
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultUtilityListsSeeded()
        }
    }

    private val coreFlow = combine(
        repository.preferences.preferencesFlow,
        repository.cyclesFlow,
        repository.dailyLogsFlow,
        repository.painLogsFlow
    ) { prefs, cycles, dailyLogs, painLogs ->
        CoreTrackingBundle(prefs, cycles, dailyLogs, painLogs)
    }

    private val extrasFlow = combine(
        repository.survivalKitFlow,
        repository.checklistFlow,
        repository.achievementsFlow,
        repository.trustedContactsFlow,
        repository.gameScoresFlow
    ) { kit, checklist, achievements, contacts, scores ->
        ExtrasBundle(kit, checklist, achievements, contacts, scores)
    }

    val uiState: StateFlow<TuulitUiState> = combine(coreFlow, extrasFlow) { core, extras ->
        val today = TuulitRepository.todayEpochDay()
        val prediction = CycleCalculator.calculatePrediction(
            todayEpochDay = today,
            cycles = core.cycles,
            dailyLogs = core.dailyLogs,
            fallbackCycleLength = core.prefs.defaultCycleLength,
            fallbackPeriodDuration = core.prefs.defaultPeriodDuration,
            regularity = core.prefs.cycleRegularity
        )
        val insights = CycleCalculator.computeInsights(
            cycles = core.cycles,
            dailyLogs = core.dailyLogs,
            painLogs = core.painLogs,
            fallbackCycleLength = core.prefs.defaultCycleLength,
            fallbackPeriodDuration = core.prefs.defaultPeriodDuration
        )
        TuulitUiState(
            isReady = true,
            todayEpochDay = today,
            prefs = core.prefs,
            cycles = core.cycles,
            dailyLogs = core.dailyLogs,
            painLogs = core.painLogs,
            survivalKit = extras.survivalKit,
            checklist = extras.checklist,
            achievements = extras.achievements,
            trustedContacts = extras.contacts,
            gameScores = extras.gameScores.associateBy { it.gameId },
            prediction = prediction,
            insights = insights
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TuulitUiState()
    )

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun unlockWithPin(enteredPin: String): Boolean {
        val requiredPin = uiState.value.prefs.privateModePin
        if (requiredPin.isBlank() || enteredPin == requiredPin) {
            _isUnlocked.value = true
            return true
        }
        return false
    }

    fun completeOnboarding(
        displayName: String,
        lastPeriodStartEpochDay: Long?,
        cycleLength: Int,
        periodDuration: Int,
        regularity: CycleRegularity,
        notificationsEnabled: Boolean,
        aiContextOptIn: Boolean,
    ) {
        viewModelScope.launch {
            repository.preferences.completeOnboarding(
                displayName = displayName,
                cycleLength = cycleLength,
                periodDuration = periodDuration,
                regularity = regularity,
                notificationsEnabled = notificationsEnabled,
                aiContextOptIn = aiContextOptIn
            )
            if (lastPeriodStartEpochDay != null) {
                val today = TuulitRepository.todayEpochDay()
                val endDay = if (today - lastPeriodStartEpochDay >= periodDuration) {
                    lastPeriodStartEpochDay + periodDuration - 1
                } else null
                repository.saveCycle(CycleRecord(startEpochDay = lastPeriodStartEpochDay, endEpochDay = endDay))
            }
            _isUnlocked.value = true
        }
    }

    fun logPeriodStart(epochDay: Long) {
        viewModelScope.launch {
            val existing = uiState.value.cycles.firstOrNull { it.startEpochDay == epochDay }
            if (existing == null) {
                repository.saveCycle(CycleRecord(startEpochDay = epochDay, endEpochDay = null))
            }
            val dayLog = uiState.value.dailyLogs.firstOrNull { it.epochDay == epochDay }
                ?: DailyLog(epochDay = epochDay)
            val updatedFlow = if (dayLog.flow == FlowIntensity.NONE) FlowIntensity.MEDIUM else dayLog.flow
            repository.saveDailyLog(dayLog.copy(flow = updatedFlow))
            showBanner("Period start logged for ${TuulitRepository.epochDayToReadable(epochDay)}.")
        }
    }

    fun logPeriodEnd(epochDay: Long) {
        viewModelScope.launch {
            val activeCycle = uiState.value.cycles
                .filter { it.startEpochDay <= epochDay }
                .maxByOrNull { it.startEpochDay }
            if (activeCycle != null) {
                repository.saveCycle(activeCycle.copy(endEpochDay = epochDay))
                showBanner("Period end updated to ${TuulitRepository.epochDayToReadable(epochDay)}.")
            } else {
                val start = epochDay - (uiState.value.prefs.defaultPeriodDuration - 1).coerceAtLeast(0)
                repository.saveCycle(CycleRecord(startEpochDay = start, endEpochDay = epochDay))
                showBanner("Period dates saved.")
            }
        }
    }

    fun deleteCycle(cycleId: Long) {
        viewModelScope.launch {
            repository.deleteCycle(cycleId)
            showBanner("Cycle entry removed.")
        }
    }

    fun saveDailyLog(log: DailyLog) {
        viewModelScope.launch {
            repository.saveDailyLog(log)
            // If flow >= Light and no cycle covers this day, create/extend a cycle cleanly
            if (log.flow.level >= FlowIntensity.LIGHT.level) {
                val nearbyCycle = uiState.value.cycles.firstOrNull { c ->
                    val end = c.endEpochDay ?: (c.startEpochDay + uiState.value.prefs.defaultPeriodDuration)
                    log.epochDay in (c.startEpochDay - 2)..(end + 2)
                }
                if (nearbyCycle == null) {
                    repository.saveCycle(CycleRecord(startEpochDay = log.epochDay, endEpochDay = null))
                } else if (log.epochDay < nearbyCycle.startEpochDay) {
                    repository.saveCycle(nearbyCycle.copy(startEpochDay = log.epochDay))
                } else if (nearbyCycle.endEpochDay != null && log.epochDay > nearbyCycle.endEpochDay) {
                    repository.saveCycle(nearbyCycle.copy(endEpochDay = log.epochDay))
                }
            }
            showBanner("Saved log for ${TuulitRepository.epochDayToReadable(log.epochDay)}.")
        }
    }

    fun deleteDailyLog(epochDay: Long) {
        viewModelScope.launch {
            repository.deleteDailyLog(epochDay)
            showBanner("Daily entry cleared.")
        }
    }

    fun savePainLog(log: PainLog) {
        viewModelScope.launch {
            repository.savePainLog(log)
            showBanner("Pain journal entry saved.")
        }
    }

    fun deletePainLog(id: Long) {
        viewModelScope.launch {
            repository.deletePainLog(id)
            showBanner("Pain entry deleted.")
        }
    }

    fun addCustomMood(mood: String) {
        viewModelScope.launch { repository.preferences.addCustomMood(mood) }
    }

    fun addCustomSymptom(symptom: String) {
        viewModelScope.launch { repository.preferences.addCustomSymptom(symptom) }
    }

    fun setBananaMode(persona: BananaModePersona) {
        viewModelScope.launch { repository.preferences.setBananaMode(persona) }
    }

    fun setTodayIsNotTheDay(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setTodayIsNotTheDay(enabled) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setDarkTheme(enabled) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setReducedMotion(enabled) }
    }

    fun setAiContextOptIn(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setAiContextOptIn(enabled) }
    }

    fun setPrivateModePin(pin: String) {
        viewModelScope.launch {
            repository.preferences.setPrivateModePin(pin)
            _isUnlocked.value = true
            showBanner(if (pin.isBlank()) "Private Mode PIN removed." else "Private Mode PIN enabled.")
        }
    }

    fun updateProfilePreferences(name: String, cycleLen: Int, periodDur: Int, regularity: CycleRegularity) {
        viewModelScope.launch {
            repository.preferences.updateProfilePreferences(name, cycleLen, periodDur, regularity)
            showBanner("Profile preferences updated.")
        }
    }

    fun updateNotifications(
        enabled: Boolean,
        period: Boolean,
        checkIn: Boolean,
        care: Boolean,
        hideLock: Boolean,
        quietHours: String
    ) {
        viewModelScope.launch {
            repository.preferences.updateNotificationSettings(enabled, period, checkIn, care, hideLock, quietHours)
            showBanner("Notification preferences saved.")
        }
    }

    fun saveSleepRoutineNote(note: String) {
        viewModelScope.launch {
            repository.preferences.saveSleepRoutineNote(note)
            showBanner("Sleep routine saved.")
        }
    }

    fun addSurvivalKitItem(title: String, category: String, detail: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.upsertSurvivalKitItem(
                SurvivalKitItemEntity(title = title.trim(), category = category, detail = detail.trim())
            )
            showBanner("Added to Period Survival Kit.")
        }
    }

    fun deleteSurvivalKitItem(id: Long) {
        viewModelScope.launch { repository.deleteSurvivalKitItem(id) }
    }

    fun toggleChecklistItem(item: EmergencyChecklistItemEntity) {
        viewModelScope.launch {
            repository.upsertChecklistItem(item.copy(packed = !item.packed))
        }
    }

    fun addChecklistItem(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.upsertChecklistItem(EmergencyChecklistItemEntity(title = title.trim(), packed = false))
        }
    }

    fun deleteChecklistItem(id: Long) {
        viewModelScope.launch { repository.deleteChecklistItem(id) }
    }

    fun saveTrustedContact(contact: TrustedContactEntity) {
        viewModelScope.launch {
            repository.upsertTrustedContact(contact)
            showBanner("Trusted contact updated.")
        }
    }

    fun deleteTrustedContact(id: Long) {
        viewModelScope.launch {
            repository.deleteTrustedContact(id)
            showBanner("Sharing access revoked and contact removed.")
        }
    }

    fun markRelaxationCompleted() {
        viewModelScope.launch {
            repository.unlockAchievement("relaxation_done", "Calm Moment", "Completed a guided relaxation exercise.")
        }
    }

    fun markQuizCompleted() {
        viewModelScope.launch {
            repository.unlockAchievement("quiz_done", "Curious Mind", "Completed a Tuulit health or wellness quiz.")
        }
    }

    fun markInsightsExplored() {
        viewModelScope.launch {
            repository.unlockAchievement("insights_seen", "Pattern Explorer", "Checked in on your cycle insights.")
        }
    }

    fun recordGameScore(gameId: String, score: Int) {
        viewModelScope.launch {
            repository.recordGameResult(gameId, score)
        }
    }

    // --- AI Actions ---
    fun sendAiMessage(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return
        val userMsg = AiChatMessage(id = "user_${System.currentTimeMillis()}", isUser = true, text = trimmed)
        _aiState.value = _aiState.value.copy(
            messages = _aiState.value.messages + userMsg,
            isThinking = true
        )
        viewModelScope.launch {
            val state = uiState.value
            val recentLogs = state.dailyLogs.take(5)
            val contextSnapshot = AiContextSnapshot(
                cycleDay = state.prediction.currentCycleDay,
                phase = state.prediction.currentPhase,
                recentSymptoms = recentLogs.flatMap { it.symptoms.keys }.distinct(),
                recentMoods = recentLogs.flatMap { it.moods }.distinct(),
                averageEnergy = state.insights.averageEnergy
            )
            val reply = repository.aiProvider.askTuulit(
                question = trimmed,
                context = contextSnapshot,
                includeContext = state.prefs.aiContextOptIn
            )
            _aiState.value = _aiState.value.copy(
                messages = _aiState.value.messages + reply,
                isThinking = false
            )
            repository.unlockAchievement("learned_something", "Body Literacy", "Explored a health topic with Tuulit.")
        }
    }

    fun explainBody(topic: String, followUpStyle: String? = null) {
        val trimmed = topic.trim()
        if (trimmed.isBlank()) return
        _aiState.value = _aiState.value.copy(isThinking = true)
        viewModelScope.launch {
            val res = repository.aiProvider.explainBody(trimmed, followUpStyle)
            _aiState.value = _aiState.value.copy(explainResponse = res, isThinking = false)
            repository.unlockAchievement("learned_something", "Body Literacy", "Explored a health topic with Tuulit.")
        }
    }

    fun checkHealthMyth(claim: String) {
        val trimmed = claim.trim()
        if (trimmed.isBlank()) return
        _aiState.value = _aiState.value.copy(isThinking = true)
        viewModelScope.launch {
            val result = repository.aiProvider.checkHealthMyth(trimmed)
            _aiState.value = _aiState.value.copy(mythResult = result, isThinking = false)
        }
    }

    fun generateAppointmentPrep(concern: String) {
        _aiState.value = _aiState.value.copy(isThinking = true)
        viewModelScope.launch {
            val state = uiState.value
            val summary = repository.aiProvider.buildAppointmentPrep(
                prediction = state.prediction,
                insights = state.insights,
                painLogs = state.painLogs,
                userConcern = concern
            )
            _aiState.value = _aiState.value.copy(appointmentPrep = summary, isThinking = false)
        }
    }

    fun buildExportCsv(): String {
        val state = uiState.value
        return repository.formatExportCsv(state.cycles, state.dailyLogs, state.painLogs)
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.wipeAllData()
            _isUnlocked.value = false
            showBanner("All Tuulit local data has been permanently deleted.")
        }
    }

    class Factory(private val repository: TuulitRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TuulitViewModel(repository) as T
        }
    }
}

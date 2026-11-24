package app.tuulit.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tuulit.data.local.TuulitDatabase
import app.tuulit.domain.Achievement
import app.tuulit.domain.DailyLog
import app.tuulit.domain.FlowIntensity
import app.tuulit.domain.PainLog
import app.tuulit.domain.SymptomSeverity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = android.app.Application::class)
class TrackingRepositoryTest {
    private lateinit var db: TuulitDatabase
    private lateinit var repo: TrackingRepository
    private lateinit var care: CarePlayRepository
    private var changes = 0
    private var clock = 1_000L

    @Before fun setUp() {
        db = TuulitDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repo = TrackingRepository(db.dao(), { changes++ }) { clock++ }
        care = CarePlayRepository(db.dao(), { changes++ }) { clock++ }
    }

    @After fun tearDown() = db.close()

    @Test fun loggingFlowCreatesOneCycleAndExtendsIt() = runTest {
        repo.saveDailyLog(DailyLog(100, flow = FlowIntensity.MEDIUM))
        repo.saveDailyLog(DailyLog(101, flow = FlowIntensity.HEAVY))
        repo.saveDailyLog(DailyLog(102, flow = FlowIntensity.LIGHT))
        val cycles = repo.cycles.first()
        assertEquals(1, cycles.size)
        assertEquals(100L, cycles[0].startEpochDay)
        assertTrue(changes >= 3)
    }

    @Test fun spottingDoesNotStartACycle() = runTest {
        repo.saveDailyLog(DailyLog(100, flow = FlowIntensity.SPOTTING))
        assertTrue(repo.cycles.first().isEmpty())
    }

    @Test fun loggingADayBeforeMovesTheStartEarlier() = runTest {
        repo.saveDailyLog(DailyLog(100, flow = FlowIntensity.MEDIUM))
        repo.saveDailyLog(DailyLog(98, flow = FlowIntensity.LIGHT))
        val cycles = repo.cycles.first()
        assertEquals(1, cycles.size)
        assertEquals(98L, cycles[0].startEpochDay)
    }

    @Test fun removingTheOnlyFlowDayRemovesTheCycle() = runTest {
        repo.saveDailyLog(DailyLog(100, flow = FlowIntensity.MEDIUM))
        repo.saveDailyLog(DailyLog(100, flow = FlowIntensity.NONE, moodScore = 3))
        assertTrue(repo.cycles.first().isEmpty())
        assertEquals(3, repo.dailyLog(100)?.moodScore)
    }

    @Test fun separateMonthsMakeSeparateCyclesAndDuplicatesAreIgnored() = runTest {
        repo.logPeriodStart(100)
        repo.logPeriodStart(100)
        repo.logPeriodStart(128)
        assertEquals(listOf(100L, 128L), repo.cycles.first().map { it.startEpochDay })
    }

    @Test fun emptyLogBecomesTombstoneNotRow() = runTest {
        repo.saveDailyLog(DailyLog(100, moodScore = 4))
        repo.saveDailyLog(DailyLog(100))
        assertNull(repo.dailyLog(100))
        assertTrue(repo.dailyLogs.first().isEmpty())
    }

    @Test fun editCycleValidates() = runTest {
        repo.logPeriodStart(100); repo.logPeriodStart(130)
        val id = repo.cycles.first().first().id
        assertNotNull(repo.editCycle(id, 105, 100))
        assertNotNull(repo.editCycle(id, 127, 130))
        assertNull(repo.editCycle(id, 99, 103))
        assertEquals(99L, repo.cycles.first().first().startEpochDay)
        repo.deleteCycle(id)
        assertEquals(1, repo.cycles.first().size)
    }

    @Test fun pastPeriodFromOnboardingHasNoFakeFlow() = runTest {
        repo.addPastPeriod(start = 90, typicalLength = 5, today = 100)
        val c = repo.cycles.first().single()
        assertEquals(94L, c.endEpochDay)
        assertTrue(repo.dailyLogs.first().isEmpty())
    }

    @Test fun painLogsSaveDeleteAndRestore() = runTest {
        val p = PainLog(id = "p1", epochDay = 100, intensity = 12, location = "Lower back", painType = "", durationHours = 2,
            medicationTaken = "", impactedWorkOrStudy = true, impactedExercise = false, impactedSleep = false, notes = "")
        repo.savePainLog(p)
        assertEquals(10, repo.painLogs.first().single().intensity) // clamped
        repo.deletePainLog("p1")
        assertTrue(repo.painLogs.first().isEmpty())
        repo.savePainLog(p)
        assertEquals(1, repo.painLogs.first().size)
    }

    @Test fun csvExportEscapesAndNeutralisesFormulas() = runTest {
        repo.saveDailyLog(DailyLog(100, flow = FlowIntensity.LIGHT, journalText = "=HYPERLINK(\"x\"), ok", symptoms = mapOf("Cramps" to SymptomSeverity.SEVERE)))
        val csv = repo.exportCsv()
        assertTrue(csv.lines().first().startsWith("type,date"))
        assertTrue(csv, csv.contains("\"'=HYPERLINK(\"\"x\"\"), ok\""))
        assertTrue(csv.contains("Cramps (Severe)"))
    }

    @Test fun sessionsCreditLibraryAndAchievementsOnce() = runTest {
        care.seedLibraryIfEmpty(); care.seedLibraryIfEmpty()
        assertEquals(4, care.comfortItems.first().count { it.kind == "kit" })
        // Fixed IDs, never pushed until touched, so another device can't duplicate the kit.
        assertEquals(CarePlayRepository.SEED_IDS, care.comfortItems.first().filter { it.kind == "kit" }.map { it.id })
        assertTrue(care.comfortItems.first().filter { it.kind == "kit" }.all { it.updatedAt == 0L })
        val first = care.completeSession("slow_breathing", fully = true)
        assertTrue(Achievement.FIRST_SESSION in first)
        care.completeSession("slow_breathing", fully = true)
        val item = care.comfortItems.first().single { it.sessionId == "slow_breathing" }
        assertEquals(2, item.helpedCount)
        assertTrue(care.completeSession("slow_breathing", fully = false).isEmpty())
    }

    @Test fun gameBestScoreOnlyGoesUp() = runTest {
        assertTrue(care.recordGame("banana_catch", 120))
        assertFalse(care.recordGame("banana_catch", 80))
        assertTrue(care.recordGame("banana_catch", 200))
        val s = care.gameScores.first()["banana_catch"]!!
        assertEquals(200, s.best); assertEquals(3, s.plays)
    }
}

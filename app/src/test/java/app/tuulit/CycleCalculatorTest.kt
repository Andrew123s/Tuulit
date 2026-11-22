package app.tuulit

import app.tuulit.domain.CycleCalculator
import app.tuulit.domain.CyclePhase
import app.tuulit.domain.CycleRecord
import app.tuulit.domain.CycleRegularity
import app.tuulit.domain.DailyLog
import app.tuulit.domain.MedicalSafetyGuard
import app.tuulit.domain.PainLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleCalculatorTest {

    @Test
    fun emptyCycles_returnsHonestUnknownState() {
        val prediction = CycleCalculator.calculatePrediction(
            todayEpochDay = 20_000L,
            cycles = emptyList(),
            dailyLogs = emptyList(),
            fallbackCycleLength = 28,
            fallbackPeriodDuration = 5,
            regularity = CycleRegularity.NOT_SURE
        )

        assertFalse(prediction.hasRecordedCycles)
        assertEquals(CyclePhase.UNKNOWN, prediction.currentPhase)
        assertNull(prediction.nextPeriodStartEpochDay)
        assertTrue(prediction.predictedPeriodDays.isEmpty())
    }

    @Test
    fun regularCycles_computesDynamicWeightedAverageAndNextStart() {
        val cycles = listOf(
            CycleRecord(id = "c1", startEpochDay = 19_910L, endEpochDay = 19_914L),
            CycleRecord(id = "c2", startEpochDay = 19_940L, endEpochDay = 19_944L),
            CycleRecord(id = "c3", startEpochDay = 19_970L, endEpochDay = 19_974L),
        )
        // Each cycle is exactly 30 days, period duration 5 days
        val prediction = CycleCalculator.calculatePrediction(
            todayEpochDay = 19_980L,
            cycles = cycles,
            dailyLogs = emptyList(),
            fallbackCycleLength = 28,
            fallbackPeriodDuration = 5,
            regularity = CycleRegularity.REGULAR
        )

        assertTrue(prediction.hasRecordedCycles)
        assertEquals(30, prediction.averageCycleLength)
        assertEquals(5, prediction.averagePeriodDuration)
        assertEquals(11, prediction.currentCycleDay)
        assertEquals(20_000L, prediction.nextPeriodStartEpochDay)
        assertFalse(prediction.isIrregularEstimate)
    }

    @Test
    fun variableCycles_flagsIrregularEstimateWithRange() {
        val cycles = listOf(
            CycleRecord(id = "c1", startEpochDay = 19_900L, endEpochDay = 19_904L),
            CycleRecord(id = "c2", startEpochDay = 19_924L, endEpochDay = 19_928L), // 24 days
            CycleRecord(id = "c3", startEpochDay = 19_960L, endEpochDay = 19_964L), // 36 days
        )
        val prediction = CycleCalculator.calculatePrediction(
            todayEpochDay = 19_970L,
            cycles = cycles,
            dailyLogs = emptyList(),
            fallbackCycleLength = 28,
            fallbackPeriodDuration = 5,
            regularity = CycleRegularity.SOMEWHAT_VARIABLE
        )

        assertTrue(prediction.isIrregularEstimate)
        assertEquals(12, prediction.variabilityDays)
        assertNotNull(prediction.nextPeriodRangeMinDays)
        assertNotNull(prediction.nextPeriodRangeMaxDays)
        assertTrue(prediction.nextPeriodRangeMaxDays!! > prediction.nextPeriodRangeMinDays!!)
    }

    @Test
    fun painPatternDetection_flagsRecurringHighPainAcrossCycles() {
        val cycles = listOf(
            CycleRecord(id = "c1", startEpochDay = 19_910L, endEpochDay = 19_914L),
            CycleRecord(id = "c2", startEpochDay = 19_940L, endEpochDay = 19_944L),
            CycleRecord(id = "c3", startEpochDay = 19_970L, endEpochDay = 19_974L),
        )
        val painLogs = listOf(
            PainLog("p1", 19_911L, 8, "Lower abdomen", "Cramping", 6, "Heat", true, true, false, ""),
            PainLog("p2", 19_941L, 8, "Lower abdomen", "Cramping", 5, "Heat", true, false, true, ""),
            PainLog("p3", 19_971L, 9, "Lower abdomen", "Cramping", 8, "Heat", true, true, true, ""),
        )

        val alert = CycleCalculator.detectPainPattern(cycles, painLogs)
        assertNotNull(alert)
        assertTrue(alert!!.hasRecurringSeverePain)
        assertEquals(3, alert.consecutiveHighPainCycles)
        assertEquals(3, alert.impactedActivitiesCount)
    }

    @Test
    fun insightsInsufficientDataGuard_preventsFabricatedTrends() {
        val insights = CycleCalculator.computeInsights(
            cycles = listOf(CycleRecord(id = "c1", startEpochDay = 19_990L, endEpochDay = 19_994L)),
            dailyLogs = listOf(DailyLog(epochDay = 19_991L, moods = listOf("Calm"))),
            painLogs = emptyList(),
            fallbackCycleLength = 28,
            fallbackPeriodDuration = 5
        )
        assertFalse(insights.hasEnoughDataForTrends)
        assertTrue(insights.dataDrivenNotes.isEmpty())
    }

    @Test
    fun medicalSafetyGuard_detectsUrgentWarningSigns() {
        val warning = MedicalSafetyGuard.inspectForUrgentWarning("I am soaking through a pad every hour and feeling faint")
        assertNotNull(warning)

        val benign = MedicalSafetyGuard.inspectForUrgentWarning("Why do I feel slightly bloated before my period?")
        assertNull(benign)
    }
}

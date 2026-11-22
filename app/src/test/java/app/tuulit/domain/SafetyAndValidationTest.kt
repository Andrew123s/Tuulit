package app.tuulit.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGuardTest {
    @Test fun emergencyPhrasesAreEmergency() {
        listOf("I fainted this morning", "sharp pain on my right side only, one-sided pain", "I'm pregnant and bleeding", "I want to end my life", "chest pain and cramps")
            .forEach { assertEquals(it, UrgencyLevel.EMERGENCY, MedicalSafetyGuard.assess(it).level) }
    }

    @Test fun urgentPhrasesAreUrgent() {
        listOf("soaking through a pad every hour", "I have a fever of 39 c", "worst pain of my life", "vomiting and can't stop all day")
            .forEach { assertEquals(it, UrgencyLevel.URGENT, MedicalSafetyGuard.assess(it).level) }
    }

    @Test fun ordinaryQuestionsAreNotFlagged() {
        listOf("Why do I feel bloated before my period?", "Is a 31 day cycle normal?", "What is the luteal phase?", "Biology 101 question about hormones", "")
            .forEach { assertEquals(it, UrgencyLevel.NONE, MedicalSafetyGuard.assess(it).level) }
    }

    @Test fun legacyHelperReturnsTextOnlyForUrgent() {
        assertNotNull(MedicalSafetyGuard.inspectForUrgentWarning("feeling faint and soaking through"))
        assertNull(MedicalSafetyGuard.inspectForUrgentWarning("no period for 3 months and bleeding after sex"))
    }
}

class ValidationTest {
    @Test fun email() {
        assertNull(Validation.email("maya@example.com"))
        assertNotNull(Validation.email(""))
        assertNotNull(Validation.email("maya@"))
        assertNotNull(Validation.email("maya example.com"))
    }

    @Test fun password() {
        assertNull(Validation.password("bananas2026"))
        assertNotNull(Validation.password("short1"))
        assertNotNull(Validation.password("onlyletterslong"))
        assertNotNull(Validation.password("1234567890"))
    }

    @Test fun pin() {
        assertNull(Validation.pin("4821"))
        assertNull(Validation.pin("482193"))
        assertNotNull(Validation.pin("482"))
        assertNotNull(Validation.pin("1111"))
        assertNotNull(Validation.pin("48a1"))
    }

    @Test fun inviteAndRanges() {
        assertNull(Validation.inviteCode("4K8P2Q"))
        assertNotNull(Validation.inviteCode("4K8P"))
        assertNotNull(Validation.inviteCode("4K8P2-"))
        assertNull(Validation.cycleLength(28)); assertNotNull(Validation.cycleLength(70))
        assertNull(Validation.periodLength(5)); assertNotNull(Validation.periodLength(0))
    }

    @Test fun pinHashRoundTripAndNoPlaintext() {
        val h = PinHasher.hash("4821")
        assertFalse(h.contains("4821"))
        assertTrue(PinHasher.verify("4821", h))
        assertFalse(PinHasher.verify("4822", h))
        assertFalse(PinHasher.verify("4821", "garbage"))
        assertTrue(PinHasher.hash("4821") != h) // salted
    }
}

class TodayAndSuggestionTest {
    private fun prediction(today: Long, starts: List<Long>, ends: List<Long?>) = CycleCalculator.calculatePrediction(
        today, starts.mapIndexed { i, s -> CycleRecord("c$i", s, ends[i]) }, emptyList(), 28, 5, CycleRegularity.REGULAR)

    @Test fun heroWithoutDataAsksToLog() {
        val p = CycleCalculator.calculatePrediction(20_000, emptyList(), emptyList(), 28, 5, CycleRegularity.NOT_SURE)
        val h = TodayHeroBuilder.build(p, 20_000) { it.toString() }
        assertNull(h.number)
        assertEquals("Let's get to know your cycle.", h.lead)
    }

    @Test fun heroCountsDownToNextPeriod() {
        val p = prediction(20_023, listOf(19_944, 19_972, 20_000), listOf(19_948, 19_976, 20_004))
        val h = TodayHeroBuilder.build(p, 20_023) { "d$it" }
        assertEquals("Your period is likely in", h.lead)
        assertEquals("5", h.number)
        assertTrue(h.caption.contains("estimate"))
    }

    @Test fun heroDuringPeriodShowsPeriodDay() {
        val p = prediction(20_001, listOf(19_944, 19_972, 20_000), listOf(19_948, 19_976, null))
        val h = TodayHeroBuilder.build(p, 20_001) { "d$it" }
        assertEquals("Your period", h.lead)
        assertEquals("2", h.number)
    }

    @Test fun crampsSuggestHeatSession() {
        val p = prediction(20_010, listOf(20_000), listOf(20_004))
        val s = SuggestionEngine.forToday(p, DailyLog(20_010, symptoms = mapOf("Cramps" to SymptomSeverity.MODERATE)), 10)
        assertEquals(CareContent.heatAndEase.id, s.sessionId)
    }

    @Test fun careHeroFollowsLog() {
        assertEquals(CareContent.sleepWindDown, CareContent.heroFor(DailyLog(1, sleepQuality = 1), CyclePhase.LUTEAL))
        assertEquals(CareContent.boxBreathing, CareContent.heroFor(DailyLog(1, moods = listOf("Anxious")), CyclePhase.FOLLICULAR))
        assertEquals(CareContent.heatAndEase, CareContent.heroFor(null, CyclePhase.MENSTRUAL))
    }

    @Test fun contentIsConsistent() {
        assertEquals(8, PlayContent.games.size)
        assertTrue(PlayContent.words.all { it.first.length in 4..7 && it.first == it.first.uppercase() })
        PlayContent.quizzes.forEach { q -> q.questions.forEach { assertTrue(it.correctIndex < it.options.size) } }
        assertTrue(CareContent.all.all { it.totalSeconds > 0 })
    }
}

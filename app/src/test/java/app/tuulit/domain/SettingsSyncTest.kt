package app.tuulit.domain

import app.tuulit.data.local.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Settings that follow the person between Android and iPhone (users/{uid}.settings). */
class SettingsSyncTest {
    @Test fun olderRemoteIsIgnored() {
        assertNull(UserSettings(syncedSettingsUpdatedAt = 100).withRemote(mapOf("cycleLength" to 31L, "updatedAt" to 50L)))
    }

    @Test fun newerRemoteAppliesWithLimits() {
        val s = UserSettings(syncedSettingsUpdatedAt = 100).withRemote(mapOf(
            "cycleLength" to 99L, "periodLength" to 4L, "regularity" to "IRREGULAR", "bananaMode" to "GYM",
            "customMoods" to listOf("Wired"), "sosMessages" to emptyList<String>(), "updatedAt" to 200L,
        ))!!
        assertEquals(60, s.cycleLength)
        assertEquals(4, s.periodLength)
        assertEquals(CycleRegularity.IRREGULAR, s.regularity)
        assertEquals(BananaModePersona.GYM, s.bananaMode)
        assertEquals(listOf("Wired"), s.customMoods)
        assertEquals(UserSettings.DEFAULT_SOS, s.sosMessages)
        assertEquals(200L, s.syncedSettingsUpdatedAt)
    }

    @Test fun mapRoundTripsAndMatchesIosKeys() {
        val s = UserSettings(displayName = "Maya", cycleLength = 30, syncedSettingsUpdatedAt = 7)
        val m = s.syncedMap()
        assertEquals(setOf("displayName", "cycleLength", "periodLength", "regularity", "bananaMode", "customMoods", "customSymptoms",
            "sosMessages", "sleepNote", "aiContextOptIn", "updatedAt"), m.keys)
        val back = UserSettings().withRemote(m)!!
        assertEquals("Maya", back.displayName); assertEquals(30, back.cycleLength)
    }
}

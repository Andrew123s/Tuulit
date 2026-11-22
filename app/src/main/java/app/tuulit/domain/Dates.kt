package app.tuulit.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date helpers. Everything in the data layer is stored as epoch days in the device time zone. */
object Dates {
    var clock: Clock = Clock.systemDefaultZone()

    fun today(): Long = LocalDate.now(clock).toEpochDay()
    fun hourNow(): Int = java.time.LocalTime.now(clock).hour
    fun nowMillis(): Long = clock.millis()

    fun date(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    private val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    private val longFmt = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun short(epochDay: Long): String = date(epochDay).format(shortFmt)
    fun day(epochDay: Long): String = date(epochDay).format(dayFmt)
    fun long(epochDay: Long): String = date(epochDay).format(longFmt)
    fun month(d: LocalDate): String = d.format(monthFmt)
    fun iso(epochDay: Long): String = date(epochDay).format(isoFmt)

    fun epochDayOfMillis(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
}

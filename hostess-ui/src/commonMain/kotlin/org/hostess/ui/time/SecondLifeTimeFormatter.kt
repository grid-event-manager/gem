package org.hostess.ui.time

import org.hostess.core.domain.HostessInstant
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

enum class SecondLifeMeridiem {
    AM,
    PM,
}

data class SecondLifeTimeDisplay(
    val hour: Int,
    val minute: Int,
    val meridiem: SecondLifeMeridiem,
) {
    fun label(textCatalogue: HostessTextCatalogue): String {
        val meridiemText = when (meridiem) {
            SecondLifeMeridiem.AM -> textCatalogue.text(HostessTextKey.MeridiemAm)
            SecondLifeMeridiem.PM -> textCatalogue.text(HostessTextKey.MeridiemPm)
        }
        return "${textCatalogue.text(HostessTextKey.SecondLifeTimePrefix)} $hour:${minute.twoDigits()} $meridiemText"
    }
}

object SecondLifeTimeFormatter {
    fun display(instant: HostessInstant): SecondLifeTimeDisplay {
        val utcMillis = instant.epochMilliseconds
        val utcYear = yearFromEpochMillis(utcMillis)
        val offsetHours = if (isPacificDaylightTime(utcMillis, utcYear)) {
            PACIFIC_DAYLIGHT_OFFSET_HOURS
        } else {
            PACIFIC_STANDARD_OFFSET_HOURS
        }
        val localMillis = utcMillis + offsetHours * MILLIS_PER_HOUR
        val minuteOfDay = floorMod(localMillis, MILLIS_PER_DAY) / MILLIS_PER_MINUTE
        val hour24 = (minuteOfDay / MINUTES_PER_HOUR).toInt()
        val minute = (minuteOfDay % MINUTES_PER_HOUR).toInt()
        val hour12 = when (val reduced = hour24 % HOURS_PER_HALF_DAY) {
            0 -> HOURS_PER_HALF_DAY
            else -> reduced
        }
        return SecondLifeTimeDisplay(
            hour = hour12,
            minute = minute,
            meridiem = if (hour24 < HOURS_PER_HALF_DAY) SecondLifeMeridiem.AM else SecondLifeMeridiem.PM,
        )
    }

    private fun isPacificDaylightTime(
        utcMillis: Long,
        utcYear: Int,
    ): Boolean {
        val daylightStart = utcMillisForLocalTransition(
            year = utcYear,
            month = MARCH,
            day = nthSundayOfMonth(utcYear, MARCH, 2),
            utcHour = PACIFIC_DAYLIGHT_START_UTC_HOUR,
        )
        val daylightEnd = utcMillisForLocalTransition(
            year = utcYear,
            month = NOVEMBER,
            day = nthSundayOfMonth(utcYear, NOVEMBER, 1),
            utcHour = PACIFIC_DAYLIGHT_END_UTC_HOUR,
        )
        return utcMillis >= daylightStart && utcMillis < daylightEnd
    }

    private fun utcMillisForLocalTransition(
        year: Int,
        month: Int,
        day: Int,
        utcHour: Int,
    ): Long =
        daysFromCivil(year, month, day) * MILLIS_PER_DAY + utcHour * MILLIS_PER_HOUR

    private fun yearFromEpochMillis(epochMillis: Long): Int =
        civilFromDays(floorDiv(epochMillis, MILLIS_PER_DAY)).year

    private fun nthSundayOfMonth(
        year: Int,
        month: Int,
        occurrence: Int,
    ): Int {
        val firstDayOfWeek = dayOfWeekSundayZero(year, month, day = 1)
        val firstSunday = if (firstDayOfWeek == SUNDAY) {
            1
        } else {
            DAYS_PER_WEEK - firstDayOfWeek + 1
        }
        return firstSunday + DAYS_PER_WEEK * (occurrence - 1)
    }

    private fun dayOfWeekSundayZero(
        year: Int,
        month: Int,
        day: Int,
    ): Int =
        floorMod(daysFromCivil(year, month, day) + THURSDAY, DAYS_PER_WEEK.toLong()).toInt()

    private fun civilFromDays(epochDay: Long): CivilDate {
        val shiftedDay = epochDay + DAYS_TO_CIVIL_EPOCH
        val era = floorDiv(shiftedDay, DAYS_PER_ERA)
        val dayOfEra = shiftedDay - era * DAYS_PER_ERA
        val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val monthPrime = (5 * dayOfYear + 2) / 153
        val month = (monthPrime + if (monthPrime < 10) 3 else -9).toInt()
        val year = (yearOfEra + era * 400 + if (month <= FEBRUARY) 1 else 0).toInt()
        return CivilDate(year)
    }

    private fun daysFromCivil(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        val adjustedYear = year.toLong() - if (month <= FEBRUARY) 1 else 0
        val era = floorDiv(adjustedYear, YEARS_PER_ERA)
        val yearOfEra = adjustedYear - era * YEARS_PER_ERA
        val monthPrime = month.toLong() + if (month > FEBRUARY) -3 else 9
        val dayOfYear = (153 * monthPrime + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * DAYS_PER_ERA + dayOfEra - DAYS_TO_CIVIL_EPOCH
    }

    private fun floorDiv(
        dividend: Long,
        divisor: Long,
    ): Long {
        val quotient = dividend / divisor
        val remainder = dividend % divisor
        return if (remainder != 0L && (dividend xor divisor) < 0L) quotient - 1 else quotient
    }

    private fun floorMod(
        dividend: Long,
        divisor: Long,
    ): Long =
        dividend - floorDiv(dividend, divisor) * divisor

    private data class CivilDate(val year: Int)
}

private fun Int.twoDigits(): String =
    if (this < 10) "0$this" else toString()

private const val PACIFIC_STANDARD_OFFSET_HOURS = -8L
private const val PACIFIC_DAYLIGHT_OFFSET_HOURS = -7L
private const val PACIFIC_DAYLIGHT_START_UTC_HOUR = 10
private const val PACIFIC_DAYLIGHT_END_UTC_HOUR = 9
private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60
private const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR
private const val HOURS_PER_HALF_DAY = 12
private const val DAYS_PER_WEEK = 7
private const val YEARS_PER_ERA = 400L
private const val DAYS_PER_ERA = 146_097L
private const val DAYS_TO_CIVIL_EPOCH = 719_468L
private const val SUNDAY = 0
private const val THURSDAY = 4L
private const val FEBRUARY = 2
private const val MARCH = 3
private const val NOVEMBER = 11

package com.kweather.global.common.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

object DateTimeUtils {
    private val FORECAST_TIMES = listOf("0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300")

    fun getForecastTimes(): List<String> = FORECAST_TIMES

    fun getCurrentApiTime(): String =
        LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMddHH"))

    fun getCurrentDateTimeFormatted(): Pair<String, String> {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val year = now.year
        val month = now.monthValue
        val day = now.dayOfMonth
        val dayOfWeek = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
        val formattedDate = "${year}년 ${month}월 ${day}일 ${dayOfWeek}"
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val formattedTime = now.format(timeFormatter)
        return Pair(formattedDate, formattedTime)
    }

    fun getCurrentHour(): Int = LocalDateTime.now(ZoneId.of("Asia/Seoul")).hour

    fun getBaseDateTimeForShortTerm(): Pair<String, String> {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val hour = now.hour
        val minute = now.minute

        val currentTimeInMinutes = hour * 60 + minute
        val latestTime = FORECAST_TIMES.map { it.toInt() }
            .map { h -> h / 100 * 60 + h % 100 }
            .filter { it <= currentTimeInMinutes + 10 }
            .maxOrNull() ?: 1380
        val latestHour = latestTime / 60
        val latestMinute = latestTime % 60
        val baseTime = String.format("%02d%02d", latestHour, latestMinute)
        val baseDate = if (currentTimeInMinutes < 130 && latestTime == 1380) {
            now.minusDays(1).format(formatter)
        } else {
            now.format(formatter)
        }
        return Pair(baseDate, baseTime)
    }

    fun getBaseDate(): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        return if (now.hour >= 12) now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")) else now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }

    fun getBaseTime(): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val hour = now.hour
        val minute = now.minute
        return when {
            minute < 10 -> String.format("%02d00", if (hour == 0) 23 else hour - 1)
            minute >= 10 && minute < 40 -> String.format("%02d00", hour)
            else -> String.format("%02d30", hour)
        }
    }
}
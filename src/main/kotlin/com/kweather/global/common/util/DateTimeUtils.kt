package com.kweather.global.common.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

object DateTimeUtils {

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

    fun getCurrentHour(): Int {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul")).hour
    }

    fun getBaseDate(): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        return if (now.hour >= 12) {
            now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        } else {
            now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        }
    }

    fun getBaseTime(): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val hour = now.hour
        val minute = now.minute

        return if (hour >= 12 && minute >= 5) {
            "0000"
        } else {
            when {
                minute < 30 -> String.format("%02d00", hour)
                else -> String.format("%02d30", hour)
            }
        }
    }
}
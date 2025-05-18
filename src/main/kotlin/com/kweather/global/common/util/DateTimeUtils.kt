package com.kweather.global.common.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

object DateTimeUtils {

    fun getCurrentDateTimeFormatted(): Pair<String, String> {
        // 한국 시간으로 현재 날짜/시간 가져오기
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

        // 날짜 포맷: "2025년 5월 18일 토요일"
        val year = now.year
        val month = now.monthValue
        val day = now.dayOfMonth
        val dayOfWeek = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
        val formattedDate = "${year}년 ${month}월 ${day}일 ${dayOfWeek}"

        // 시간 포맷: "9:52 PM"
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val formattedTime = now.format(timeFormatter)

        return Pair(formattedDate, formattedTime)
    }

    fun getCurrentHour(): Int {
        // 한국 시간으로 현재 시간(hour) 가져오기
        return LocalDateTime.now(ZoneId.of("Asia/Seoul")).hour
    }
}
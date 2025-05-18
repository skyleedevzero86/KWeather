package com.kweather.global.common.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * 날짜 및 시간 관련 유틸리티 클래스입니다.
 */
object DateTimeUtils {

    /**
     * 현재 한국 시간 기준으로 포맷된 날짜와 시간을 반환합니다.
     *
     * @return (날짜, 시간) 형식의 Pair
     */
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

    /**
     * 현재 한국 시간 기준의 시(hour)를 반환합니다.
     *
     * @return 현재 시각의 시(hour)
     */
    fun getCurrentHour(): Int {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul")).hour
    }
}

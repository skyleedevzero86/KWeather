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

    /**
     * 현재 한국 시간 기준으로 기상청 API에 필요한 baseDate를 반환합니다.
     *
     * @return YYYYMMDD 형식의 문자열 (예: "20250520")
     */
    fun getBaseDate(): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return now.format(formatter)
    }

    /**
     * 현재 한국 시간 기준으로 기상청 API에 필요한 baseTime을 반환합니다.
     * baseTime은 30분 단위로 조정됩니다 (예: 06:07 -> "0600", 06:37 -> "0630").
     *
     * @return HHmm 형식의 문자열 (예: "0600", "0630")
     */
    fun getBaseTime(): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val hour = now.hour
        val minute = now.minute

        // 30분 단위로 조정
        val adjustedHour = if (minute < 30) hour else if (hour < 23) hour + 1 else hour
        val adjustedMinute = if (minute < 30) "00" else "30"

        // HHmm 형식으로 반환
        return String.format("%02d%s", adjustedHour, adjustedMinute)
    }
}
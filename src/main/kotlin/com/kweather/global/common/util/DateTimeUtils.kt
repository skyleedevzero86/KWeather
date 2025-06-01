package com.kweather.global.common.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * 날짜/시간 유틸리티 클래스
 *
 * 한국 시간대(Asia/Seoul)를 기준으로 다양한 날짜/시간 포맷팅 기능을 제공합니다.
 * 주로 기상청 API 호출 시 필요한 시간 형식과 화면 표시용 시간 형식을 제공합니다.
 *
 * @author kylee (궁금하면 500원)
 * @version 1.0
 * @since 2025-05-24
 */
object DateTimeUtils {

    // 단기예보 발표 시간 (02:00, 05:00, 08:00, 11:00, 14:00, 17:00, 20:00, 23:00)
    private val FORECAST_TIMES = listOf("0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300")

    /**
     * 단기예보 발표 시간 리스트를 반환합니다.
     *
     * @return List<String> - 발표 시간 리스트 (HHmm 형식)
     */
    fun getForecastTimes(): List<String> = FORECAST_TIMES

    /**
     * 현재 시간을 API 호출용 형식으로 반환합니다.
     *
     * 기상청 API 등에서 사용하는 yyyyMMddHH 형식의 시간 문자열을 생성합니다.
     * 한국 표준시(KST)를 기준으로 합니다.
     *
     * @return API 호출용 시간 문자열 (예: "2024120115")
     *
     * @see getCurrentDateTimeFormatted 화면 표시용 시간 형식
     */
    fun getCurrentApiTime(): String {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
    }

    /**
     * 현재 날짜와 시간을 사용자 친화적 형식으로 반환합니다.
     *
     * 화면에 표시할 용도로 한국어 날짜 형식과 12시간제 시간 형식을 제공합니다.
     *
     * @return Pair<String, String> - 첫 번째는 날짜, 두 번째는 시간
     *         예: Pair("2024년 12월 1일 일요일", "3:45 PM")
     *
     * @see getCurrentApiTime API 호출용 시간 형식
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
     * 현재 시간(24시간제)을 반환합니다.
     *
     * 한국 표준시 기준으로 현재 시간을 0-23 범위의 정수로 반환합니다.
     *
     * @return 현재 시간 (0-23)
     */
    fun getCurrentHour(): Int {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul")).hour
    }

    /**
     * 기상청 단기예보 API 호출 시 사용할 기준 날짜와 시간을 반환합니다.
     *
     * 단기예보는 매일 02, 05, 08, 11, 14, 17, 20, 23시에 발표되며,
     * 발표 후 10분 뒤부터 데이터가 제공됩니다.
     * 현재 시간을 기준으로 가장 최근의 발표 시간을 계산합니다.
     *
     * @return Pair<String, String> - 첫 번째는 base_date (yyyyMMdd), 두 번째는 base_time (HHmm)
     */
    fun getBaseDateTimeForShortTerm(): Pair<String, String> {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val hour = now.hour
        val minute = now.minute

        // 현재 시간(분 단위)으로 변환
        val currentTimeInMinutes = hour * 60 + minute

        // 가장 최근 발표 시간을 찾음
        val latestTime = FORECAST_TIMES
            .map { it.toInt() }
            .map { h -> h / 100 * 60 + h % 100 } // HHmm -> 분 단위로 변환
            .filter { it <= currentTimeInMinutes + 10 } // 발표 후 10분 뒤부터 유효
            .maxOrNull() ?: 1380 // 기본값: 23:00 (1380분)

        val latestHour = latestTime / 60
        val latestMinute = latestTime % 60
        val baseTime = String.format("%02d%02d", latestHour, latestMinute)

        // base_date 조정: 02:00 발표 시간 전에 요청 시 전날 날짜 사용
        val baseDate = if (currentTimeInMinutes < 130 && latestTime == 1380) { // 02:10 전이고 23:00 데이터일 경우
            now.minusDays(1).format(formatter)
        } else {
            now.format(formatter)
        }

        return Pair(baseDate, baseTime)
    }

    /**
     * 기존 getBaseDate와 getBaseTime은 초단기예보를 위해 유지
     */
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

        return when {
            minute < 10 -> String.format("%02d00", if (hour == 0) 23 else hour - 1)
            minute >= 10 && minute < 40 -> String.format("%02d00", hour)
            else -> String.format("%02d30", hour)
        }
    }
}
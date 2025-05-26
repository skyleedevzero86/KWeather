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
     * 기상청 API 호출 시 사용할 기준 날짜를 반환합니다.
     *
     * 기상청 API의 특성상 정오(12시) 이후에는 전날 데이터를 기준으로 하는
     * 비즈니스 로직을 적용합니다.
     *
     * @return 기준 날짜 문자열 (yyyyMMdd 형식)
     *         - 현재 시간이 12시 이후: 전날 날짜
     *         - 현재 시간이 12시 이전: 오늘 날짜
     *
     * @see getBaseTime 기준 시간 조회
     */
    fun getBaseDate(): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        return if (now.hour >= 12) {
            now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        } else {
            now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        }
    }

    /**
     * 기상청 API 호출 시 사용할 기준 시간을 반환합니다.
     *
     * 기상청 API의 데이터 업데이트 주기에 맞춰 적절한 기준 시간을 계산합니다.
     * - 12시 이후 5분 경과 시: "0000" (자정)
     * - 30분 이전: 현재 시간의 정각 (예: 14:25 → "1400")
     * - 30분 이후: 현재 시간의 30분 (예: 14:35 → "1430")
     *
     * @return 기준 시간 문자열 (HHmm 형식, 예: "1400", "1430", "0000")
     *
     * @see getBaseDate 기준 날짜 조회
     */
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
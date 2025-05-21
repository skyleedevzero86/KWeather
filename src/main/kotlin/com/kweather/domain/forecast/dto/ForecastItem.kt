package com.kweather.domain.forecast.dto

data class ForecastItem(
    val informCode: String? = null,      // 예보 코드 (PM10, PM25, O3 등)
    val informData: String? = null,      // 예보 날짜
    val informGrade: String? = null,     // 지역별 예보 등급
    val informOverall: String? = null,   // 전체 예보 요약
    val informCause: String? = null,     // 예보 원인
    val dataTime: String? = null,        // 발표 시간
    val imageUrl1: String? = null,       // 예보 이미지 URL 1
    val imageUrl2: String? = null,       // 예보 이미지 URL 2
    val imageUrl3: String? = null,       // 예보 이미지 URL 3
    val imageUrl4: String? = null,       // 예보 이미지 URL 4
    val imageUrl5: String? = null,       // 예보 이미지 URL 5
    val imageUrl6: String? = null        // 예보 이미지 URL 6
)
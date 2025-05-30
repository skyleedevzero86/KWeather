package com.kweather.domain.uvi.dto

/**
 * 시간별 날씨 예보 클래스입니다.
 *
 * @property time 시간 (예: 지금, 0시, 3시 등)
 * @property icon 날씨 아이콘
 * @property temperature 기온
 * @property humidity 습도
 */
data class UVIndex(
    val title: String,      // 필수 매개변수
    val icon: String,       // 필수 매개변수
    val status: String,     // 필수 매개변수
    val value: String,
    val measurement: String
)
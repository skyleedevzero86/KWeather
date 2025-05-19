package com.kweather.domain.weather.model

/**
 * 미세먼지 정보 클래스입니다.
 *
 * @property title 항목 이름 (예: 미세먼지)
 * @property icon 상태 아이콘
 * @property status 상태 (예: 좋음)
 * @property value 측정값 (예: 20 ㎍/㎥)
 * @property measurement 단위
 */
data class AirQuality(
    val title: String,
    val icon: String,
    val status: String,
    val value: String,
    val measurement: String
)
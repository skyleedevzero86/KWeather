package com.kweather.domain.weather.model

/**
 * 자외선 지수 정보 클래스입니다.
 *
 * @property title 항목 이름 (예: 초미세먼지)
 * @property icon 상태 아이콘
 * @property status 상태 (예: 좋음)
 * @property value 측정값
 * @property measurement 단위
 */
data class HourlyForecast(
    val time: String,
    val icon: String,
    val temperature: String,
    val humidity: String
)
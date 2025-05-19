package com.kweather.domain.weather.entity

import com.kweather.domain.weather.model.AirQuality
import com.kweather.domain.weather.model.HourlyForecast
import com.kweather.domain.weather.model.UVIndex

/**
 * 날씨 정보를 담는 데이터 클래스입니다.
 *
 * @property date 날짜 (예: 2025년 5월 18일 토요일)
 * @property time 시간 (예: 9:52 PM)
 * @property location 지역명 (예: 한남동)
 * @property currentTemperature 현재 기온
 * @property highLowTemperature 최고/최저 기온
 * @property weatherCondition 날씨 상태 (예: 맑음)
 * @property windSpeed 풍속 정보
 * @property airQuality 미세먼지 정보
 * @property uvIndex 자외선 지수
 * @property hourlyForecast 시간별 날씨 예보 목록
 */
data class Weather(
    val date: String,
    val time: String,
    val location: String,
    val currentTemperature: String,
    val highLowTemperature: String,
    val weatherCondition: String,
    val windSpeed: String,
    val airQuality: AirQuality,
    val uvIndex: UVIndex,
    val hourlyForecast: List<HourlyForecast>
)

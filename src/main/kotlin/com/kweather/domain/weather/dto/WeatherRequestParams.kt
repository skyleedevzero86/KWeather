package com.kweather.domain.weather.dto

// API 요청 파라미터를 위한 데이터 클래스들
data class WeatherRequestParams(
    val baseDate: String,
    val baseTime: String,
    val nx: Int,
    val ny: Int
)
package com.kweather.domain.weather.dto

data class WeatherRequestParams(
    val baseDate: String,
    val baseTime: String,
    val nx: Int,
    val ny: Int
)
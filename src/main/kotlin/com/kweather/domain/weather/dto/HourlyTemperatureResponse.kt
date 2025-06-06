package com.kweather.domain.weather.dto

data class HourlyTemperatureResponse(
    val date: String,
    val temperatures: Map<String, String> // "h1" to "15", "h2" to "16" 형태
)
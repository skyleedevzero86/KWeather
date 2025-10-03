package com.kweather.domain.weather.dto

data class HourlyTemperatureResponse(
    val date: String,
    val temperatures: Map<String, String>
)
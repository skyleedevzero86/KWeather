package com.kweather.domain.weather.dto

data class WeatherItem(
    val baseDate: String? = null,
    val baseTime: String? = null,
    val category: String? = null,
    val nx: Int? = null,
    val ny: Int? = null,
    val obsrValue: String? = null
)
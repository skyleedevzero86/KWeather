package com.kweather.domain.weather.dto

data class WeatherItem(
    val baseDate: String? = null,
    val baseTime: String? = null,
    val category: String? = null,
    val fcstDate: String? = null, // Forecast date
    val fcstTime: String? = null, // Forecast time (e.g., "0600")
    val fcstValue: String? = null,
    val nx: Int? = null,
    val ny: Int? = null
)
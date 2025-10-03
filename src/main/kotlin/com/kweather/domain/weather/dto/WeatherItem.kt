package com.kweather.domain.weather.dto

data class WeatherItem(
    val baseDate: String? = null,
    val baseTime: String? = null,
    val category: String? = null,
    val fcstDate: String? = null,
    val fcstTime: String? = null,
    val fcstValue: String? = null,
    val nx: Int? = null,
    val ny: Int? = null
)
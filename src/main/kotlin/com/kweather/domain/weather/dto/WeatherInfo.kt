package com.kweather.domain.weather.dto

data class WeatherInfo(
    val baseDate: String,
    val baseTime: String,
    val category: String,
    val value: String,
    val unit: String
)
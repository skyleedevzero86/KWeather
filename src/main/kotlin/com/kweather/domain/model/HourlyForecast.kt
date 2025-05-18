package com.kweather.domain.model

data class HourlyForecast(
    val time: String,
    val icon: String,
    val temperature: String,
    val humidity: String
)
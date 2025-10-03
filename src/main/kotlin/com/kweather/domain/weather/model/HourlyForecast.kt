package com.kweather.domain.weather.model

data class HourlyForecast(
    val time: String,
    val icon: String,
    val temperature: String,
    val humidity: String
)
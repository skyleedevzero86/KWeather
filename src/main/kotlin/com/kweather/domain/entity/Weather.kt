package com.kweather.domain.entity

import com.kweather.domain.model.AirQuality
import com.kweather.domain.model.HourlyForecast
import com.kweather.domain.model.UVIndex

data class Weather(
    val date: String,
    val time: String,
    val location: String,
    val currentTemperature: String,
    val highLowTemperature: String,
    val weatherCondition: String,
    val windSpeed: String,
    val airQuality: AirQuality,
    val uvIndex: UVIndex,
    val hourlyForecast: List<HourlyForecast>
)
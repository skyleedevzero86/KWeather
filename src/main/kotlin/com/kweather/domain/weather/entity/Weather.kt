package com.kweather.domain.weather.entity

import com.kweather.domain.weather.model.AirQuality
import com.kweather.domain.weather.model.HourlyForecast
import com.kweather.domain.uvi.dto.UVIndex

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
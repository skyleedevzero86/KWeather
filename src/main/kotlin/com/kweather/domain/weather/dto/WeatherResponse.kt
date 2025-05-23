package com.kweather.domain.weather.dto

import com.kweather.domain.weather.model.Response

data class WeatherResponse(
    val response: Response<WeatherItem>? = null
)
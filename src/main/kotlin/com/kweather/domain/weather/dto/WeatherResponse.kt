package com.kweather.domain.weather.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.kweather.domain.weather.model.Response

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherResponse(
    val response: Response<WeatherItem>? = null
)
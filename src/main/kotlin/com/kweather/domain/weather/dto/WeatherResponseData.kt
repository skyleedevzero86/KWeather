package com.kweather.domain.weather.dto

import com.kweather.domain.weather.model.Body
import com.kweather.domain.weather.model.Header


data class WeatherResponseData(
    val header: Header? = null,
    val body: Body<WeatherItem>? = null
)
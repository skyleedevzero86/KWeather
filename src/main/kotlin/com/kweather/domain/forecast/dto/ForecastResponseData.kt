package com.kweather.domain.forecast.dto

import com.kweather.domain.weather.model.Header

data class ForecastResponseData(
    val header: Header? = null,
    val body: ForecastBody? = null
)
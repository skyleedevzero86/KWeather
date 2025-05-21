package com.kweather.domain.forecast.dto

import com.kweather.domain.weather.dto.Header

data class ForecastResponseData(
    val header: Header? = null,
    val body: ForecastBody? = null
)
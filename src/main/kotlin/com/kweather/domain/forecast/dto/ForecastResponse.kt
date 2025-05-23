package com.kweather.domain.forecast.dto

import com.kweather.domain.weather.model.Response

data class ForecastResponse(
    val response: Response<ForecastItem>? = null
)
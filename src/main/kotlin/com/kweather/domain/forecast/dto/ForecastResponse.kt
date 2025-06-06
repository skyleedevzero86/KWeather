package com.kweather.domain.forecast.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.kweather.domain.weather.model.Response

@JsonIgnoreProperties(ignoreUnknown = true)
data class ForecastResponse(
    val response: Response<ForecastItem>? = null
)
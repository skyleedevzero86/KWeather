package com.kweather.domain.realtime.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.kweather.domain.weather.model.Response

@JsonIgnoreProperties(ignoreUnknown = true)
data class RealTimeDustResponse(
    val response: Response<RealTimeDustItem>? = null
)
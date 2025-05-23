package com.kweather.domain.realtime.dto

import com.kweather.domain.weather.model.Response

data class RealTimeDustResponse(
    val response: Response<RealTimeDustItem>? = null
)
package com.kweather.domain.senta.dto

import com.kweather.domain.weather.model.Response

data class SenTaIndexResponse(
    val response: Response<SenTaIndexItem>? = null
)
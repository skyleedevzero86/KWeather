package com.kweather.domain.uvi.dto

import com.kweather.domain.weather.model.Response

data class UVIndexResponse(
    val response: Response<UVIndexItem>? = null
)
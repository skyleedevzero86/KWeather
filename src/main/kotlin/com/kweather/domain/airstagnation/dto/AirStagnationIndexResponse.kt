package com.kweather.domain.airstagnation.dto

import com.kweather.domain.weather.model.Response

data class AirStagnationIndexResponse(
    val response: Response<AirStagnationIndexItem>? = null
)
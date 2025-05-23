package com.kweather.domain.airstagnation.dto

import com.kweather.domain.weather.model.Body
import com.kweather.domain.weather.model.Header

data class AirStagnationIndexResponseData(
    val header: Header? = null,
    val body: Body<AirStagnationIndexItem>? = null
)
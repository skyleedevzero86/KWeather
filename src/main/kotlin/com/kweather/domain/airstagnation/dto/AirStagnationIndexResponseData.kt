package com.kweather.domain.airstagnation.dto

import com.kweather.domain.weather.model.Header

data class AirStagnationIndexResponseData(
    val header: Header? = null,
    val body: AirStagnationIndexBody? = null
)
package com.kweather.domain.uvi.dto

import com.kweather.domain.weather.model.Header

data class UVIndexResponseData(
    val header: Header? = null,
    val body: UVIndexBody? = null
)
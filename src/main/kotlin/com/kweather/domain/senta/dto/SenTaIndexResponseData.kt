package com.kweather.domain.senta.dto

import com.kweather.domain.weather.model.Header

data class SenTaIndexResponseData(
    val header: Header? = null,
    val body: Body? = null
)
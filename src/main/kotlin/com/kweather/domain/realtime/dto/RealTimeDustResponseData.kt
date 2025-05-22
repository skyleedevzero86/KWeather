package com.kweather.domain.realtime.dto

import com.kweather.domain.weather.model.Header


data class RealTimeDustResponseData(
    val header: Header? = null,
    val body: RealTimeDustBody? = null
)
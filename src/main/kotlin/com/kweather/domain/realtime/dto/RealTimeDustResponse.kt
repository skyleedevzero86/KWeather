package com.kweather.domain.realtime.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RealTimeDustResponse(
    val response: RealTimeDustResponseData? = null
)
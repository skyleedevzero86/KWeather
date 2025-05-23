package com.kweather.domain.senta.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.kweather.domain.weather.model.Header

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseBody(
    val header: Header?,
    val body: Body?
)
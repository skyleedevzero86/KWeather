package com.kweather.domain.uvi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.kweather.domain.weather.model.Header

data class Response(
    @JsonProperty("header")
    val header: Header,
    @JsonProperty("body")
    val body: Body
)
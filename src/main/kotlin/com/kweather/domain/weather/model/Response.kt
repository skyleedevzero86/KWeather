package com.kweather.domain.weather.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Response<T>(
    @JsonProperty("header")
    val header: Header?,
    @JsonProperty("body")
    val body: Body<T>?
)
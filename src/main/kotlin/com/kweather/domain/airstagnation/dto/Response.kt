package com.kweather.domain.airstagnation.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.kweather.domain.weather.model.Header

@JsonIgnoreProperties(ignoreUnknown = true)
data class Response(
    @JsonProperty("header") val header: Header?,
    @JsonProperty("body") val body: Body?
)
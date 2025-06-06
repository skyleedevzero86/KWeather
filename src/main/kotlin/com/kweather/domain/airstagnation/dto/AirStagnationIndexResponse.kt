package com.kweather.domain.airstagnation.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AirStagnationIndexResponse(
    @JsonProperty("response") val response: Response?
)
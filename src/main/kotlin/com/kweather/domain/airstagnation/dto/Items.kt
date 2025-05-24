package com.kweather.domain.airstagnation.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Items(
    @JsonProperty("item") val item: List<AirStagnationIndexItem>?
)
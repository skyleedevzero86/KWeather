package com.kweather.domain.airstagnation.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AirStagnationIndexItems(
    @JsonProperty("item")
    val item: List<AirStagnationIndexItem>? = null
)

package com.kweather.domain.uvi.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UVIndexItems(
    @JsonProperty("item")
    val item: List<UVIndexItem>? = null
)
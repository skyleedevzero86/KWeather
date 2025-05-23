package com.kweather.domain.senta.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SenTaIndexItems(
    @JsonProperty("item")
    val item: List<SenTaIndexItem>? = null
)
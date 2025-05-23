package com.kweather.domain.weather.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Items<T>(
    @JsonProperty("item")
    val item: List<T>? = null
)
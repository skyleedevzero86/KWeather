package com.kweather.domain.weather.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonIgnoreProperties(ignoreUnknown = true)
data class Items<T>(
    @JsonProperty("item")
    @JsonDeserialize(using = ItemsDeserializer::class)
    val item: List<T>? = null
)
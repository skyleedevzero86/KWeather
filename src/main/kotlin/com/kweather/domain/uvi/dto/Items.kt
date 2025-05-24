package com.kweather.domain.uvi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper

data class Items(
    @JsonProperty("item")
    @JacksonXmlElementWrapper(useWrapping = false)
    val item: List<UVIndexItem>
)
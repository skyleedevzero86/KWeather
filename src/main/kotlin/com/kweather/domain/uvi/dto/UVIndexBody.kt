package com.kweather.domain.uvi.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class UVIndexBody(
    @JsonProperty("item")
    val items: List<UVIndexItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)
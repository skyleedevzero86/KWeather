package com.kweather.domain.uvi.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Body(
    val dataType: String?,
    @JsonProperty("items")
    val items: List<UVIndexItem>?,
    val pageNo: Int?,
    val numOfRows: Int?,
    val totalCount: Int?
)
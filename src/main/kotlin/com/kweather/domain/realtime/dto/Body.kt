package com.kweather.domain.realtime.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Body(
    val totalCount: Int?,
    @JsonProperty("items")
    val items: List<RealTimeDustItem>?,
    val pageNo: Int?,
    val numOfRows: Int?
)
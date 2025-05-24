package com.kweather.domain.uvi.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class Body(
    @JsonProperty("dataType")
    val dataType: String,
    @JsonProperty("items")
    val items: Items,
    @JsonProperty("pageNo")
    val pageNo: Int,
    @JsonProperty("numOfRows")
    val numOfRows: Int,
    @JsonProperty("totalCount")
    val totalCount: Int
)
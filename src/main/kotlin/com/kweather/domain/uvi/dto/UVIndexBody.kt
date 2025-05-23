package com.kweather.domain.uvi.dto


import com.fasterxml.jackson.annotation.JsonProperty

data class UVIndexBody(
    @JsonProperty("dataType")
    val dataType: String?,
    @JsonProperty("items")
    val items: UVIndexItems?,
    @JsonProperty("pageNo")
    val pageNo: Int?,
    @JsonProperty("numOfRows")
    val numOfRows: Int?,
    @JsonProperty("totalCount")
    val totalCount: Int?
)
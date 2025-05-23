package com.kweather.domain.airstagnation.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AirStagnationIndexBody(
    @JsonProperty("dataType")
    val dataType: String?,
    @JsonProperty("items")
    val items: AirStagnationIndexItems?,
    @JsonProperty("pageNo")
    val pageNo: Int?,
    @JsonProperty("numOfRows")
    val numOfRows: Int?,
    @JsonProperty("totalCount")
    val totalCount: Int?
)
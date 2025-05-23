package com.kweather.domain.senta.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SenTaIndexBody(
    @JsonProperty("dataType")
    val dataType: String?,
    @JsonProperty("items")
    val items: SenTaIndexItems?,
    @JsonProperty("pageNo")
    val pageNo: Int?,
    @JsonProperty("numOfRows")
    val numOfRows: Int?,
    @JsonProperty("totalCount")
    val totalCount: Int?
)
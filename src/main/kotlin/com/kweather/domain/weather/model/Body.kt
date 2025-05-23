package com.kweather.domain.weather.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Body<T>(
    @JsonProperty("dataType")
    val dataType: String?,
    @JsonProperty("items")
    val items: Items<T>?,
    @JsonProperty("pageNo")
    val pageNo: Int?,
    @JsonProperty("numOfRows")
    val numOfRows: Int?,
    @JsonProperty("totalCount")
    val totalCount: Int?
)
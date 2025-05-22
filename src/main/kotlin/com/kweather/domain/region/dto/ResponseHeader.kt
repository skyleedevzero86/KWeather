package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseHeader(
    @JsonProperty("totalCount")
    val totalCount: Int? = null,

    @JsonProperty("numOfRows")
    val numOfRows: String? = null,

    @JsonProperty("pageNo")
    val pageNo: String? = null,

    @JsonProperty("type")
    val type: String? = null,

    @JsonProperty("RESULT")
    val result: ResponseResult? = null
)
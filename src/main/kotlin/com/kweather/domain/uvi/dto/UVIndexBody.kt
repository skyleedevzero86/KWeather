package com.kweather.domain.uvi.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UVIndexBody(
    val items: List<UVIndexItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)
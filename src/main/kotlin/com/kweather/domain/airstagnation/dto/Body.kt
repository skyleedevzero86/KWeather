package com.kweather.domain.airstagnation.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Body(
    val dataType: String?,
    val items: List<AirStagnationIndexItem>?,
    val pageNo: Int?,
    val numOfRows: Int?,
    val totalCount: Int?
)
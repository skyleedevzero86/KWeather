package com.kweather.domain.airstagnation.dto

data class AirStagnationIndexBody(
    val items: List<AirStagnationIndexItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)
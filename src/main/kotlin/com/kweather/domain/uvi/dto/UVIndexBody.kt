package com.kweather.domain.uvi.dto

data class UVIndexBody(
    val items: List<UVIndexItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)
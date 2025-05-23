package com.kweather.domain.realtime.dto

data class RealTimeDustBody(
    val items: List<RealTimeDustItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)
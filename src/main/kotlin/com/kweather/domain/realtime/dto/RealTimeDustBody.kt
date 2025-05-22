package com.kweather.domain.realtime.dto

data class RealTimeDustBody(
    val totalCount: Int? = null,
    val items: List<RealTimeDustItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null
)
package com.kweather.domain.senta.dto

data class SenTaIndexBody(
    val items: List<SenTaIndexItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)
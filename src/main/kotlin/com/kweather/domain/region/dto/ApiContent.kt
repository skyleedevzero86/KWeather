package com.kweather.domain.region.dto

data class ApiContent(
    val totalCount: Int = 0,
    val resultCode: String = "",
    val resultMsg: String = "",
    val numOfRows: Int = 0,
    val pageNo: Int = 0,
    val rows: List<ApiRow>? = emptyList()
)
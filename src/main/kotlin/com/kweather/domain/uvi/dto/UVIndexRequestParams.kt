package com.kweather.domain.uvi.dto

data class UVIndexRequestParams(
    val areaNo: String,
    val time: String,
    val pageNo: Int = 1,
    val numOfRows: Int = 10,
    val dataType: String = "json"
)
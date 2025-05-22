package com.kweather.domain.realtime.dto

data class RealTimeDustRequestParams(
    val sidoName: String,
    val pageNo: Int = 1,
    val numOfRows: Int = 100,
    val returnType: String = "json",
    val ver: String = "1.0"
)
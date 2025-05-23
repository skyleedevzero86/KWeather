package com.kweather.domain.senta.dto

data class SenTaIndexRequestParams(
    val areaNo: String,
    val time: String,
    val requestCode: String = "A41",
    val pageNo: Int = 1,
    val numOfRows: Int = 10,
    val dataType: String = "json"
)
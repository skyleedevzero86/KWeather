package com.kweather.domain.airstagnation.dto

data class AirStagnationIndexRequestParams(
    val areaNo: String,
    val time: String,
    val pageNo: Int = 1,
    val numOfRows: Int = 10,
    val dataType: String = "json",
    val requestCode: String? = null
)
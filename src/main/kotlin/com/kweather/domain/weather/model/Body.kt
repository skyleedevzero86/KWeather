package com.kweather.domain.weather.model

data class Body(
    val dataType: String? = null,
    val items: Items? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)
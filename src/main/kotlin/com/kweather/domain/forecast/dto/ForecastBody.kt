package com.kweather.domain.forecast.dto

data class ForecastBody(
    val totalCount: Int? = null,
    val items: List<ForecastItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null
)
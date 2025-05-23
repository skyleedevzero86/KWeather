package com.kweather.domain.forecast.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Body(
    val totalCount: Int?,
    val items: List<ForecastItem>?,
    val pageNo: Int?,
    val numOfRows: Int?
)
package com.kweather.domain.weather.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Body<T>(
    val totalCount: Int? = null,
    val items: List<T>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val dataType: String? = null
)
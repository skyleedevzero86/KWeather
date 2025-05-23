package com.kweather.domain.senta.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Body(
    val dataType: String?,
    val items: List<SenTaIndexItem>?,
    val pageNo: Int?,
    val numOfRows: Int?,
    val totalCount: Int?
)
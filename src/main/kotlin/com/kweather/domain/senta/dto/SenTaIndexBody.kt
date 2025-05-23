package com.kweather.domain.senta.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SenTaIndexBody(
    val items: List<SenTaIndexItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)
package com.kweather.domain.forecast.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ForecastItem(
    val informCode: String? = null,
    val informData: String? = null,
    val informGrade: String? = null,
    val informOverall: String? = null,
    val informCause: String? = null,
    val dataTime: String? = null,
    val imageUrl1: String? = null,
    val imageUrl2: String? = null,
    val imageUrl3: String? = null,
    val imageUrl4: String? = null,
    val imageUrl5: String? = null,
    val imageUrl6: String? = null
)
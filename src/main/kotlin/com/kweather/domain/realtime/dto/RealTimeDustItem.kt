package com.kweather.domain.realtime.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RealTimeDustItem(
    val sidoName: String? = null,
    val stationName: String? = null,
    val pm10Value: String? = null,
    val pm25Value: String? = null,
    val pm10Grade: String? = null,
    val pm25Grade: String? = null,
    val dataTime: String? = null,
    val so2Grade: String? = null,
    val coValue: String? = null,
    val khaiValue: String? = null,
    val o3Grade: String? = null
)
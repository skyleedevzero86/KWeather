package com.kweather.domain.realtime.dto

data class RealTimeDustInfo(
    val sidoName: String,
    val stationName: String,
    val dataTime: String,
    val pm10Value: String,
    val pm10Grade: String,
    val pm25Value: String,
    val pm25Grade: String
)
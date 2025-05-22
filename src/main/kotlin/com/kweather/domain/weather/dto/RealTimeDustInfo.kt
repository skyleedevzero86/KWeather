package com.kweather.domain.weather.dto

data class RealTimeDustInfo(
    val sidoName: String,          // 시도 이름
    val stationName: String,       // 측정소 이름
    val pm10Value: String,         // 미세먼지 값
    val pm10Grade: String,         // 미세먼지 등급
    val pm25Value: String,         // 초미세먼지 값
    val pm25Grade: String,         // 초미세먼지 등급
    val dataTime: String           // 측정 시간
)
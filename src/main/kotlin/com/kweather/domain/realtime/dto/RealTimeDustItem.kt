package com.kweather.domain.realtime.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
data class RealTimeDustItem(
    val sidoName: String? = null,          // 시도 이름 (예: 서울)
    val stationName: String? = null,       // 측정소 이름 (예: 중구)
    val pm10Value: String? = null,         // 미세먼지(PM10) 농도 (μg/m³)
    val pm25Value: String? = null,         // 초미세먼지(PM2.5) 농도 (μg/m³)
    val pm10Grade: String? = null,         // 미세먼지(PM10) 등급 (1: 좋음, 2: 보통, 3: 나쁨, 4: 매우나쁨)
    val pm25Grade: String? = null,         // 초미세먼지(PM2.5) 등급
    val dataTime: String? = null,           // 측정 시간 (예: 2020-11-14 14시)
    val so2Grade: String? = null,         // SO2 등급
    val coValue: String? = null,          // CO 농도
    val khaiValue: String? = null,        // 종합 대기질 지수
    val o3Grade: String? = null,          // 오존 등급
)
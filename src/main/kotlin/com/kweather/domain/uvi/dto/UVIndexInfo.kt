package com.kweather.domain.uvi.dto

data class UVIndexInfo(
    val date: String,
    val values: Map<String, String> // 시간별 자외선 지수 값 (예: "h0" to "1")
)
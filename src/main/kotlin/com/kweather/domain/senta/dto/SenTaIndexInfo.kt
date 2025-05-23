package com.kweather.domain.senta.dto

data class SenTaIndexInfo(
    val date: String,
    val values: Map<String, String> // 시간별 체감온도 값 (예: "h0" to "1")
)
package com.kweather.domain.airstagnation.dto

data class AirStagnationIndexInfo(
    val date: String,
    val values: Map<String, String> // 시간별 대기정체지수 값 (예: "h3" to "50")
)
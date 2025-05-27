package com.kweather.domain.uvi.dto

data class UVIndexInfo(
    val date: String,
    val values: Map<String, Float>
)
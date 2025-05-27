package com.kweather.domain.senta.dto

data class SenTaIndexInfo(
    val date: String,
    val values: Map<String, Float>
)
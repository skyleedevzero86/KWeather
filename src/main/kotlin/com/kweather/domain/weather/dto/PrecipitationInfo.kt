package com.kweather.domain.weather.dto

data class PrecipitationInfo(
    val date: String,
    val values: Map<String, Float>
)
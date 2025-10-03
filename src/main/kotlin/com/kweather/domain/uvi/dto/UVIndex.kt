package com.kweather.domain.uvi.dto

data class UVIndex(
    val title: String,
    val icon: String,
    val status: String,
    val value: String,
    val measurement: String
)
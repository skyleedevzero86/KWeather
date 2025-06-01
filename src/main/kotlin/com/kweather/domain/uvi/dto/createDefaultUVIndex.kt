package com.kweather.domain.uvi.dto

import java.time.LocalDateTime
import java.time.ZoneId

private fun createDefaultUVIndex(): UVIndex {
    val currentHour = LocalDateTime.now(ZoneId.of("Asia/Seoul")).hour
    val (uvValue, uvStatus) = when (currentHour) {
        in 6..8 -> "3" to "보통"
        in 9..15 -> "7" to "높음"
        in 16..18 -> "4" to "보통"
        else -> "0" to "낮음"
    }
    return UVIndex(
        title = "자외선 지수",
        icon = when (uvStatus) {
            "낮음" -> "uv-low"
            "보통" -> "uv-moderate"
            "높음" -> "uv-high"
            else -> "uv-low"
        },
        status = uvStatus,
        value = uvValue,
        measurement = "UV Index"
    )
}
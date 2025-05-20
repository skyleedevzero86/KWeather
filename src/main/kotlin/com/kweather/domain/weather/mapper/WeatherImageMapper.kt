package com.kweather.domain.weather.mapper

import org.springframework.stereotype.Component

@Component
class WeatherImageMapper {
    fun mapToImageText(category: String, value: String): String {
        return when (category) {
            "PTY" -> mapPrecipitationType(value.toInt())
            "T1H" -> mapTemperature(value.toDouble())
            "REH" -> mapHumidity(value.toInt())
            "RN1" -> mapRainfall(value.toDouble())
            "WSD" -> mapWindSpeed(value.toDouble())
            else -> "지원되지 않는 카테고리: $category, 값: $value"
        }
    }

    private fun mapPrecipitationType(value: Int): String {
        return when (value) {
            0 -> """
                ☀️ 맑음
                --------
                """
            1 -> """
                🌧️ 비
                --------
                """
            2 -> """
                🌨️ 눈/비
                --------
                """
            3 -> """
                🌨️ 눈
                --------
                """
            else -> "알 수 없는 강수형태: $value"
        }
    }

    private fun mapTemperature(value: Double): String {
        return """
            🌡️ 온도: ${value}°C
            --------
            """
    }

    private fun mapHumidity(value: Int): String {
        return """
            💧 습도: ${value}%
            --------
            """
    }

    private fun mapRainfall(value: Double): String {
        return """
            💦 1시간 강수량: ${value}mm
            --------
            """
    }

    private fun mapWindSpeed(value: Double): String {
        return """
            💨 풍속: ${value}m/s
            --------
            """
    }
}
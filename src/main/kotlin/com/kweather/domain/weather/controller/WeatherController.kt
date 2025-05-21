package com.kweather.domain.weather.controller

import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.weather.model.AirQuality
import com.kweather.domain.weather.model.HourlyForecast
import com.kweather.domain.weather.model.UVIndex
import com.kweather.domain.weather.service.WeatherService
import com.kweather.global.common.util.DateTimeUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Controller
class WeatherController(
    private val weatherService: WeatherService
) {

    @GetMapping("/")
    fun getWeather(model: Model): String {
        val (date, time) = DateTimeUtils.getCurrentDateTimeFormatted()
        val hour = DateTimeUtils.getCurrentHour()

        val weatherData = Weather(
            date = date,
            time = time,
            location = "한남동 (용산구)",
            currentTemperature = "-1.8°C",
            highLowTemperature = "-5°C / -1°C",
            weatherCondition = "맑음",
            windSpeed = "1km/초(남서) m/s 0",
            airQuality = AirQuality("미세먼지", "yellow-smiley", "좋음", "20 ㎍/㎥", "㎍/㎥"),
            uvIndex = UVIndex("초미세먼지", "yellow-smiley", "좋음", "8 ㎍/㎥", "㎍/㎥"),
            hourlyForecast = listOf(
                HourlyForecast("지금", "moon", "-1.8°C", "34%"),
                HourlyForecast("0시", "moon", "-6°C", "55%"),
                HourlyForecast("3시", "moon", "-6°C", "60%"),
                HourlyForecast("6시", "moon", "-7°C", "67%"),
                HourlyForecast("9시", "sun", "-6°C", "55%")
            )
        )

        val currentDate = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val previousHour = now.minusHours(1) // 현재 15:46 KST, 1시간 전은 14:46 -> "O3"
        val informCode = when (previousHour.hour) {
            in 0..5 -> "PM10"
            in 6..11 -> "PM25"
            in 12..17 -> "O3"  // 현재 시각 기준 "O3"로 설정
            else -> "PM10"
        }

        val dustForecast = weatherService.getDustForecast(currentDate, informCode)

        val categorizedForecast = dustForecast.map { forecast ->
            val regions = forecast.grade.split(",").map { it.trim() }.associate {
                val parts = it.split(":")
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else "N/A" to "N/A"
            }
            Triple(
                regions.filter { it.value == "좋음" }.keys.toList().ifEmpty { listOf("N/A") },
                regions.filter { it.value == "보통" }.keys.toList().ifEmpty { listOf("N/A") },
                regions.filter { it.value == "나쁨" }.keys.toList().ifEmpty { listOf("N/A") }
            )
        }

        val timeOfDay = when {
            hour in 6..11 -> " ( 아침 )"
            hour in 12..17 -> " ( 낮 )"  // 현재 15:46 KST, "낮"으로 표시
            hour in 18..23 -> " ( 저녁 )"
            else -> "새벽 (밤)"
        }

        model.addAttribute("timeOfDay", timeOfDay)
        model.addAttribute("weather", weatherData)
        model.addAttribute("dustForecast", if (dustForecast.isEmpty()) null else dustForecast)
        model.addAttribute("categorizedForecast", if (categorizedForecast.isEmpty()) null else categorizedForecast)
        return "domain/weather/weather"
    }
}
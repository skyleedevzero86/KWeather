package com.kweather.domain.weather.controller

import com.kweather.domain.weather.dto.HourlyTemperatureResponse
import com.kweather.domain.weather.dto.WeatherResponse
import com.kweather.domain.weather.service.GeneralWeatherService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RestController
class HourlyRestController(
    private val generalWeatherService: GeneralWeatherService
) {
    private val logger = LoggerFactory.getLogger(HourlyRestController::class.java)

    @Value("\${weather.default.area-no:1100000000}")
    private lateinit var defaultAreaNo: String
    @GetMapping("/api/weather")
    @ResponseBody
    fun getWeather(@RequestParam nx: Int, @RequestParam ny: Int): WeatherResponse {
        return generalWeatherService.getUltraShortWeather(nx, ny)
    }


    @GetMapping("/api/hourly-temperature")
    @ResponseBody
    fun getHourlyTemperature(): HourlyTemperatureResponse {
        return try {
            val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            val apiTime = now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"))

            val hourlyTemps = generalWeatherService.getHourlyTemperature(defaultAreaNo, apiTime)
            val temperatures = hourlyTemps.mapValues { it.value.toString() }

            HourlyTemperatureResponse(
                date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                temperatures = temperatures.filterKeys { it.startsWith("h") }
            )
        } catch (e: Exception) {
            logger.error("시간별 온도 데이터 가져오기 실패: ${e.message}", e)
            HourlyTemperatureResponse(
                date = "20250607",
                temperatures = (1..72).associate { "h$it" to "15" }
            )
        }
    }
}
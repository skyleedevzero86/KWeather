package com.kweather.domain.weather.controller

import com.kweather.domain.weather.dto.WeatherInfo
import com.kweather.domain.weather.service.WeatherService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class Weather2(
    private val weatherService: WeatherService
) {
    @GetMapping("/weather/ultra-short")
    fun getUltraShortWeather(
        @RequestParam("nx", defaultValue = "55") nx: Int,
        @RequestParam("ny", defaultValue = "127") ny: Int
    ): ResponseEntity<List<WeatherInfo>> {
        return try {
            val response = weatherService.getUltraShortWeather(nx, ny)
            val weatherInfoList = weatherService.parseWeatherData(response)
            if (weatherInfoList.isEmpty()) {
                ResponseEntity.noContent().build()
            } else {
                ResponseEntity.ok(weatherInfoList)
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.status(500).body(listOf(WeatherInfo("", "", "ERROR", e.message ?: "Unknown error", "")))
        }
    }
}
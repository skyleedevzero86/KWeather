package com.kweather.domain.weather.controller

import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.weather.dto.WeatherInfo
import com.kweather.domain.weather.service.WeatherService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class Weather2(
    private val weatherService: WeatherService
) {

    @GetMapping("/weather/dust-forecast")
    fun getDustForecast(
        @RequestParam("searchDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) searchDate: LocalDate,
        @RequestParam("informCode", defaultValue = "PM10") informCode: String
    ): ResponseEntity<List<ForecastInfo>> {
        return try {
            val response = weatherService.getDustForecast(searchDate.toString(), informCode)
            val forecastInfoList = weatherService.parseDustForecast(response)
            if (forecastInfoList.isEmpty()) {
                ResponseEntity.noContent().build() // 204 No Content for empty response
            } else {
                ResponseEntity.ok(forecastInfoList)
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.status(500).body(
                listOf(
                    ForecastInfo(
                        date = "",
                        type = "",
                        overall = "",
                        cause = "",
                        grade = "ERROR: ${e.message ?: "Unknown error"}",
                        dataTime = "",
                        imageUrls = emptyList()
                    )
                )
            )
        }
    }
}
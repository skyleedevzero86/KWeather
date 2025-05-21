package com.kweather.domain.weather.controller

import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.weather.service.WeatherService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
class Weather2(
    private val weatherService: WeatherService
) {

    @GetMapping("/weather/dust-forecast")
    fun getDustForecast(
        @RequestParam("searchDate", required = false) searchDate: String? = null,
        @RequestParam("informCode", defaultValue = "PM10") informCode: String
    ): ResponseEntity<List<ForecastInfo>> {
        return try {
            // searchDate가 없으면 현재 날짜를 사용
            val formattedDate = searchDate?.let {
                parseDate(it)
            } ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val response = weatherService.getDustForecast(formattedDate, informCode)
            if (response.isEmpty()) {
                ResponseEntity.noContent().build() // 204 No Content for empty response
            } else {
                ResponseEntity.ok(response)
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
        } catch (e: DateTimeParseException) {
            ResponseEntity.status(400).body(
                listOf(
                    ForecastInfo(
                        date = "",
                        type = "",
                        overall = "",
                        cause = "",
                        grade = "ERROR: Invalid date format. Use yyyy-MM-dd or yyyyMMdd",
                        dataTime = "",
                        imageUrls = emptyList()
                    )
                )
            )
        }
    }

    /**
     * 날짜 문자열을 파싱하여 yyyy-MM-dd 형식으로 변환
     */
    private fun parseDate(dateString: String): String {
        return try {
            // yyyy-MM-dd 형식 시도
            val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd
            LocalDate.parse(dateString, isoFormatter).format(isoFormatter)
        } catch (e: DateTimeParseException) {
            // yyyyMMdd 형식 시도
            val customFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            LocalDate.parse(dateString, customFormatter).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }
}
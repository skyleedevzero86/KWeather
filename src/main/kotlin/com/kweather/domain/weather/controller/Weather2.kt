package com.kweather.domain.weather.controller

import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.weather.service.GeneralWeatherService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
class Weather2(
    private val weatherService: GeneralWeatherService
) {

    @GetMapping("/weather/dust-forecast")
    fun getDustForecast(
        @RequestParam("searchDate", required = false) searchDate: String?,
        @RequestParam("informCode", defaultValue = "PM10") informCode: String
    ): ResponseEntity<List<ForecastInfo>> =
        runCatching {
            val formattedDate = searchDate
                ?.let(::parseDate)
                ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

            weatherService.getDustForecast(formattedDate, informCode)
        }.fold(
            onSuccess = { forecastList ->
                if (forecastList.isEmpty())
                    ResponseEntity.noContent().build()
                else
                    ResponseEntity.ok(forecastList)
            },
            onFailure = { ex -> ResponseEntity.status(getHttpStatus(ex))
                .body(listOf(errorForecastInfo(ex)))
            }
        )

    private fun parseDate(dateString: String): String =
        runCatching {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        }.recoverCatching {
            LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyyMMdd"))
        }.getOrElse {
            throw DateTimeParseException("Invalid date format", dateString, 0)
        }.format(DateTimeFormatter.ISO_LOCAL_DATE)

    private fun errorForecastInfo(ex: Throwable): ForecastInfo =
        ForecastInfo(
            date = "",
            type = "",
            overall = "",
            cause = "",
            grade = "ERROR: ${ex.message ?: "Unknown error"}",
            dataTime = "",
            imageUrls = emptyList()
        )

    private fun getHttpStatus(ex: Throwable): Int = when (ex) {
        is DateTimeParseException -> 400
        is IllegalStateException -> 500
        else -> 500
    }
}
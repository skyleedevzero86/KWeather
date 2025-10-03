package com.kweather.domain.weather.controller

import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.locations.service.GeoService
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
    private val weatherService: GeneralWeatherService,
    private val geoService: GeoService
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
    @GetMapping("/weather/real-time-dust")
    fun getRealTimeDust(
        @RequestParam("sidoName", defaultValue = "서울") sidoName: String
    ): ResponseEntity<List<com.kweather.domain.realtime.dto.RealTimeDustInfo>> =
        runCatching {
            weatherService.getRealTimeDust(sidoName)
        }.fold(
            onSuccess = { dustList ->
                if (dustList.isEmpty())
                    ResponseEntity.noContent().build()
                else
                    ResponseEntity.ok(dustList)
            },
            onFailure = { ex -> ResponseEntity.status(500)
                .body(emptyList())
            }
        )

    @GetMapping("/weather/geo")
    fun getGeoCoordinates(@RequestParam address: String): Map<String, String> {
        return geoService.getCoordinates(address).fold(
            { errorMessage ->
                println("오류 발생: $errorMessage")
                mapOf(
                    "latitude" to "0.0",
                    "longitude" to "0.0",
                    "error" to errorMessage
                )
            },
            { (latitude, longitude) ->
                println("위도: ${latitude.toDouble().toInt()}")
                println("경도: ${longitude.toDouble().toInt()}")
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                )
            }
        )
    }
}
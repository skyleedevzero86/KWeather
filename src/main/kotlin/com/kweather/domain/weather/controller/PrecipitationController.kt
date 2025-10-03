package com.kweather.domain.weather.controller

import com.kweather.domain.weather.dto.PrecipitationInfo
import com.kweather.domain.weather.service.GeneralWeatherService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RestController
class PrecipitationController(private val generalWeatherService: GeneralWeatherService) {

    private val logger = LoggerFactory.getLogger(PrecipitationController::class.java)

    @GetMapping("/api/precipitation")
    fun getPrecipitationData(): Map<String, Any> {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val apiTime = now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val precipitationData = generalWeatherService.getPrecipitationData("1100000000", apiTime)

        val labels = precipitationData.firstOrNull()?.values?.keys?.map { it.replace("h", "5/30 ") + ":00" } ?: emptyList()
        val precipitations = precipitationData.firstOrNull()?.values?.values?.map { it.toFloat() } ?: emptyList()

        return mapOf(
            "labels" to labels,
            "precipitations" to precipitations
        )
    }
}
package com.kweather.domain.weather.controller

import com.kweather.domain.senta.service.SenTaIndexService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/chart")
class SenTaIndexApiChartController(
    private val senTaIndexService: SenTaIndexService,
    @Value("\${weather.default.area-no:1100000000}") private val defaultAreaNo: String
) {
    private val logger = LoggerFactory.getLogger(SenTaIndexApiChartController::class.java)

    data class ChartDataResponse(
        val startDate: String,
        val temperatures: List<Float>
    )

    @GetMapping("/temperature")
    fun getTemperatureChartData(): ChartDataResponse {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val apiTime = now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"))

        val senTaIndexData = try {
            val currentMonth = now.monthValue
            if (currentMonth in 5..9) {
                val result = senTaIndexService.getSenTaIndex(defaultAreaNo, apiTime)
                logger.info("SenTaIndexData for chart: $result")
                result
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("체감온도 데이터 가져오기 실패: ${e.message}", e)
            emptyList()
        }

        val firstSenTaIndex = senTaIndexData.firstOrNull()
        val temperatures = if (firstSenTaIndex != null && firstSenTaIndex.values.isNotEmpty()) {
            (1..32).mapNotNull { hour ->
                firstSenTaIndex.values["h$hour"]
            }
        } else {
            listOf(18.0f, 17.0f, 18.0f, 18.0f, 18.0f, 18.0f, 18.0f, 20.0f, 21.0f, 22.0f)
        }

        return ChartDataResponse(apiTime, temperatures)
    }
}
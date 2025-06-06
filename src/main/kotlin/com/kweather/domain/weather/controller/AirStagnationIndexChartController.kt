package com.kweather.domain.weather.controller

import com.kweather.domain.airstagnation.service.AirStagnationIndexService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/airchart")
class AirStagnationIndexChartController {

    @Autowired
    lateinit var airStagnationIndexService: AirStagnationIndexService

    @GetMapping("/air-stagnation")
    fun getAirStagnationChartData(): Map<String, Any> {
        return try {
            val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
            val areaNo = "1100000000" // 서울 지역 번호

            val dataList = airStagnationIndexService.getAirStagnationIndex(areaNo, currentTime)
                ?: return mapOf(
                    "startDate" to currentTime,
                    "indices" to listOf(50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0, 90.0, 95.0)
                )

            if (dataList.isEmpty()) {
                return mapOf(
                    "startDate" to currentTime,
                    "indices" to listOf(50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0, 90.0, 95.0)
                )
            }

            val data = dataList[0]
            val startDate = data.date
            val hours = listOf(3, 6, 9, 12, 15, 18, 21, 24, 27, 30)

            val indices = hours.map { hour ->
                val key = "h$hour"
                data.values[key]?.toDoubleOrNull() ?: 0.0
            }

            mapOf(
                "startDate" to startDate,
                "indices" to indices
            )
        } catch (e: Exception) {
            // 예외 발생 시 기본 데이터 반환
            mapOf(
                "startDate" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH")),
                "indices" to listOf(50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0, 90.0, 95.0)
            )
        }
    }
}
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
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val areaNo = "1100000000" // 서울 지역 번호

        val dataList = airStagnationIndexService.getAirStagnationIndex(areaNo, currentTime)
            ?: throw RuntimeException("대기정체지수 데이터를 불러올 수 없습니다.")

        if (dataList.isEmpty()) {
            throw RuntimeException("대기정체지수 데이터가 비어 있습니다.")
        }

        val data = dataList[0]
        val startDate = data.date
        val hours = listOf(3, 6, 9, 12, 15, 18, 21, 24, 27, 30)

        val indices = hours.map { hour ->
            val key = "h$hour"
            data.values[key]?.toDoubleOrNull() ?: 0.0
        }

        return mapOf(
            "startDate" to startDate,
            "indices" to indices
        )
    }
}

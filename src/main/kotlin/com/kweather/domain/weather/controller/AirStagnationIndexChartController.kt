package com.kweather.domain.weather.controller

import com.kweather.domain.airstagnation.dto.AirStagnationIndexInfo
import com.kweather.domain.airstagnation.service.AirStagnationIndexService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/api/airchart")
class AirStagnationIndexChartController {

    @Autowired
    lateinit var airStagnationIndexService: AirStagnationIndexService

    @GetMapping("/air-stagnation")
    fun getAirStagnationChartData(): Map<String, Any> {
        // 현재 시간을 기준으로 데이터 요청
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        // 지역 번호 (예: 서울, 하드코딩 대신 실제 구현에서는 동적으로 설정해야 함)
        val areaNo = "1100000000" // 서울

        val airStagnationData = airStagnationIndexService.getAirStagnationIndex(areaNo, currentTime)

        if (airStagnationData.isEmpty()) {
            throw RuntimeException("대기정체지수 데이터를 불러올 수 없습니다.")
        }

        // 첫 번째 데이터만 사용 (최신 데이터)
        val data = airStagnationData[0]
        val startDate = data.date // 예: "2025052900"

        // 차트 데이터 준비
        val hours = intArrayOf(3, 6, 9, 12, 15, 18, 21, 24, 27, 30)
        val indices = DoubleArray(hours.size) { i ->
            val key = "h${hours[i]}"
            val value = data.values[key]
            value?.toDoubleOrNull() ?: 0.0
        }

        return mapOf(
            "startDate" to startDate,
            "indices" to indices
        )
    }
}
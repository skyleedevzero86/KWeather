package com.kweather.domain.weather.controller

import com.kweather.domain.airstagnation.dto.AirStagnationIndexInfo
import com.kweather.domain.airstagnation.service.AirStagnationIndexService
import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.realtime.dto.RealTimeDustInfo
import com.kweather.domain.region.service.RegionService
import com.kweather.domain.senta.dto.SenTaIndexInfo
import com.kweather.domain.senta.service.SenTaIndexService
import com.kweather.domain.uvi.dto.UVIndex
import com.kweather.domain.uvi.dto.UVIndexInfo
import com.kweather.domain.uvi.service.UVIndexService
import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.weather.model.AirQuality
import com.kweather.domain.weather.model.HourlyForecast
import com.kweather.domain.weather.model.WeatherDataProvider
import com.kweather.domain.weather.dto.PrecipitationInfo
import com.kweather.domain.weather.service.GeneralWeatherService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Controller
class WeatherController(
    private val generalWeatherService: GeneralWeatherService,
    private val uvIndexService: UVIndexService,
    private val senTaIndexService: SenTaIndexService,
    private val airStagnationIndexService: AirStagnationIndexService,
    private val regionService: RegionService,
    @Value("\${weather.default.region.name:한남동}") private val defaultRegionName: String,
    @Value("\${weather.default.region.district:용산구}") private val defaultRegionDistrict: String,
    @Value("\${weather.default.sido:서울특별시}") private val defaultSido: String,
    @Value("\${weather.default.area-no:1100000000}") private val defaultAreaNo: String
) {
    private val logger = LoggerFactory.getLogger(WeatherController::class.java)
    private val restTemplate = RestTemplate()

    private inner class LiveWeatherDataProvider : WeatherDataProvider {
        override fun getWeatherData(date: String, time: String): Weather = try {
            generalWeatherService.buildWeatherEntity(60, 127, defaultSido)
        } catch (e: Exception) {
            logger.error("실시간 날씨 데이터 가져오기 실패: ${e.message}", e)
            createDefaultWeatherData(date, time)
        }

        override fun getDustForecastData(date: String, informCode: String): List<ForecastInfo> = try {
            generalWeatherService.getDustForecast(date, informCode)
        } catch (e: Exception) {
            logger.error("미세먼지 예보 데이터 가져오기 실패: ${e.message}", e)
            emptyList()
        }

        override fun getRealTimeDustData(sidoName: String): List<RealTimeDustInfo> = try {
            generalWeatherService.getRealTimeDust(sidoName)
        } catch (e: Exception) {
            logger.error("실시간 미세먼지 데이터 가져오기 실패: ${e.message}", e)
            emptyList()
        }

        override fun getUVIndexData(areaNo: String, time: String): List<UVIndexInfo> = try {
            uvIndexService.getUVIndex(areaNo, time)
        } catch (e: Exception) {
            logger.error("자외선 지수 데이터 가져오기 실패: ${e.message}", e)
            emptyList()
        }

        override fun getSenTaIndexData(areaNo: String, time: String): List<SenTaIndexInfo> = try {
            val currentMonth = LocalDateTime.now(ZoneId.of("Asia/Seoul")).monthValue
            if (currentMonth in 5..9) senTaIndexService.getSenTaIndex(areaNo, time) else emptyList()
        } catch (e: Exception) {
            logger.error("체감온도 데이터 가져오기 실패: ${e.message}", e)
            emptyList()
        }

        override fun getAirStagnationIndexData(areaNo: String, time: String): List<AirStagnationIndexInfo> = try {
            airStagnationIndexService.getAirStagnationIndex(areaNo, time)
        } catch (e: Exception) {
            logger.error("대기정체지수 데이터 가져오기 실패: ${e.message}", e)
            emptyList()
        }

        override fun getPrecipitationData(areaNo: String, time: String): List<PrecipitationInfo> {
            val today = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val values = (0..23).associate { "h$it" to "${it % 5}.0 mm" }
            return listOf(PrecipitationInfo(today, values))
        }
    }

    @GetMapping("/")
    fun index(): String = "index"

    @GetMapping("/weather")
    fun getWeather(@RequestParam(value = "location", required = false) location: String?, model: Model): String {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val dateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).split(" ")
        val hour = now.hour

        val weatherDataProvider = LiveWeatherDataProvider()
        val (finalSido, finalSgg, finalUmd) = location?.split(" ")?.let {
            listOf(normalizeSido(it[0]), it[1], it[2])
        } ?: listOf(normalizeSido(defaultSido), defaultRegionDistrict, defaultRegionName)

        // WeatherDataProvider 대신 GeneralWeatherService 사용
        val weatherData = generalWeatherService.buildWeatherEntity(60, 127, finalSido)

        val currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val apiTime = now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val informCode = when (hour) {
            in 0..5 -> "PM10"
            in 6..11 -> "PM25"
            in 12..17 -> "O3"
            else -> "PM10"
        }

        val dustForecast = weatherDataProvider.getDustForecastData(currentDate, informCode)
        val realTimeDust = weatherDataProvider.getRealTimeDustData(finalSido)
        val errorMessage = if (realTimeDust.isEmpty()) "실시간 미세먼지 데이터 오류" else null

        logger.info("realTimeDust 데이터: $realTimeDust") // 디버깅 로그 추가

        val uvIndexData = weatherDataProvider.getUVIndexData(defaultAreaNo, apiTime)
        val senTaIndexData = weatherDataProvider.getSenTaIndexData(defaultAreaNo, apiTime)
        val airStagnationIndexData = weatherDataProvider.getAirStagnationIndexData(defaultAreaNo, apiTime)
        val precipitationData = weatherDataProvider.getPrecipitationData(defaultAreaNo, apiTime)

        val uvHoursSequence = (0..75 step 3).toList()
        val sentaHoursSequence = (1..31).toList()
        val asiHoursSequence = (3..78 step 3).toList()
        val precipHoursSequence = (0..23).toList()

        val categorizedForecast = dustForecast.map { forecast ->
            val regions = forecast.grade.split(",").map { it.trim().split(":") }
                .filter { it.size == 2 }.associate { it[0] to it[1] }
            Triple(
                regions.filterValues { it == "좋음" }.keys.toList().ifEmpty { listOf("N/A") },
                regions.filterValues { it == "보통" }.keys.toList().ifEmpty { listOf("N/A") },
                regions.filterValues { it == "나쁨" }.keys.toList().ifEmpty { listOf("N/A") }
            )
        }

        val timeOfDay = when (hour) {
            in 6..11 -> " ( 아침 )"
            in 12..17 -> " ( 낮 )"
            in 18..23 -> " ( 저녁 )"
            else -> " ( 새벽 )"
        }

        val sidos = restTemplate.getForObject("http://localhost:8090/api/regions/sidos", List::class.java) as List<String>
        val sggs = if (sidos.contains(finalSido)) restTemplate.getForObject("http://localhost:8090/api/regions/sggs?sido=$finalSido", List::class.java) as List<String> else emptyList()
        val umds = if (sggs.contains(finalSgg)) restTemplate.getForObject("http://localhost:8090/api/regions/umds?sido=$finalSido&sgg=$finalSgg", List::class.java) as List<String> else emptyList()

        model.addAllAttributes(mapOf(
            "sidos" to sidos, "selectedSido" to finalSido,
            "sggs" to sggs, "selectedSgg" to finalSgg,
            "umds" to umds, "selectedUmd" to finalUmd,
            "timeOfDay" to timeOfDay, "weather" to weatherData,
            "dustForecast" to dustForecast.ifEmpty { null },
            "realTimeDust" to realTimeDust.ifEmpty { null },
            "uvIndexData" to uvIndexData.ifEmpty { null },
            "senTaIndexData" to senTaIndexData.ifEmpty { null },
            "airStagnationIndexData" to airStagnationIndexData.ifEmpty { null },
            "precipitationData" to precipitationData.ifEmpty { null },
            "categorizedForecast" to categorizedForecast.ifEmpty { null },
            "errorMessage" to errorMessage,
            "uvHoursSequence" to uvHoursSequence,
            "sentaHoursSequence" to sentaHoursSequence,
            "asiHoursSequence" to asiHoursSequence,
            "precipHoursSequence" to precipHoursSequence
        ))

        return "domain/weather/weather"
    }

    private fun normalizeSido(sido: String) = when (sido) {
        "서울" -> "서울특별시"
        "경기" -> "경기도"
        "인천" -> "인천광역시"
        "강원" -> "강원특별자치도"
        "충북" -> "충청북도"
        "충남" -> "충청남도"
        "대전" -> "대전광역시"
        "세종" -> "세종특별자치시"
        "전북" -> "전북특별자치도"
        "전남" -> "전라남도"
        "광주" -> "광주광역시"
        "경북" -> "경상북도"
        "경남" -> "경상남도"
        "대구" -> "대구광역시"
        "부산" -> "부산광역시"
        "울산" -> "울산광역시"
        "제주" -> "제주특별자치도"
        else -> sido
    }

    private fun createDefaultWeatherData(date: String, time: String) = Weather(
        date = date,
        time = time,
        location = "$defaultRegionName ($defaultRegionDistrict)",
        currentTemperature = "-1.8°C",
        highLowTemperature = "-5°C / -1°C",
        weatherCondition = "맑음",
        windSpeed = "1km/초(남서) m/s 0",
        airQuality = AirQuality("미세먼지", "yellow-smiley", "좋음", "20 ㎍/㎥", "㎍/㎥"),
        uvIndex = UVIndex("초미세먼지", "yellow-smiley", "좋음", "8 ㎍/㎥", "㎍/㎥"),
        hourlyForecast = (0..7).map {
            val hour = (LocalDateTime.now().hour + it * 3) % 24
            HourlyForecast(
                time = if (it == 0) "지금" else "${hour}시",
                icon = if (hour in 6..18) "sun" else "moon",
                temperature = "-${it + 1}°C",
                humidity = "${50 + it * 5}%"
            )
        }
    )
}
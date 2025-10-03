package com.kweather.domain.weather.controller

import com.kweather.domain.airstagnation.dto.AirStagnationIndexInfo
import com.kweather.domain.airstagnation.service.AirStagnationIndexService
import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.realtime.dto.RealTimeDustInfo
import com.kweather.domain.senta.dto.SenTaIndexInfo
import com.kweather.domain.senta.service.SenTaIndexService
import com.kweather.domain.uvi.dto.UVIndex
import com.kweather.domain.uvi.dto.UVIndexInfo
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
    private val senTaIndexService: SenTaIndexService,
    private val airStagnationIndexService: AirStagnationIndexService,
    @Value("\${weather.default.region.name:한남동}") private val defaultRegionName: String,
    @Value("\${weather.default.region.district:용산구}") private val defaultRegionDistrict: String,
    @Value("\${weather.default.sido:서울특별시}") private val defaultSido: String,
    @Value("\${weather.default.area-no:1100000000}") private val defaultAreaNo: String,
    @Value("\${weather.default.nx:60}") private val defaultNx: Int,
    @Value("\${weather.default.ny:127}") private val defaultNy: Int
) {
    private val logger = LoggerFactory.getLogger(WeatherController::class.java)
    private val restTemplate = RestTemplate()

    private inner class LiveWeatherDataProvider : WeatherDataProvider {
        override fun getWeatherData(date: String, time: String): Weather = try {
            generalWeatherService.buildWeatherEntity(defaultNx, defaultNy, defaultSido)
        } catch (e: Exception) {
            createDefaultWeatherData(date, time)
        }

        override fun getDustForecastData(date: String, informCode: String): List<ForecastInfo> = try {
            generalWeatherService.getDustForecast(date, informCode)
        } catch (e: Exception) {
            emptyList()
        }

        override fun getRealTimeDustData(sidoName: String): List<RealTimeDustInfo> = try {
            generalWeatherService.getRealTimeDust(sidoName)
        } catch (e: Exception) {
            emptyList()
        }

        override fun getSenTaIndexData(areaNo: String, time: String): List<SenTaIndexInfo> = try {
            val currentMonth = LocalDateTime.now(ZoneId.of("Asia/Seoul")).monthValue
            if (currentMonth in 5..9) senTaIndexService.getSenTaIndex(areaNo, time) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        override fun getAirStagnationIndexData(areaNo: String, time: String): List<AirStagnationIndexInfo> = try {
            airStagnationIndexService.getAirStagnationIndex(areaNo, time)
        } catch (e: Exception) {
            emptyList()
        }

        override fun getPrecipitationData(areaNo: String, time: String): List<PrecipitationInfo> {
            val today = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val values = (0..23).associate { "h$it" to (it % 5).toFloat() }
            return listOf(PrecipitationInfo(today, values))
        }

        override fun getUVIndexData(areaNo: String, time: String): List<UVIndexInfo> = try {
            val today = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val rawValues = mapOf("h12" to "3", "h13" to "4", "h14" to "3")
            val convertedValues = rawValues.mapValues { it.value.toFloatOrNull() ?: 0.0f }
            listOf(UVIndexInfo(date = today, values = convertedValues))
        } catch (e: Exception) {
            emptyList()
        }

        override fun getHourlyTemperatureData(areaNo: String, time: String): Map<String, Any> = try {
            val hourlyTemps = generalWeatherService.getHourlyTemperature(areaNo, time)
            hourlyTemps.mapValues { it.value.toString() }
        } catch (e: Exception) {
            (1..72).associate { "h$it" to "15" }
        }
    }

    @GetMapping("/")
    fun index(): String = "index"

    @GetMapping("/weather")
    fun getWeather(@RequestParam(value = "location", required = false) location: String?, model: Model): String {
        return try {
            val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            val dateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).split(" ")
            val hour = now.hour

            val weatherDataProvider = LiveWeatherDataProvider()
            val (finalSido, finalSgg, finalUmd) = location?.split(" ")?.let { parts ->
                if (parts.size >= 3) listOf(normalizeSido(parts[0]), parts[1], parts[2])
                else listOf(normalizeSido(defaultSido), defaultRegionDistrict, defaultRegionName)
            } ?: listOf(normalizeSido(defaultSido), defaultRegionDistrict, defaultRegionName)

            val weatherData = try {
                generalWeatherService.buildWeatherEntity(defaultNx, defaultNy, finalSido)
            } catch (e: Exception) {
                createDefaultWeatherData(dateTime[0], dateTime[1])
            }

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

            val senTaIndexData = weatherDataProvider.getSenTaIndexData(defaultAreaNo, apiTime)
            val airStagnationIndexData = weatherDataProvider.getAirStagnationIndexData(defaultAreaNo, apiTime)
            val precipitationData = weatherDataProvider.getPrecipitationData(defaultAreaNo, apiTime)
            val uvIndexData = weatherDataProvider.getUVIndexData(defaultAreaNo, apiTime)

            val sentaHoursSequence = (1..31).toList()
            val asiHoursSequence = (3..78 step 3).toList()
            val precipHoursSequence = (0..23).toList()

            val categorizedForecast = dustForecast.map { forecast ->
                val regions = forecast.grade.split(",").map { it.trim() }
                    .filter { it.contains(":") }
                    .map { it.split(":") }
                    .filter { it.size == 2 }
                    .associate { it[0].trim() to it[1].trim() }

                Triple(
                    regions.filterValues { it == "좋음" }.keys.toList().ifEmpty { listOf("없음") },
                    regions.filterValues { it == "보통" }.keys.toList().ifEmpty { listOf("없음") },
                    regions.filterValues { it == "나쁨" }.keys.toList().ifEmpty { listOf("없음") }
                )
            }

            val timeOfDay = when (hour) {
                in 6..11 -> " ( 아침 )"
                in 12..17 -> " ( 낮 )"
                in 18..23 -> " ( 저녁 )"
                else -> " ( 새벽 )"
            }

            val sidos = try {
                restTemplate.getForObject("http://localhost:8090/api/regions/sidos", List::class.java) as? List<String> ?: emptyList()
            } catch (e: Exception) {
                listOf(finalSido)
            }

            val sggs = try {
                if (sidos.contains(finalSido)) {
                    restTemplate.getForObject("http://localhost:8090/api/regions/sggs?sido=$finalSido", List::class.java) as? List<String> ?: emptyList()
                } else emptyList()
            } catch (e: Exception) {
                listOf(finalSgg)
            }

            val umds = try {
                if (sggs.contains(finalSgg)) {
                    restTemplate.getForObject("http://localhost:8090/api/regions/umds?sido=$finalSido&sgg=$finalSgg", List::class.java) as? List<String> ?: emptyList()
                } else emptyList()
            } catch (e: Exception) {
                listOf(finalUmd)
            }

            model.addAllAttributes(mapOf(
                "sidos" to sidos, "selectedSido" to finalSido,
                "sggs" to sggs, "selectedSgg" to finalSgg,
                "umds" to umds, "selectedUmd" to finalUmd,
                "timeOfDay" to timeOfDay, "weather" to weatherData,
                "dustForecast" to dustForecast.ifEmpty { null },
                "realTimeDust" to realTimeDust.ifEmpty { null },
                "senTaIndexData" to senTaIndexData.ifEmpty { null },
                "airStagnationIndexData" to airStagnationIndexData,
                "precipitationData" to precipitationData.ifEmpty { null },
                "uvIndexData" to uvIndexData.ifEmpty { null },
                "categorizedForecast" to categorizedForecast.ifEmpty { null },
                "errorMessage" to errorMessage,
                "sentaHoursSequence" to sentaHoursSequence,
                "asiHoursSequence" to asiHoursSequence,
                "precipHoursSequence" to precipHoursSequence
            ))

            "domain/weather/weather"
        } catch (e: Exception) {
            model.addAttribute("errorMessage", "날씨 정보를 불러오는 중 오류가 발생했습니다.")
            "error"
        }
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
        currentTemperature = "0°C",
        highLowTemperature = "-3°C / 3°C",
        weatherCondition = "맑음",
        windSpeed = "0 m/s",
        airQuality = AirQuality(
            title = "미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "20",
            measurement = "㎍/㎥",
            title2 = "초미세먼지",
            status2 = "좋음",
            value2 = "10",
            measurement2 = "㎍/㎥"
        ),
        hourlyForecast = (0..7).map {
            val hour = (LocalDateTime.now().hour + it * 3) % 24
            HourlyForecast(
                time = if (it == 0) "지금" else "${hour}시",
                icon = if (hour in 6..18) "sun" else "moon",
                temperature = "0°C",
                humidity = "50%"
            )
        },
        uvIndex = createDefaultUVIndex()
    )

    private fun createDefaultUVIndex(): UVIndex {
        return UVIndex(
            title = "자외선 지수",
            icon = "uv-moderate",
            status = "보통",
            value = "3",
            measurement = "UV Index"
        )
    }
}
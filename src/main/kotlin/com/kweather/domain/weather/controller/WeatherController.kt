package com.kweather.domain.weather.controller

import com.kweather.domain.airstagnation.dto.AirStagnationIndexInfo
import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.realtime.dto.RealTimeDustInfo
import com.kweather.domain.senta.dto.SenTaIndexInfo
import com.kweather.domain.uvi.dto.UVIndexInfo
import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.weather.model.AirQuality
import com.kweather.domain.weather.model.HourlyForecast
import com.kweather.domain.weather.model.WeatherDataProvider
import com.kweather.domain.weather.dto.PrecipitationInfo
import com.kweather.global.common.util.DateTimeUtils
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
import arrow.core.Option
import arrow.core.Some
import arrow.core.None
import com.kweather.domain.airstagnation.service.AirStagnationIndexService
import com.kweather.domain.senta.service.SenTaIndexService
import com.kweather.domain.uvi.service.UVIndexService
import com.kweather.domain.weather.service.GeneralWeatherService
import com.kweather.domain.region.service.RegionService
import com.kweather.domain.uvi.dto.UVIndex

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

    private inner class DefaultWeatherDataProvider : WeatherDataProvider {
        override fun getWeatherData(date: String, time: String): Weather =
            createDefaultWeatherData(date, time)

        override fun getDustForecastData(date: String, informCode: String): List<ForecastInfo> =
            runCatching { generalWeatherService.getDustForecast(date, informCode) }
                .getOrElse { e ->
                    logger.error("미세먼지 예보 데이터 가져오기 실패: ${e.message}", e)
                    emptyList()
                }

        override fun getRealTimeDustData(sidoName: String): List<RealTimeDustInfo> = emptyList()

        override fun getUVIndexData(areaNo: String, time: String): List<UVIndexInfo> = emptyList()

        override fun getSenTaIndexData(areaNo: String, time: String): List<SenTaIndexInfo> = emptyList()

        override fun getAirStagnationIndexData(areaNo: String, time: String): List<AirStagnationIndexInfo> = emptyList()

        override fun getPrecipitationData(areaNo: String, time: String): List<PrecipitationInfo> {
            val today = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val values = (0..23).associate { "h$it" to "${(it % 5).toDouble()} mm" }
            return listOf(PrecipitationInfo(today, values))
        }
    }

    private inner class LiveWeatherDataProvider : WeatherDataProvider {
        override fun getWeatherData(date: String, time: String): Weather =
            runCatching { generalWeatherService.buildWeatherEntity(60, 127) }
                .getOrElse { e ->
                    logger.error("실시간 날씨 데이터 가져오기 실패: ${e.message}", e)
                    createDefaultWeatherData(date, time)
                }

        override fun getDustForecastData(date: String, informCode: String): List<ForecastInfo> =
            runCatching { generalWeatherService.getDustForecast(date, informCode) }
                .getOrElse { e ->
                    logger.error("미세먼지 예보 데이터 가져오기 실패: ${e.message}", e)
                    emptyList()
                }

        override fun getRealTimeDustData(sidoName: String): List<RealTimeDustInfo> =
            runCatching { generalWeatherService.getRealTimeDust(sidoName) }
                .getOrElse { e ->
                    logger.error("실시간 미세먼지 데이터 가져오기 실패: ${e.message}", e)
                    emptyList()
                }

        override fun getUVIndexData(areaNo: String, time: String): List<UVIndexInfo> =
            runCatching { uvIndexService.getUVIndex(areaNo, time) }
                .getOrElse { e ->
                    logger.error("자외선 지수 데이터 가져오기 실패: ${e.message}", e)
                    emptyList()
                }

        override fun getSenTaIndexData(areaNo: String, time: String): List<SenTaIndexInfo> =
            runCatching {
                val currentMonth = LocalDateTime.now(ZoneId.of("Asia/Seoul")).monthValue
                if (currentMonth in 5..9) senTaIndexService.getSenTaIndex(areaNo, time) else emptyList()
            }.getOrElse { e ->
                logger.error("여름철 체감온도 데이터 가져오기 실패: ${e.message}", e)
                emptyList()
            }

        override fun getAirStagnationIndexData(areaNo: String, time: String): List<AirStagnationIndexInfo> =
            runCatching { airStagnationIndexService.getAirStagnationIndex(areaNo, time) }
                .getOrElse { e ->
                    logger.error("대기정체지수 데이터 가져오기 실패: ${e.message}", e)
                    emptyList()
                }

        override fun getPrecipitationData(areaNo: String, time: String): List<PrecipitationInfo> {
            val today = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val values = (0..23).associate { "h$it" to "${(it % 5).toDouble()} mm" }
            return listOf(PrecipitationInfo(today, values))
        }
    }

    @GetMapping("/")
    fun index(): String {
        return "index"
    }

    @GetMapping("/weather")
    fun getWeather(@RequestParam(value = "location", required = false) location: String?, model: Model): String {
        val dateTimeInfo = getCurrentDateTimeInfo()
        val (date, time, hour) = dateTimeInfo

        val weatherDataProvider = createWeatherDataProvider(useRealTimeData = true)

        // 기본 위치 또는 요청된 위치 설정
        val (finalSido, finalSgg, finalUmd) = if (location != null) {
            val parts = location.split(" ")
            // 시도 값을 정규화
            val normalizedSido = when (parts[0]) {
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
                else -> parts[0]
            }
            Triple(normalizedSido, parts[1], parts[2])
        } else {
            val normalizedDefaultSido = when (defaultSido) {
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
                else -> defaultSido
            }
            Triple(normalizedDefaultSido, defaultRegionDistrict, defaultRegionName)
        }

        // Weather 객체를 수동으로 생성하여 location 업데이트
        val baseWeatherData = weatherDataProvider.getWeatherData(date, time)
        val weatherData = Weather(
            date = baseWeatherData.date,
            time = baseWeatherData.time,
            location = "$finalUmd ($finalSgg)",
            currentTemperature = baseWeatherData.currentTemperature,
            highLowTemperature = baseWeatherData.highLowTemperature,
            weatherCondition = baseWeatherData.weatherCondition,
            windSpeed = baseWeatherData.windSpeed,
            airQuality = baseWeatherData.airQuality,
            uvIndex = baseWeatherData.uvIndex,
            hourlyForecast = baseWeatherData.hourlyForecast
        )

        val currentDate = getCurrentFormattedDate()
        val apiTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val informCode = determineInformCode(hour)
        val dustForecast = weatherDataProvider.getDustForecastData(currentDate, informCode)

        val realTimeDust = weatherDataProvider.getRealTimeDustData(finalSido)
        val errorMessage = if (realTimeDust.isEmpty()) "실시간 미세먼지 데이터를 불러올 수 없습니다. API 키가 유효하지 않을 수 있습니다. 설정을 확인해 주세요." else null

        val uvIndexData = weatherDataProvider.getUVIndexData(defaultAreaNo, apiTime)
        val senTaIndexData = weatherDataProvider.getSenTaIndexData(defaultAreaNo, apiTime)
        val airStagnationIndexData = weatherDataProvider.getAirStagnationIndexData(defaultAreaNo, apiTime)
        val precipitationData = weatherDataProvider.getPrecipitationData(defaultAreaNo, apiTime)

        val uvHoursSequence = (0..75 step 3).toList()
        val sentaHoursSequence = (1..31).toList()
        val asiHoursSequence = (3..78 step 3).toList()
        val precipHoursSequence = (0..23).toList()

        logger.debug("UVIndex 데이터: $uvIndexData")
        logger.debug("SenTaIndex 데이터: $senTaIndexData")
        logger.debug("AirStagnationIndex 데이터: $airStagnationIndexData")
        logger.debug("Precipitation 데이터: $precipitationData")

        logger.info("weatherData: $weatherData")
        logger.info("dustForecast: $dustForecast")
        logger.info("realTimeDust: $realTimeDust")

        val categorizedForecast = processDustForecastData(dustForecast)
        val timeOfDay = determineTimeOfDay(hour)

        // 지역 데이터는 REST API를 통해 가져옴
        val sidos = restTemplate.getForObject("http://localhost:8090/api/regions/sidos", List::class.java) as List<String>
        val sggs = if (sidos.contains(finalSido)) restTemplate.getForObject("http://localhost:8090/api/regions/sggs?sido=$finalSido", List::class.java) as List<String> else emptyList()
        val umds = if (sggs.contains(finalSgg)) restTemplate.getForObject("http://localhost:8090/api/regions/umds?sido=$finalSido&sgg=$finalSgg", List::class.java) as List<String> else emptyList()

        logger.info("finalSido: $finalSido")
        logger.info("finalSgg: $finalSgg, sggs: $sggs")
        logger.info("finalUmd: $finalUmd, umds: $umds")

        // 모델에 데이터 추가
        model.addAttribute("sidos", sidos)
        model.addAttribute("selectedSido", finalSido)
        model.addAttribute("sggs", sggs)
        model.addAttribute("selectedSgg", finalSgg)
        model.addAttribute("umds", umds)
        model.addAttribute("selectedUmd", finalUmd)

        addAttributesToModel(model, weatherData, dustForecast, realTimeDust, uvIndexData, senTaIndexData, airStagnationIndexData, precipitationData, categorizedForecast, timeOfDay, errorMessage)
        model.addAttribute("uvHoursSequence", uvHoursSequence)
        model.addAttribute("sentaHoursSequence", sentaHoursSequence)
        model.addAttribute("asiHoursSequence", asiHoursSequence)
        model.addAttribute("precipHoursSequence", precipHoursSequence)

        return "domain/weather/weather"
    }

    private fun determineInformCode(hour: Int): String = when (hour) {
        in 0..5 -> "PM10"
        in 6..11 -> "PM25"
        in 12..17 -> "O3"
        else -> "PM10"
    }

    private fun determineTimeOfDay(hour: Int): String = when {
        hour in 6..11 -> " ( 아침 )"
        hour in 12..17 -> " ( 낮 )"
        hour in 18..23 -> " ( 저녁 )"
        else -> " ( 새벽 )"
    }

    private fun getCurrentDateTimeInfo(): Triple<String, String, Int> {
        val (date, time) = DateTimeUtils.getCurrentDateTimeFormatted()
        val hour = DateTimeUtils.getCurrentHour()
        return Triple(date, time, hour)
    }

    private fun getCurrentFormattedDate(): String =
        LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    private fun createWeatherDataProvider(useRealTimeData: Boolean): WeatherDataProvider =
        if (useRealTimeData) LiveWeatherDataProvider() else DefaultWeatherDataProvider()

    private fun processDustForecastData(dustForecast: List<ForecastInfo>): List<Triple<List<String>, List<String>, List<String>>> =
        dustForecast.map { forecast ->
            val regions = parseRegionGrades(forecast.grade)
            Triple(
                filterRegionsByGrade(regions, "좋음"),
                filterRegionsByGrade(regions, "보통"),
                filterRegionsByGrade(regions, "나쁨")
            )
        }

    private fun parseRegionGrades(gradeInfo: String): Map<String, String> =
        gradeInfo.split(",")
            .map { it.trim() }
            .mapNotNull { regionGrade ->
                val parts = regionGrade.split(":")
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }
            .toMap()

    private fun filterRegionsByGrade(regions: Map<String, String>, grade: String): List<String> =
        regions.filter { it.value == grade }
            .keys
            .toList()
            .ifEmpty { listOf("N/A") }

    private fun addAttributesToModel(
        model: Model,
        weatherData: Weather,
        dustForecast: List<ForecastInfo>,
        realTimeDust: List<RealTimeDustInfo>,
        uvIndexData: List<UVIndexInfo>,
        senTaIndexData: List<SenTaIndexInfo>,
        airStagnationIndexData: List<AirStagnationIndexInfo>,
        precipitationData: List<PrecipitationInfo>,
        categorizedForecast: List<Triple<List<String>, List<String>, List<String>>>,
        timeOfDay: String,
        errorMessage: String?
    ) {
        model.addAttribute("timeOfDay", timeOfDay)
        model.addAttribute("weather", weatherData)
        model.addAttribute("dustForecast", dustForecast.toOption().orNull())
        model.addAttribute("realTimeDust", realTimeDust.toOption().orNull())
        model.addAttribute("uvIndexData", uvIndexData.toOption().orNull())
        model.addAttribute("senTaIndexData", senTaIndexData.toOption().orNull())
        model.addAttribute("airStagnationIndexData", airStagnationIndexData.toOption().orNull())
        model.addAttribute("precipitationData", precipitationData.toOption().orNull())
        model.addAttribute("categorizedForecast", categorizedForecast.toOption().orNull())
        model.addAttribute("errorMessage", errorMessage)
    }

    private fun <T> List<T>.toOption(): Option<List<T>> =
        if (this.isEmpty()) None else Some(this)

    private fun <T> Option<T>.orNull(): T? = when (this) {
        is Some -> this.value
        is None -> null
    }

    private fun createDefaultWeatherData(date: String, time: String): Weather =
        Weather(
            date = date,
            time = time,
            location = "$defaultRegionName ($defaultRegionDistrict)",
            currentTemperature = "-1.8°C",
            highLowTemperature = "-5°C / -1°C",
            weatherCondition = "맑음",
            windSpeed = "1km/초(남서) m/s 0",
            airQuality = createDefaultAirQuality(),
            uvIndex = createDefaultUVIndex(),
            hourlyForecast = createDefaultHourlyForecast()
        )

    private fun createDefaultAirQuality(): AirQuality =
        AirQuality(
            title = "미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "20 ㎍/㎥",
            measurement = "㎍/㎥"
        )

    private fun createDefaultUVIndex(): UVIndex =
        UVIndex(
            title = "초미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "8 ㎍/㎥",
            measurement = "㎍/㎥"
        )

    private fun createDefaultHourlyForecast(): List<HourlyForecast> {
        val currentHour = DateTimeUtils.getCurrentHour()
        val currentHourIndex = currentHour / 3 * 3
        val hours = listOf(0, 3, 6, 9, 12, 15, 18, 21)
        return hours.mapIndexed { index, hourOffset ->
            val forecastHour = (currentHourIndex + hourOffset) % 24
            val forecastTime = if (hourOffset == 0) "지금" else "${forecastHour}시"
            val forecastIcon = if (forecastHour in 6..18) "sun" else "moon"
            val forecastTemp = "-${index + 1}°C"
            val forecastHumidity = "${(50 + index * 5)}%"
            HourlyForecast(forecastTime, forecastIcon, forecastTemp, forecastHumidity)
        }
    }
}
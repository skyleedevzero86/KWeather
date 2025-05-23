package com.kweather.domain.weather.controller

import com.kweather.domain.airstagnation.dto.AirStagnationIndexInfo
import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.weather.model.AirQuality
import com.kweather.domain.weather.model.HourlyForecast
import com.kweather.domain.weather.model.UVIndex
import com.kweather.domain.weather.service.WeatherService
import com.kweather.global.common.util.DateTimeUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import com.kweather.domain.weather.model.WeatherDataProvider
import com.kweather.domain.realtime.dto.RealTimeDustInfo
import com.kweather.domain.senta.dto.SenTaIndexInfo
import com.kweather.domain.uvi.dto.UVIndexInfo
import org.springframework.beans.factory.annotation.Value
import arrow.core.Option
import arrow.core.Some
import arrow.core.None

@Controller
class WeatherController(
    private val weatherService: WeatherService,
    @Value("\${weather.default.region.name:한남동}")
    private val defaultRegionName: String,
    @Value("\${weather.default.region.district:용산구}")
    private val defaultRegionDistrict: String,
    @Value("\${weather.default.sido:서울}")
    private val defaultSido: String,
    @Value("\${weather.default.area-no:1100000000}")
    private val defaultAreaNo: String
) {
    private val logger = LoggerFactory.getLogger(WeatherController::class.java)

    private inner class DefaultWeatherDataProvider : WeatherDataProvider {
        override fun getWeatherData(date: String, time: String): Weather =
            createDefaultWeatherData(date, time)

        override fun getDustForecastData(date: String, informCode: String): List<com.kweather.domain.forecast.dto.ForecastInfo> =
            runCatching {
                weatherService.getDustForecast(date, informCode)
            }.getOrElse { e ->
                logger.error("Failed to get dust forecast: ${e.message}", e)
                emptyList()
            }

        override fun getRealTimeDustData(sidoName: String): List<RealTimeDustInfo> =
            emptyList()

        override fun getUVIndexData(areaNo: String, time: String): List<UVIndexInfo> =
            emptyList()

        override fun getSenTaIndexData(areaNo: String, time: String): List<SenTaIndexInfo> =
            emptyList()

        override fun getAirStagnationIndexData(areaNo: String, time: String): List<AirStagnationIndexInfo> =
            emptyList()
    }

    private inner class LiveWeatherDataProvider : WeatherDataProvider {
        override fun getWeatherData(date: String, time: String): Weather =
            runCatching {
                weatherService.buildWeatherEntity(60, 127)
            }.getOrElse { e ->
                logger.error("Failed to get live weather data: ${e.message}", e)
                createDefaultWeatherData(date, time)
            }

        override fun getDustForecastData(date: String, informCode: String): List<com.kweather.domain.forecast.dto.ForecastInfo> =
            runCatching {
                weatherService.getDustForecast(date, informCode)
            }.getOrElse { e ->
                logger.error("Failed to get dust forecast: ${e.message}", e)
                emptyList()
            }

        override fun getRealTimeDustData(sidoName: String): List<RealTimeDustInfo> =
            runCatching {
                weatherService.getRealTimeDust(sidoName)
            }.getOrElse { e ->
                logger.error("Failed to get real-time dust data: ${e.message}", e)
                emptyList()
            }

        override fun getUVIndexData(areaNo: String, time: String): List<UVIndexInfo> =
            runCatching {
                weatherService.getUVIndex(areaNo, time)
            }.getOrElse { e ->
                logger.error("Failed to get UV index data: ${e.message}", e)
                emptyList()
            }

        override fun getSenTaIndexData(areaNo: String, time: String): List<SenTaIndexInfo> =
            runCatching {
                val currentMonth = LocalDateTime.now(ZoneId.of("Asia/Seoul")).monthValue
                if (currentMonth in 5..9) {
                    weatherService.getSenTaIndex(areaNo, time)
                } else {
                    emptyList()
                }
            }.getOrElse { e ->
                logger.error("Failed to get sensible temperature index data: ${e.message}", e)
                emptyList()
            }

        override fun getAirStagnationIndexData(areaNo: String, time: String): List<AirStagnationIndexInfo> =
            runCatching {
                weatherService.getAirStagnationIndex(areaNo, time)
            }.getOrElse { e ->
                logger.error("Failed to get air stagnation index data: ${e.message}", e)
                emptyList()
            }
    }

    @GetMapping("/")
    fun index(): String {
        return "index"
    }

    @GetMapping("/weather")
    fun getWeather(model: Model): String {
        val dateTimeInfo = getCurrentDateTimeInfo()
        val (date, time, hour) = dateTimeInfo

        val weatherDataProvider = createWeatherDataProvider(useRealTimeData = true)

        val weatherData = weatherDataProvider.getWeatherData(date, time)
        val currentDate = getCurrentFormattedDate()
        val apiTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val informCode = determineInformCode(hour)
        val dustForecast = weatherDataProvider.getDustForecastData(currentDate, informCode)

        val realTimeDust = weatherDataProvider.getRealTimeDustData(defaultSido)
        val errorMessage = if (realTimeDust.isEmpty()) "실시간 미세먼지 데이터를 불러올 수 없습니다. API 키가 유효하지 않을 수 있습니다. 설정을 확인해 주세요." else null

        val uvIndexData = weatherDataProvider.getUVIndexData(defaultAreaNo, apiTime)
        val senTaIndexData = weatherDataProvider.getSenTaIndexData(defaultAreaNo, apiTime)
        val airStagnationIndexData = weatherDataProvider.getAirStagnationIndexData(defaultAreaNo, apiTime)

        // 숫자 시퀀스 생성
        val uvHoursSequence = (0..75 step 3).toList() // 자외선 지수 (0, 3, 6, ..., 75)
        val sentaHoursSequence = (1..31).toList() // 여름철 체감온도 (1, 2, ..., 31)
        val asiHoursSequence = (3..78 step 3).toList() // 대기정체지수 (3, 6, ..., 78)

        logger.debug("UVIndex 데이터: $uvIndexData")
        logger.debug("SenTaIndex 데이터: $senTaIndexData")
        logger.debug("AirStagnationIndex 데이터: $airStagnationIndexData")

        logger.info("weatherData: $weatherData")
        logger.info("dustForecast: $dustForecast")
        logger.info("realTimeDust: $realTimeDust")

        val categorizedForecast = processDustForecastData(dustForecast)
        val timeOfDay = determineTimeOfDay(hour)

        addAttributesToModel(model, weatherData, dustForecast, realTimeDust, uvIndexData, senTaIndexData, airStagnationIndexData, categorizedForecast, timeOfDay, errorMessage)
        model.addAttribute("uvHoursSequence", uvHoursSequence)
        model.addAttribute("sentaHoursSequence", sentaHoursSequence)
        model.addAttribute("asiHoursSequence", asiHoursSequence)

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
        val hour = DateTimeUtils.getCurrentHour() // 현재 시간: 21
        return Triple(date, time, hour)
    }

    private fun getCurrentFormattedDate(): String =
        LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    private fun createWeatherDataProvider(useRealTimeData: Boolean): WeatherDataProvider =
        if (useRealTimeData) LiveWeatherDataProvider() else DefaultWeatherDataProvider()

    private fun processDustForecastData(dustForecast: List<com.kweather.domain.forecast.dto.ForecastInfo>): List<Triple<List<String>, List<String>, List<String>>> =
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
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    null
                }
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
        dustForecast: List<com.kweather.domain.forecast.dto.ForecastInfo>,
        realTimeDust: List<RealTimeDustInfo>,
        uvIndexData: List<UVIndexInfo>,
        senTaIndexData: List<SenTaIndexInfo>,
        airStagnationIndexData: List<AirStagnationIndexInfo>,
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
        val currentHour = DateTimeUtils.getCurrentHour() // 21
        val currentHourIndex = currentHour / 3 * 3 // 21
        val hours = listOf(0, 3, 6, 9, 12, 15, 18, 21)
        return hours.mapIndexed { index, hourOffset ->
            val forecastHour = (currentHourIndex + hourOffset) % 24 // 21, 0, 3, 6, 9, 12, 15, 18
            val forecastTime = if (hourOffset == 0) "지금" else "${forecastHour}시"
            val forecastIcon = if (forecastHour in 6..18) "sun" else "moon"
            val forecastTemp = "-${index + 1}°C" // 기본값, 실제 데이터로 대체 필요
            val forecastHumidity = "${(50 + index * 5)}%" // 기본값, 실제 데이터로 대체 필요
            HourlyForecast(forecastTime, forecastIcon, forecastTemp, forecastHumidity)
        }
    }
}
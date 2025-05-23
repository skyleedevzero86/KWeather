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

    // 날씨 정보 제공자 구현체 - WeatherService 어댑터 패턴 적용
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
            emptyList() // 기본 제공자는 빈 리스트 반환

        override fun getUVIndexData(areaNo: String, time: String): List<UVIndexInfo> =
            emptyList()

        override fun getSenTaIndexData(areaNo: String, time: String): List<SenTaIndexInfo> =
            emptyList()

        override fun getAirStagnationIndexData(areaNo: String, time: String): List<AirStagnationIndexInfo> =
            emptyList()
    }

    // 실시간 API 기반 날씨 정보 제공자
    private inner class LiveWeatherDataProvider : WeatherDataProvider {
        override fun getWeatherData(date: String, time: String): Weather =
            runCatching {
                weatherService.buildWeatherEntity(60, 127) // 기본값 사용
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
                if (currentMonth in 5..9) { // 5월~9월에만 조회
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
        return "index" // index.html을 렌더링
    }

    @GetMapping("/weather")
    fun getWeather(model: Model): String {
        // 현재 날짜/시간 정보 가져오기
        val dateTimeInfo = getCurrentDateTimeInfo()
        val (date, time, hour) = dateTimeInfo

        // 날씨 데이터 제공자 선택
        val weatherDataProvider = createWeatherDataProvider(useRealTimeData = true)

        // 날씨 데이터 및 미세먼지 예보 조회
        val weatherData = weatherDataProvider.getWeatherData(date, time)
        val currentDate = getCurrentFormattedDate()
        val apiTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val informCode = determineInformCode(hour)
        val dustForecast = weatherDataProvider.getDustForecastData(currentDate, informCode)

        // 실시간 미세먼지 데이터 조회
        val realTimeDust = weatherDataProvider.getRealTimeDustData(defaultSido)
        val errorMessage = if (realTimeDust.isEmpty()) "실시간 미세먼지 데이터를 불러올 수 없습니다. API 키가 유효하지 않을 수 있습니다. 설정을 확인해 주세요." else null

        // 자외선 지수, 체감온도, 대기정체지수 조회
        val uvIndexData = weatherDataProvider.getUVIndexData(defaultAreaNo, apiTime)
        val senTaIndexData = weatherDataProvider.getSenTaIndexData(defaultAreaNo, apiTime)
        val airStagnationIndexData = weatherDataProvider.getAirStagnationIndexData(defaultAreaNo, apiTime)

        // 미세먼지 예보 데이터 가공
        val categorizedForecast = processDustForecastData(dustForecast)

        // 시간대 문자열 생성
        val timeOfDay = determineTimeOfDay(hour)

        // 모델에 데이터 추가
        addAttributesToModel(model, weatherData, dustForecast, realTimeDust, uvIndexData, senTaIndexData, airStagnationIndexData, categorizedForecast, timeOfDay, errorMessage)

        return "domain/weather/weather"
    }

    /**
     * 시간대별 정보 코드 결정
     */
    private fun determineInformCode(hour: Int): String = when (hour) {
        in 0..5 -> "PM10"
        in 6..11 -> "PM25"
        in 12..17 -> "O3"
        else -> "PM10"
    }

    /**
     * 시간대 문자열 생성
     */
    private fun determineTimeOfDay(hour: Int): String = when {
        hour in 6..11 -> " ( 아침 )"
        hour in 12..17 -> " ( 낮 )"
        hour in 18..23 -> " ( 저녁 )"
        else -> " ( 새벽 )"
    }

    /**
     * 현재 날짜/시간 정보 가져오기
     * @return Triple<날짜, 시간, 시간(정수)>
     */
    private fun getCurrentDateTimeInfo(): Triple<String, String, Int> {
        val (date, time) = DateTimeUtils.getCurrentDateTimeFormatted()
        val hour = DateTimeUtils.getCurrentHour()
        return Triple(date, time, hour)
    }

    /**
     * 현재 날짜를 yyyy-MM-dd 형식으로 반환
     */
    private fun getCurrentFormattedDate(): String =
        LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    /**
     * 날씨 데이터 제공자 생성
     */
    private fun createWeatherDataProvider(useRealTimeData: Boolean): WeatherDataProvider =
        if (useRealTimeData) LiveWeatherDataProvider() else DefaultWeatherDataProvider()

    /**
     * 미세먼지 예보 데이터 처리
     */
    private fun processDustForecastData(dustForecast: List<com.kweather.domain.forecast.dto.ForecastInfo>): List<Triple<List<String>, List<String>, List<String>>> =
        dustForecast.map { forecast ->
            // 지역별 등급 정보 파싱
            val regions = parseRegionGrades(forecast.grade)

            // 등급별 지역 그룹화
            Triple(
                filterRegionsByGrade(regions, "좋음"),
                filterRegionsByGrade(regions, "보통"),
                filterRegionsByGrade(regions, "나쁨")
            )
        }

    /**
     * 지역별 등급 정보 파싱
     */
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

    /**
     * 특정 등급에 해당하는 지역 필터링
     */
    private fun filterRegionsByGrade(regions: Map<String, String>, grade: String): List<String> =
        regions.filter { it.value == grade }
            .keys
            .toList()
            .ifEmpty { listOf("N/A") }

    /**
     * 모델에 속성 추가
     */
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

    /**
     * 리스트를 Option으로 변환
     */
    private fun <T> List<T>.toOption(): Option<List<T>> =
        if (this.isEmpty()) None else Some(this)

    /**
     * Option을 null 또는 값으로 변환
     */
    private fun <T> Option<T>.orNull(): T? = when (this) {
        is Some -> this.value
        is None -> null
    }

    /**
     * 기본 날씨 데이터 생성
     */
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

    /**
     * 기본 미세먼지 정보 생성
     */
    private fun createDefaultAirQuality(): AirQuality =
        AirQuality(
            title = "미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "20 ㎍/㎥",
            measurement = "㎍/㎥"
        )

    /**
     * 기본 자외선 정보 생성
     */
    private fun createDefaultUVIndex(): UVIndex =
        UVIndex(
            title = "초미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "8 ㎍/㎥",
            measurement = "㎍/㎥"
        )

    /**
     * 기본 시간별 예보 생성
     */
    private fun createDefaultHourlyForecast(): List<HourlyForecast> =
        listOf(
            HourlyForecast("지금", "moon", "-1.8°C", "34%"),
            HourlyForecast("0시", "moon", "-6°C", "55%"),
            HourlyForecast("3시", "moon", "-6°C", "60%"),
            HourlyForecast("6시", "moon", "-7°C", "67%"),
            HourlyForecast("9시", "sun", "-6°C", "55%")
        )
}
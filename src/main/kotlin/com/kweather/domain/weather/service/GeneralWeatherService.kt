package com.kweather.domain.weather.service

import arrow.core.Either
import com.kweather.domain.airstagnation.service.AirStagnationIndexService
import com.kweather.domain.forecast.dto.DustForecastRequestParams
import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.forecast.dto.ForecastItem
import com.kweather.domain.forecast.dto.ForecastResponse
import com.kweather.domain.realtime.dto.RealTimeDustInfo
import com.kweather.domain.realtime.dto.RealTimeDustItem
import com.kweather.domain.realtime.dto.RealTimeDustResponse
import com.kweather.domain.senta.service.SenTaIndexService
import com.kweather.domain.uvi.service.UVIndexService
import com.kweather.domain.weather.dto.*
import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.weather.model.*
import com.kweather.global.common.util.ApiClientUtility
import com.kweather.global.common.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class GeneralWeatherService(
    @Value("\${api.weather.base-url:}") private val weatherBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.base-url:}") private val dustForecastBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.real-time-base-url:}") private val realTimeDustBaseUrl: String,
    @Value("\${api.service-key:}") private val serviceKey: String,
    private val uvIndexService: UVIndexService,
    private val senTaIndexService: SenTaIndexService,
    private val airStagnationIndexService: AirStagnationIndexService
) {
    private val logger = LoggerFactory.getLogger(GeneralWeatherService::class.java)

    data class RealTimeDustRequestParams(
        val returnType: String = "json",
        val numOfRows: Int = 50,
        val pageNo: Int = 1,
        val sidoName: String,
        val ver: String = "1.0"
    )

    private inner class WeatherApiClient : ApiClientUtility.ApiClient<WeatherRequestParams, WeatherResponse> {
        override fun buildUrl(params: WeatherRequestParams): String {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val date = LocalDate.parse(params.baseDate, formatter)
            val targetDate = date.minusDays(1).format(formatter) // 과거 데이터 요청

            return "${weatherBaseUrl}?serviceKey=$serviceKey" +
                    "&numOfRows=1000" +
                    "&pageNo=1" +
                    "&base_date=$targetDate" +
                    "&base_time=${params.baseTime}" +
                    "&nx=${params.nx}" +
                    "&ny=${params.ny}" +
                    "&dataType=JSON"
        }

        override fun parseResponse(response: String): Either<String, WeatherResponse> =
            runCatching {
                ApiClientUtility.getObjectMapper().readValue(response, WeatherResponse::class.java)
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("날씨 응답 파싱 실패: ${it.message}") }
            )
    }

    private inner class DustForecastApiClient : ApiClientUtility.ApiClient<DustForecastRequestParams, ForecastResponse> {
        override fun buildUrl(params: DustForecastRequestParams): String {
            return "${dustForecastBaseUrl}?serviceKey=$serviceKey" +
                    "&returnType=json" +
                    "&numOfRows=100" +
                    "&pageNo=1" +
                    "&searchDate=${params.searchDate}" +
                    "&dataTerm=DAILY"
        }

        override fun parseResponse(response: String): Either<String, ForecastResponse> =
            runCatching {
                ApiClientUtility.getObjectMapper().readValue(response, ForecastResponse::class.java)
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("미세먼지 예보 응답 파싱 실패: ${it.message}") }
            )
    }

    private inner class RealTimeDustApiClient : ApiClientUtility.ApiClient<RealTimeDustRequestParams, RealTimeDustResponse> {
        override fun buildUrl(params: RealTimeDustRequestParams): String {
            val encodedSidoName = URLEncoder.encode(params.sidoName, StandardCharsets.UTF_8.toString())
            return "${realTimeDustBaseUrl}?serviceKey=$serviceKey" +
                    "&returnType=${params.returnType}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&pageNo=${params.pageNo}" +
                    "&sidoName=$encodedSidoName" +
                    "&ver=${params.ver}"
        }

        override fun parseResponse(response: String): Either<String, RealTimeDustResponse> =
            runCatching {
                ApiClientUtility.getObjectMapper().readValue(response, RealTimeDustResponse::class.java)
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("실시간 미세먼지 응답 파싱 실패: ${it.message}") }
            )
    }

    fun getUltraShortWeather(nx: Int, ny: Int): WeatherResponse {
        val baseDate = DateTimeUtils.getBaseDate()
        val baseTime = DateTimeUtils.getBaseTime()
        val params = WeatherRequestParams(baseDate, baseTime, nx, ny)
        val weatherClient = WeatherApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(weatherClient, params) { response ->
            if (response.response?.header?.resultCode == "03" &&
                response.response.header.resultMsg == "NO_DATA") {
                logger.info("데이터 없음, 이전 시간으로 재시도: $baseDate $baseTime")
                Either.Right(retryWithEarlierTime(nx, ny, baseDate) ?: response)
            } else {
                Either.Right(response)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("날씨 API 요청 실패: ${result.message}")
                WeatherResponse(response = Response(
                    header = Header("ERROR", "날씨 데이터 가져오기 실패: ${result.message}"),
                    body = null
                ))
            }
        }
    }

    fun getDustForecast(searchDate: String, informCode: String): List<ForecastInfo> {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val currentHour = now.hour
        val targetDate = if (currentHour < 6) {
            now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } else {
            searchDate
        }
        val params = DustForecastRequestParams(targetDate, informCode)
        val dustClient = DustForecastApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(dustClient, params) { response ->
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                logger.info("미세먼지 예보 데이터 없음, 전날 데이터로 재시도")
                val previousDate = LocalDate.parse(targetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val retryParams = DustForecastRequestParams(previousDate, informCode)
                when (val retryResult = ApiClientUtility.makeApiRequest(dustClient, retryParams) { retryResponse ->
                    val forecastInfos = retryResponse.response?.body?.items?.mapNotNull { item ->
                        item?.let { parseForecastItem(it, informCode) }
                    } ?: emptyList()
                    Either.Right(forecastInfos)
                }) {
                    is ApiResult.Success -> Either.Right(retryResult.data)
                    is ApiResult.Error -> Either.Right(emptyList())
                }
            } else {
                val forecastInfos = response.response?.body?.items?.mapNotNull { item ->
                    item?.let { parseForecastItem(it, informCode) }
                } ?: emptyList()
                Either.Right(forecastInfos)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("미세먼지 예보 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    fun getRealTimeDust(sidoName: String): List<RealTimeDustInfo> {
        val params = RealTimeDustRequestParams(sidoName = sidoName)
        val realTimeDustClient = RealTimeDustApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(realTimeDustClient, params) { response ->
            val dustInfos = response.response?.body?.items?.mapNotNull { item ->
                item?.let { parseRealTimeDustItem(it) }
            } ?: emptyList()
            Either.Right(dustInfos)
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("실시간 미세먼지 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    fun parseWeatherData(response: WeatherResponse): List<WeatherInfo> =
        when {
            response.response?.header?.resultCode != "00" -> {
                listOf(
                    WeatherInfo(
                        baseDate = "",
                        baseTime = "",
                        category = "ERROR",
                        value = "API 오류: ${response.response?.header?.resultCode} - ${response.response?.header?.resultMsg}",
                        unit = ""
                    )
                )
            }
            response.response?.body?.items.isNullOrEmpty() -> {
                logger.warn("날씨 응답에 항목이 없습니다")
                emptyList()
            }
            else -> {
                response.response?.body?.items?.mapNotNull { item -> item?.let { parseWeatherItem(it) } } ?: emptyList()
            }
        }

    private fun parseWeatherItem(item: WeatherItem): WeatherInfo? =
        runCatching {
            WeatherInfo(
                baseDate = item.baseDate ?: return null,
                baseTime = item.baseTime ?: return null,
                category = item.category ?: return null,
                value = item.obsrValue ?: return null,
                unit = getUnitForCategory(item.category)
            )
        }.onFailure { e ->
            logger.warn("날씨 항목 파싱 실패: ${e.message}")
        }.getOrNull()

    private fun parseRealTimeDustItem(item: RealTimeDustItem): RealTimeDustInfo? =
        runCatching {
            RealTimeDustInfo(
                sidoName = item.sidoName ?: return null,
                stationName = item.stationName ?: return null,
                pm10Value = item.pm10Value?.takeIf { it != "-" } ?: "N/A",
                pm10Grade = convertGrade(item.pm10Grade),
                pm25Value = item.pm25Value?.takeIf { it != "-" } ?: "N/A",
                pm25Grade = convertGrade(item.pm25Grade),
                dataTime = item.dataTime ?: return null
            )
        }.onFailure { e ->
            logger.warn("실시간 미세먼지 항목 파싱 실패: ${e.message}")
        }.getOrNull()

    private fun convertGrade(grade: String?): String = when (grade) {
        "1" -> "좋음"
        "2" -> "보통"
        "3" -> "나쁨"
        "4" -> "매우나쁨"
        else -> "N/A"
    }

    private fun parseForecastItem(item: ForecastItem, defaultInformCode: String): ForecastInfo? =
        runCatching {
            ForecastInfo(
                date = item.dataTime ?: return null,
                type = item.informCode ?: defaultInformCode,
                overall = item.informOverall ?: "N/A",
                cause = item.informCause ?: "N/A",
                grade = item.informGrade ?: "N/A",
                dataTime = item.dataTime ?: return null,
                imageUrls = listOfNotNull(
                    item.imageUrl1, item.imageUrl2, item.imageUrl3,
                    item.imageUrl4, item.imageUrl5, item.imageUrl6
                )
            )
        }.onFailure { e ->
            logger.warn("예보 항목 파싱 실패: ${e.message}")
        }.getOrNull()

    private fun retryWithEarlierTime(nx: Int, ny: Int, baseDate: String): WeatherResponse? {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val retryTime = calculateRetryTime(now.hour, now.minute)
        val params = WeatherRequestParams(baseDate, retryTime, nx, ny)
        val weatherClient = WeatherApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(weatherClient, params) { response ->
            Either.Right(response)
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("재시도 실패: ${result.message}")
                null
            }
        }
    }

    private fun calculateRetryTime(hour: Int, minute: Int): String =
        when {
            hour >= 12 && minute >= 5 -> "0000"
            minute < 30 -> String.format("%02d00", if (hour == 0) 23 else hour - 1)
            else -> String.format("%02d30", if (minute < 45) hour else if (hour == 0) 23 else hour - 1)
        }

    fun buildWeatherEntity(nx: Int, ny: Int): Weather {
        val response = getUltraShortWeather(nx, ny)
        val weatherInfoList = parseWeatherData(response)
        val sidoName = "서울"
        val areaNo = "1100000000"
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val apiTime = now.minusHours(1).format(DateTimeFormatter.ofPattern("yyyyMMddHH")) // 1시간 전 시간 사용
        val realTimeDust = getRealTimeDust(sidoName)
        val uvIndexData = uvIndexService.getUVIndex(areaNo, apiTime)
        val senTaIndexData = senTaIndexService.getSenTaIndex(areaNo, apiTime)
        val airStagnationIndexData = airStagnationIndexService.getAirStagnationIndex(areaNo, apiTime)

        val (date, time) = DateTimeUtils.getCurrentDateTimeFormatted()
        val currentHour = DateTimeUtils.getCurrentHour()

        val temperature = weatherInfoList.find { it.category == "T1H" }?.value?.let { "$it°C" } ?: "-1.8°C"
        val humidity = weatherInfoList.find { it.category == "REH" }?.value?.let { "$it%" } ?: "34%"
        val windSpeed = weatherInfoList.find { it.category == "WSD" }?.value?.let { "${it}m/s" } ?: "1km/초(남서) m/s 0"
        val weatherCondition = weatherInfoList.find { it.category == "PTY" }?.value?.let {
            when (it) {
                "0" -> "맑음"
                "1" -> "비"
                "2" -> "비/눈"
                "3" -> "눈"
                else -> "맑음"
            }
        } ?: "맑음"

        val airQuality = realTimeDust.firstOrNull()?.let {
            AirQuality(
                title = "미세먼지",
                icon = "yellow-smiley",
                status = it.pm10Grade,
                value = it.pm10Value,
                measurement = "㎍/㎥"
            )
        } ?: createDefaultAirQuality()

        val uvIndex = uvIndexData.firstOrNull()?.let { uvInfo ->
            val currentHourKey = "h${currentHour}"
            val uvValue = uvInfo.values[currentHourKey] ?: uvInfo.values["h24"] ?: "N/A"
            val uvStatus = when (uvValue.toIntOrNull() ?: 0) {
                in 0..2 -> "낮음"
                in 3..5 -> "보통"
                in 6..7 -> "높음"
                in 8..10 -> "매우 높음"
                11 -> "위험"
                else -> "N/A"
            }
            UVIndex(
                title = "자외선 지수",
                icon = "yellow-smiley",
                status = uvStatus,
                value = uvValue,
                measurement = ""
            )
        } ?: createDefaultUVIndex()

        val hourlyForecast = mutableListOf<HourlyForecast>()
        val currentHourIndex = currentHour / 3 * 3
        val hours = listOf(0, 3, 6, 9, 12, 15, 18, 21)

        hours.forEachIndexed { index, hourOffset ->
            val forecastHour = (currentHourIndex + hourOffset) % 24
            val forecastTime = if (hourOffset == 0) "지금" else "${forecastHour}시"
            val forecastIcon = if (forecastHour in 6..18) "sun" else "moon"
            val senTaKeyHour = forecastHour + 1
            val senTaKey = "h$senTaKeyHour"
            val forecastTemp = senTaIndexData.firstOrNull()?.values?.get(senTaKey)?.let { "$it°C" } ?: temperature
            hourlyForecast.add(HourlyForecast(forecastTime, forecastIcon, forecastTemp, humidity))
        }

        return Weather(
            date = date,
            time = time,
            location = "한남동 (용산구)",
            currentTemperature = temperature,
            highLowTemperature = "-7°C / -1°C",
            weatherCondition = weatherCondition,
            windSpeed = windSpeed,
            airQuality = airQuality,
            uvIndex = uvIndex,
            hourlyForecast = hourlyForecast
        )
    }

    private fun createDefaultAirQuality(): AirQuality =
        AirQuality(
            title = "미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "20",
            measurement = "㎍/㎥"
        )

    private fun createDefaultUVIndex(): UVIndex =
        UVIndex(
            title = "자외선 지수",
            icon = "yellow-smiley",
            status = "좋음",
            value = "8",
            measurement = ""
        )

    private fun getUnitForCategory(category: String?): String =
        when (category) {
            "T1H" -> "°C"
            "RN1" -> "mm"
            "UUU", "VVV", "WSD" -> "m/s"
            "REH" -> "%"
            "VEC" -> "deg"
            else -> ""
        }

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serviceKey.isBlank() || weatherBaseUrl.isBlank() || dustForecastBaseUrl.isBlank() || realTimeDustBaseUrl.isBlank()) {
            logger.error("날씨 및 미세먼지 서비스: 필수 설정값이 누락되었습니다")
        }
    }
}
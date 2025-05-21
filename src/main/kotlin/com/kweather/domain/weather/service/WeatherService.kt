package com.kweather.domain.weather.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kweather.domain.forecast.dto.DustForecastRequestParams
import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.forecast.dto.ForecastItem
import com.kweather.domain.forecast.dto.ForecastResponse
import com.kweather.domain.weather.dto.*
import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.weather.model.AirQuality
import com.kweather.domain.weather.model.HourlyForecast
import com.kweather.domain.weather.model.UVIndex
import com.kweather.global.common.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right

/**
 * 날씨 정보 서비스
 * - 함수형 프로그래밍 접근법과 전략 패턴 적용
 */
@Service
class WeatherService(
    private val objectMapper: ObjectMapper,
    @Value("\${api.weather.base-url:}")
    private val weatherBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.base-url:}")
    private val dustForecastBaseUrl: String,
    @Value("\${api.service-key:}")
    private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(WeatherService::class.java)

    // API Client 전략 인터페이스
    private interface ApiClient<P, T> {
        fun buildUrl(params: P): String
        fun parseResponse(response: String): Either<String, T>
    }

    // 날씨 API 클라이언트 구현
    private inner class WeatherApiClient : ApiClient<WeatherRequestParams, WeatherResponse> {
        override fun buildUrl(params: WeatherRequestParams): String {
            return "${weatherBaseUrl}?serviceKey=${serviceKey}" +
                    "&numOfRows=10" +
                    "&pageNo=1" +
                    "&base_date=${params.baseDate}" +
                    "&base_time=${params.baseTime}" +
                    "&nx=${params.nx}" +
                    "&ny=${params.ny}" +
                    "&dataType=JSON"
        }

        override fun parseResponse(response: String): Either<String, WeatherResponse> =
            runCatching {
                objectMapper.readValue<WeatherResponse>(response)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "Failed to parse weather response: ${it.message}".left() }
            )
    }

    // 미세먼지 예보 API 클라이언트 구현
    private inner class DustForecastApiClient : ApiClient<DustForecastRequestParams, ForecastResponse> {
        override fun buildUrl(params: DustForecastRequestParams): String {
            return "${dustForecastBaseUrl}?serviceKey=${serviceKey}" +
                    "&returnType=json" +
                    "&numOfRows=100" +
                    "&pageNo=1" +
                    "&searchDate=${params.searchDate}" +
                    "&dataTerm=DAILY"
        }

        override fun parseResponse(response: String): Either<String, ForecastResponse> =
            runCatching {
                objectMapper.readValue<ForecastResponse>(response)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "Failed to parse dust forecast response: ${it.message}".left() }
            )
    }

    // API 요청을 추상화한 함수
    private fun <P, T, R> makeApiRequest(
        client: ApiClient<P, T>,
        params: P,
        transform: (T) -> Either<String, R>
    ): ApiResult<R> {
        logger.info("Making API request with params: $params")

        return try {
            val urlString = client.buildUrl(params)
            logger.info("URL: {} (redacted service key)", urlString.replace(serviceKey, "[REDACTED]"))

            val response = fetchDataFromApi(urlString).getOrElse { error ->
                return ApiResult.Error("Failed to fetch data: $error")
            }

            if (response.trim().startsWith("<")) {
                handleXmlErrorResponse(response)
                return ApiResult.Error("API returned an XML error response")
            }

            client.parseResponse(response).flatMap(transform).fold(
                { error -> ApiResult.Error(error) },
                { result -> ApiResult.Success(result) }
            )
        } catch (e: Exception) {
            logger.error("API request failed", e)
            ApiResult.Error("API request failed: ${e.message}", e)
        }
    }

    private fun handleXmlErrorResponse(response: String) {
        when {
            response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") || response.contains("SERVICE ERROR") -> {
                logger.error("API key error: {}", response)
                throw IllegalStateException("API service key error: Check if the key is valid and properly formatted")
            }
            else -> {
                logger.error("Received XML error response: {}", response.take(500))
            }
        }
    }

    // HTTP 요청을 수행하고 결과를 반환하는 함수
    private fun fetchDataFromApi(urlString: String): Either<String, String> =
        Either.catch {
            URL(urlString).openConnection().let { conn ->
                (conn as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val responseCode = conn.responseCode
                logger.info("Response code: $responseCode")

                val reader = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(conn.inputStream))
                } else {
                    BufferedReader(InputStreamReader(conn.errorStream))
                }

                use(reader) { r ->
                    r.lines().collect(java.util.stream.Collectors.joining())
                }
            }
        }.mapLeft { e ->
            logger.error("HTTP request failed", e)
            "HTTP request failed: ${e.message}"
        }

    // '자원 사용'을 안전하게 처리하는 고차 함수
    private inline fun <T : AutoCloseable, R> use(resource: T, block: (T) -> R): R {
        try {
            return block(resource)
        } finally {
            try {
                resource.close()
            } catch (e: IOException) {
                logger.error("Failed to close resource", e)
            }
        }
    }

    /**
     * 초단기 날씨 정보를 조회합니다.
     */
    fun getUltraShortWeather(nx: Int, ny: Int): WeatherResponse {
        val baseDate = DateTimeUtils.getBaseDate()
        val baseTime = DateTimeUtils.getBaseTime()

        logger.info("Using baseDate: $baseDate, baseTime: $baseTime")

        val params = WeatherRequestParams(baseDate, baseTime, nx, ny)
        val weatherClient = WeatherApiClient()

        return when (val result = makeApiRequest(weatherClient, params) { response ->
            if (response.response?.header?.resultCode == "03" &&
                response.response.header.resultMsg == "NO_DATA") {
                logger.warn("No weather data available for $baseDate $baseTime, retrying with earlier time")
                Either.Right(retryWithEarlierTime(nx, ny, baseDate) ?: response)
            } else {
                Either.Right(response)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("Weather API request failed: ${result.message}")
                WeatherResponse(WeatherResponseData(
                    header = Header("ERROR", "Failed to fetch weather data: ${result.message}"),
                    body = null
                ))
            }
        }
    }

    /**
     * 미세먼지 예보 정보를 조회합니다.
     */
    fun getDustForecast(searchDate: String, informCode: String): List<ForecastInfo> {
        val params = DustForecastRequestParams(searchDate, informCode)
        val dustClient = DustForecastApiClient()

        return when (val result = makeApiRequest(dustClient, params) { response: ForecastResponse ->
            val forecastInfos = response.response?.body?.items?.mapNotNull { item: ForecastItem? ->
                item?.let { parseForecastItem(it, informCode) }
            } ?: emptyList<ForecastInfo>()

            Either.Right(forecastInfos)
        }) {
            is ApiResult.Success<List<ForecastInfo>> -> result.data
            is ApiResult.Error -> {
                logger.error("Dust forecast API request failed: ${result.message}")
                throw IllegalStateException("Failed to fetch dust forecast data: ${result.message}")
            }
        }
    }

    // 날씨 예보 응답 항목 파싱
    private fun parseForecastItem(item: ForecastItem, defaultInformCode: String): ForecastInfo? =
        runCatching {
            ForecastInfo(
                date = item.dataTime ?: throw IllegalArgumentException("dataTime is missing"),
                type = item.informCode ?: defaultInformCode,
                overall = item.informOverall ?: "N/A",
                cause = item.informCause ?: "N/A",
                grade = item.informGrade ?: "N/A",
                dataTime = item.dataTime ?: throw IllegalArgumentException("dataTime is missing"),
                imageUrls = listOfNotNull(
                    item.imageUrl1,
                    item.imageUrl2,
                    item.imageUrl3,
                    item.imageUrl4,
                    item.imageUrl5,
                    item.imageUrl6
                )
            )
        }.onFailure { e ->
            logger.warn("Skipping invalid forecast item: ${e.message}")
        }.getOrNull()

    /**
     * 초기 요청 실패 시 이전 시간대로 재시도
     */
    private fun retryWithEarlierTime(nx: Int, ny: Int, baseDate: String): WeatherResponse? {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val retryTime = calculateRetryTime(now.hour, now.minute)

        logger.info("Retrying with baseTime: $retryTime")

        val params = WeatherRequestParams(baseDate, retryTime, nx, ny)
        val weatherClient = WeatherApiClient()

        return when (val result = makeApiRequest(weatherClient, params) { response ->
            Either.Right(response)
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("Retry failed: ${result.message}")
                null
            }
        }
    }

    // 재시도용 시간 계산 로직
    private fun calculateRetryTime(hour: Int, minute: Int): String =
        when {
            hour >= 12 && minute >= 5 -> "0000"
            minute < 30 -> String.format("%02d00", if (hour == 0) 23 else hour - 1)
            else -> String.format("%02d30", if (minute < 45) hour else if (hour == 0) 23 else hour - 1)
        }

    /**
     * 날씨 데이터를 파싱합니다.
     */
    fun parseWeatherData(response: WeatherResponse): List<WeatherInfo> =
        when {
            response.response?.header?.resultCode != "00" && response.response?.header?.resultCode != null -> {
                listOf(
                    WeatherInfo(
                        baseDate = "",
                        baseTime = "",
                        category = "ERROR",
                        value = "API Error: ${response.response.header.resultCode} - ${response.response.header.resultMsg}",
                        unit = ""
                    )
                )
            }
            response.response?.body?.items?.item == null -> {
                logger.warn("Weather response does not contain items. Response: $response")
                emptyList()
            }
            else -> {
                response.response.body.items.item.mapNotNull { item ->
                    parseWeatherItem(item)
                }
            }
        }

    // 날씨 항목 파싱 로직
    private fun parseWeatherItem(item: WeatherItem?): WeatherInfo? =
        item?.let {
            runCatching {
                WeatherInfo(
                    baseDate = it.baseDate ?: throw IllegalArgumentException("baseDate is missing"),
                    baseTime = it.baseTime ?: throw IllegalArgumentException("baseTime is missing"),
                    category = it.category ?: throw IllegalArgumentException("category is missing"),
                    value = it.obsrValue ?: throw IllegalArgumentException("obsrValue is missing"),
                    unit = getUnitForCategory(it.category)
                )
            }.onFailure { e ->
                logger.warn("Skipping invalid weather item: ${e.message}")
            }.getOrNull()
        }

    /**
     * 날씨 엔티티를 구성합니다.
     */
    fun buildWeatherEntity(nx: Int, ny: Int): Weather {
        val response = getUltraShortWeather(nx, ny)
        val weatherInfoList = parseWeatherData(response)

        val (date, time) = DateTimeUtils.getCurrentDateTimeFormatted()
        val temperature = weatherInfoList.find { it.category == "T1H" }?.value ?: "-1.8°C"

        return Weather(
            date = date,
            time = time,
            location = "한남동 (용산구)",
            currentTemperature = temperature,
            highLowTemperature = "-7°C / -1°C",
            weatherCondition = "맑음",
            windSpeed = "1km/초(남서) m/s 0",
            airQuality = createAirQuality(),
            uvIndex = createUVIndex(),
            hourlyForecast = createHourlyForecast()
        )
    }

    // 미세먼지 정보 생성
    private fun createAirQuality(): AirQuality =
        AirQuality(
            title = "미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "20",
            measurement = "㎍/㎥"
        )

    // 자외선 정보 생성
    private fun createUVIndex(): UVIndex =
        UVIndex(
            title = "초미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "8",
            measurement = "㎍/㎥"
        )

    // 시간별 예보 생성
    private fun createHourlyForecast(): List<HourlyForecast> =
        listOf(
            HourlyForecast("지금", "moon", "-1.8°C", "34%"),
            HourlyForecast("0시", "moon", "-6°C", "55%"),
            HourlyForecast("3시", "moon", "-6°C", "60%"),
            HourlyForecast("6시", "moon", "-7°C", "67%"),
            HourlyForecast("9시", "sun", "-6°C", "55%")
        )

    // 날씨 항목별 단위 반환
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

    // 설정 유효성 검사
    private fun validateConfiguration() {
        if (serviceKey.isBlank()) {
            logger.warn("Service key is blank. API calls may fail.")
        }
        if (weatherBaseUrl.isBlank()) {
            logger.warn("Weather Base URL is blank. API calls may fail.")
        }
        if (dustForecastBaseUrl.isBlank()) {
            logger.warn("Dust Forecast Base URL is blank. API calls may fail.")
        }
    }
}
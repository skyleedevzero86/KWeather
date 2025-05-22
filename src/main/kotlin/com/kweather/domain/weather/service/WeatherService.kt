package com.kweather.domain.weather.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kweather.domain.forecast.dto.DustForecastRequestParams
import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.forecast.dto.ForecastItem
import com.kweather.domain.forecast.dto.ForecastResponse
import com.kweather.domain.weather.dto.*
import com.kweather.domain.weather.entity.Weather
import com.kweather.global.common.util.DateTimeUtils
import org.slf4j.LoggerFactory
import java.net.SocketTimeoutException
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.nio.charset.StandardCharsets
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.kweather.domain.realtime.dto.RealTimeDustInfo
import com.kweather.domain.realtime.dto.RealTimeDustItem
import com.kweather.domain.realtime.dto.RealTimeDustResponse
import com.kweather.domain.weather.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class WeatherService(
    private val objectMapper: ObjectMapper,
    @Value("\${api.weather.base-url:}")
    private val weatherBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.base-url:}")
    private val dustForecastBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.real-time-base-url:}")
    private val realTimeDustBaseUrl: String,
    @Value("\${api.service-key:}")
    private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(WeatherService::class.java)

    data class RealTimeDustRequestParams(
        val returnType: String = "json",
        val numOfRows: Int = 50, // 요청 크기를 100에서 50으로 줄임
        val pageNo: Int = 1,
        val sidoName: String,
        val ver: String = "1.0"
    )

    private interface ApiClient<P, T> {
        fun buildUrl(params: P): String
        fun parseResponse(response: String): Either<String, T>
    }

    private inner class WeatherApiClient : ApiClient<WeatherRequestParams, WeatherResponse> {
        override fun buildUrl(params: WeatherRequestParams): String {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val date = LocalDate.parse(params.baseDate, formatter)
            val nextDate = date.plusDays(1).format(formatter)

            val url = "${weatherBaseUrl}?serviceKey=${serviceKey}" +
                    "&numOfRows=1000" +
                    "&pageNo=1" +
                    "&base_date=${nextDate}" +
                    "&base_time=${params.baseTime}" +
                    "&nx=${params.nx}" +
                    "&ny=${params.ny}" +
                    "&dataType=JSON"
            logger.info("날씨 API URL 생성 완료: $url")
            return url
        }

        override fun parseResponse(response: String): Either<String, WeatherResponse> =
            runCatching {
                objectMapper.readValue<WeatherResponse>(response)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "날씨 응답 파싱 실패: ${it.message}".left() }
            )
    }

    private inner class DustForecastApiClient : ApiClient<DustForecastRequestParams, ForecastResponse> {
        override fun buildUrl(params: DustForecastRequestParams): String {
            val url = "${dustForecastBaseUrl}?serviceKey=${serviceKey}" +
                    "&returnType=json" +
                    "&numOfRows=100" +
                    "&pageNo=1" +
                    "&searchDate=${params.searchDate}" +
                    "&dataTerm=DAILY"
            logger.info("미세먼지 예보 API URL 생성 완료: $url")
            return url
        }

        override fun parseResponse(response: String): Either<String, ForecastResponse> =
            runCatching {
                objectMapper.readValue<ForecastResponse>(response)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "미세먼지 예보 응답 파싱 실패: ${it.message}".left() }
            )
    }

    private inner class RealTimeDustApiClient : ApiClient<RealTimeDustRequestParams, RealTimeDustResponse> {
        override fun buildUrl(params: RealTimeDustRequestParams): String {
            val encodedSidoName = URLEncoder.encode(params.sidoName, StandardCharsets.UTF_8.toString())
            val url = "${realTimeDustBaseUrl}?serviceKey=${serviceKey}" +
                    "&returnType=${params.returnType}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&pageNo=${params.pageNo}" +
                    "&sidoName=${encodedSidoName}" +
                    "&ver=${params.ver}"
            logger.info("실시간 미세먼지 API URL 생성 완료: $url")
            return url
        }

        override fun parseResponse(response: String): Either<String, RealTimeDustResponse> =
            runCatching {
                objectMapper.readValue<RealTimeDustResponse>(response)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "실시간 미세먼지 응답 파싱 실패: ${it.message}".left() }
            )
    }

    @Retryable(
        value = [SocketTimeoutException::class, IOException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, multiplier = 1.5)
    )
    private fun <P, T, R> makeApiRequest(
        client: ApiClient<P, T>,
        params: P,
        transform: (T) -> Either<String, R>
    ): ApiResult<R> {
        logger.info("API 요청 시작 - 파라미터: $params")
        logger.info("사용 중인 서비스키: $serviceKey")

        return try {
            val urlString = client.buildUrl(params)
            logger.info("최종 요청 URL: $urlString")

            val response = fetchDataFromApi(urlString).getOrElse { error ->
                return ApiResult.Error("데이터 가져오기 실패: $error")
            }

            logger.info("응답 수신 완료: ${response.take(500)}")

            if (response.trim().startsWith("<")) {
                handleXmlErrorResponse(response)
                return ApiResult.Error("API에서 XML 오류 응답을 반환했습니다")
            }

            client.parseResponse(response).flatMap(transform).fold(
                { error -> ApiResult.Error(error) },
                { result -> ApiResult.Success(result) }
            )
        } catch (e: Exception) {
            logger.error("API 요청 실패", e)
            ApiResult.Error("API 요청 실패: ${e.message}", e)
        }
    }

    private fun handleXmlErrorResponse(response: String) {
        when {
            response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") || response.contains("SERVICE ERROR") -> {
                logger.error("API 키 오류: {}", response)
                throw IllegalStateException("API 서비스 키 오류: 키가 유효하고 올바르게 형식화되었는지 확인하세요")
            }
            else -> {
                logger.error("XML 오류 응답 수신: {}", response.take(500))
            }
        }
    }

    @Retryable(
        value = [SocketTimeoutException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, multiplier = 1.5)
    )
    private fun fetchDataFromApi(urlString: String): Either<String, String> =
        Either.catch {
            URL(urlString).openConnection().let { conn ->
                (conn as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000 // 타임아웃을 15초로 증가
                    readTimeout = 15000   // 타임아웃을 15초로 증가
                    setRequestProperty("User-Agent", "KWeather/1.0 (your.email@example.com)")
                }

                val responseCode = conn.responseCode
                logger.info("응답 코드: $responseCode")

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
            logger.error("HTTP 요청 실패", e)
            "HTTP 요청 실패: ${e.message}"
        }

    private inline fun <T : AutoCloseable, R> use(resource: T, block: (T) -> R): R {
        try {
            return block(resource)
        } finally {
            try {
                resource.close()
            } catch (e: IOException) {
                logger.error("리소스 닫기 실패", e)
            }
        }
    }

    fun getUltraShortWeather(nx: Int, ny: Int): WeatherResponse {
        val baseDate = DateTimeUtils.getBaseDate()
        val baseTime = DateTimeUtils.getBaseTime()

        logger.info("사용 중인 기준날짜: $baseDate, 기준시간: $baseTime")

        val params = WeatherRequestParams(baseDate, baseTime, nx, ny)
        val weatherClient = WeatherApiClient()

        return when (val result = makeApiRequest(weatherClient, params) { response ->
            if (response.response?.header?.resultCode == "03" &&
                response.response.header.resultMsg == "NO_DATA") {
                logger.warn("${baseDate} ${baseTime}에 대한 날씨 데이터가 없습니다. 이전 시간으로 재시도합니다.")
                Either.Right(retryWithEarlierTime(nx, ny, baseDate) ?: response)
            } else {
                Either.Right(response)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("날씨 API 요청 실패: ${result.message}")
                WeatherResponse(WeatherResponseData(
                    header = Header("ERROR", "날씨 데이터 가져오기 실패: ${result.message}"),
                    body = null
                ))
            }
        }
    }

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
                logger.error("미세먼지 예보 API 요청 실패: ${result.message}")
                throw IllegalStateException("미세먼지 예보 데이터 가져오기 실패: ${result.message}")
            }
        }
    }

    fun getRealTimeDust(sidoName: String): List<RealTimeDustInfo> {
        val params = RealTimeDustRequestParams(sidoName = sidoName)
        val realTimeDustClient = RealTimeDustApiClient()

        return when (val result = makeApiRequest(realTimeDustClient, params) { response: RealTimeDustResponse ->
            val dustInfos = response.response?.body?.items?.mapNotNull { item ->
                item?.let { parseRealTimeDustItem(it) }
            } ?: emptyList<RealTimeDustInfo>()

            Either.Right(dustInfos)
        }) {
            is ApiResult.Success<List<RealTimeDustInfo>> -> result.data
            is ApiResult.Error -> {
                logger.error("실시간 미세먼지 API 요청 실패: ${result.message}")
                emptyList() // 실패 시 빈 리스트 반환
            }
        }
    }

    private fun parseRealTimeDustItem(item: RealTimeDustItem): RealTimeDustInfo? =
        runCatching {
            RealTimeDustInfo(
                sidoName = item.sidoName ?: throw IllegalArgumentException("시도명이 누락되었습니다"),
                stationName = item.stationName ?: throw IllegalArgumentException("측정소명이 누락되었습니다"),
                pm10Value = item.pm10Value ?: "N/A",
                pm10Grade = convertGrade(item.pm10Grade),
                pm25Value = item.pm25Value ?: "N/A",
                pm25Grade = convertGrade(item.pm25Grade),
                dataTime = item.dataTime ?: throw IllegalArgumentException("측정시간이 누락되었습니다")
            )
        }.onFailure { e ->
            logger.warn("유효하지 않은 실시간 미세먼지 항목을 건너뜁니다: ${e.message}")
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
                date = item.dataTime ?: throw IllegalArgumentException("데이터 시간이 누락되었습니다"),
                type = item.informCode ?: defaultInformCode,
                overall = item.informOverall ?: "N/A",
                cause = item.informCause ?: "N/A",
                grade = item.informGrade ?: "N/A",
                dataTime = item.dataTime ?: throw IllegalArgumentException("데이터 시간이 누락되었습니다"),
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
            logger.warn("유효하지 않은 예보 항목을 건너뜁니다: ${e.message}")
        }.getOrNull()

    private fun retryWithEarlierTime(nx: Int, ny: Int, baseDate: String): WeatherResponse? {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val retryTime = calculateRetryTime(now.hour, now.minute)

        logger.info("이전 시간으로 재시도: $retryTime")

        val params = WeatherRequestParams(baseDate, retryTime, nx, ny)
        val weatherClient = WeatherApiClient()

        return when (val result = makeApiRequest(weatherClient, params) { response ->
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

    fun parseWeatherData(response: WeatherResponse): List<WeatherInfo> =
        when {
            response.response?.header?.resultCode != "00" && response.response?.header?.resultCode != null -> {
                listOf(
                    WeatherInfo(
                        baseDate = "",
                        baseTime = "",
                        category = "ERROR",
                        value = "API 오류: ${response.response.header.resultCode} - ${response.response.header.resultMsg}",
                        unit = ""
                    )
                )
            }
            response.response?.body?.items?.item == null -> {
                logger.warn("날씨 응답에 항목이 포함되어 있지 않습니다. 응답: $response")
                emptyList()
            }
            else -> {
                response.response.body.items.item.mapNotNull { item ->
                    parseWeatherItem(item)
                }
            }
        }

    private fun parseWeatherItem(item: WeatherItem?): WeatherInfo? =
        item?.let {
            runCatching {
                WeatherInfo(
                    baseDate = it.baseDate ?: throw IllegalArgumentException("기준날짜가 누락되었습니다"),
                    baseTime = it.baseTime ?: throw IllegalArgumentException("기준시간이 누락되었습니다"),
                    category = it.category ?: throw IllegalArgumentException("카테고리가 누락되었습니다"),
                    value = it.obsrValue ?: throw IllegalArgumentException("관측값이 누락되었습니다"),
                    unit = getUnitForCategory(it.category)
                )
            }.onFailure { e ->
                logger.warn("유효하지 않은 날씨 항목을 건너뜁니다: ${e.message}")
            }.getOrNull()
        }

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

    private fun createAirQuality(): AirQuality =
        AirQuality(
            title = "미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "20",
            measurement = "㎍/㎥"
        )

    private fun createUVIndex(): UVIndex =
        UVIndex(
            title = "초미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "8",
            measurement = "㎍/㎥"
        )

    private fun createHourlyForecast(): List<HourlyForecast> =
        listOf(
            HourlyForecast("지금", "moon", "-1.8°C", "34%"),
            HourlyForecast("0시", "moon", "-6°C", "55%"),
            HourlyForecast("3시", "moon", "-6°C", "60%"),
            HourlyForecast("6시", "moon", "-7°C", "67%"),
            HourlyForecast("9시", "sun", "-6°C", "55%")
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
        logger.info("서비스 초기화 완료 - 서비스키: $serviceKey")
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serviceKey.isBlank()) {
            logger.warn("서비스 키가 비어있습니다. API 호출이 실패할 수 있습니다.")
        }
        if (weatherBaseUrl.isBlank()) {
            logger.warn("날씨 기본 URL이 비어있습니다. API 호출이 실패할 수 있습니다.")
        }
        if (dustForecastBaseUrl.isBlank()) {
            logger.warn("미세먼지 예보 기본 URL이 비어있습니다. API 호출이 실패할 수 있습니다.")
        }
        if (realTimeDustBaseUrl.isBlank()) {
            logger.warn("실시간 미세먼지 기본 URL이 비어있습니다. API 호출이 실패할 수 있습니다.")
        }
    }
}
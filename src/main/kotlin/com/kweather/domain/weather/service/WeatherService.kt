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
import arrow.core.left
import arrow.core.right
import com.kweather.domain.airstagnation.dto.AirStagnationIndexInfo
import com.kweather.domain.airstagnation.dto.AirStagnationIndexItem
import com.kweather.domain.airstagnation.dto.AirStagnationIndexRequestParams
import com.kweather.domain.airstagnation.dto.AirStagnationIndexResponse
import com.kweather.domain.realtime.dto.RealTimeDustInfo
import com.kweather.domain.realtime.dto.RealTimeDustItem
import com.kweather.domain.realtime.dto.RealTimeDustResponse
import com.kweather.domain.senta.dto.SenTaIndexInfo
import com.kweather.domain.senta.dto.SenTaIndexItem
import com.kweather.domain.senta.dto.SenTaIndexRequestParams
import com.kweather.domain.senta.dto.SenTaIndexResponse
import com.kweather.domain.uvi.dto.UVIndexInfo
import com.kweather.domain.uvi.dto.UVIndexItem
import com.kweather.domain.uvi.dto.UVIndexRequestParams
import com.kweather.domain.uvi.dto.UVIndexResponse
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
    @Value("\${api.livingwthridxservice.uv-base-url:}")
    private val uvIndexBaseUrl: String,
    @Value("\${api.livingwthridxservice.senta-base-url:}")
    private val senTaIndexBaseUrl: String,
    @Value("\${api.livingwthridxservice.airstagnation-base-url:}")
    private val airStagnationIndexBaseUrl: String,
    @Value("\${api.service-key:}")
    private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(WeatherService::class.java)

    data class RealTimeDustRequestParams(
        val returnType: String = "json",
        val numOfRows: Int = 50,
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

    private inner class UVIndexApiClient : ApiClient<UVIndexRequestParams, UVIndexResponse> {
        override fun buildUrl(params: UVIndexRequestParams): String {
            val url = "${uvIndexBaseUrl}?serviceKey=${serviceKey}" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}"
            logger.info("자외선 지수 API URL 생성 완료: $url")
            return url
        }

        override fun parseResponse(response: String): Either<String, UVIndexResponse> =
            runCatching {
                objectMapper.readValue<UVIndexResponse>(response)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "자외선 지수 응답 파싱 실패: ${it.message}".left() }
            )
    }

    private inner class SenTaIndexApiClient : ApiClient<SenTaIndexRequestParams, SenTaIndexResponse> {
        override fun buildUrl(params: SenTaIndexRequestParams): String {
            val url = "${senTaIndexBaseUrl}?serviceKey=${serviceKey}" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}" +
                    "&requestCode=${params.requestCode}"
            logger.info("여름철 체감온도 API URL 생성 완료: $url")
            return url
        }

        override fun parseResponse(response: String): Either<String, SenTaIndexResponse> =
            runCatching {
                objectMapper.readValue<SenTaIndexResponse>(response)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "여름철 체감온도 응답 파싱 실패: ${it.message}".left() }
            )
    }

    private inner class AirStagnationIndexApiClient : ApiClient<AirStagnationIndexRequestParams, AirStagnationIndexResponse> {
        override fun buildUrl(params: AirStagnationIndexRequestParams): String {
            val url = "${airStagnationIndexBaseUrl}?serviceKey=${serviceKey}" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}" +
                    (params.requestCode?.let { "&requestCode=$it" } ?: "")
            logger.info("대기정체지수 API URL 생성 완료: $url")
            return url
        }

        override fun parseResponse(response: String): Either<String, AirStagnationIndexResponse> =
            runCatching {
                objectMapper.readValue<AirStagnationIndexResponse>(response)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "대기정체지수 응답 파싱 실패: ${it.message}".left() }
            )
    }

    @Retryable(
        value = [SocketTimeoutException::class, IOException::class],
        maxAttempts = 5,
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

            val response = fetchDataFromApi(urlString).fold(
                { error -> return ApiResult.Error("데이터 가져오기 실패: $error") },
                { it }
            )

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
                    connectTimeout = 15000
                    readTimeout = 15000
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
            is ApiResult.Success -> result.data
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
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("실시간 미세먼지 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    fun getUVIndex(areaNo: String, time: String): List<UVIndexInfo> {
        val params = UVIndexRequestParams(areaNo = areaNo, time = time)
        val uvIndexClient = UVIndexApiClient()

        return when (val result = makeApiRequest(uvIndexClient, params) { response: UVIndexResponse ->
            val uvIndexInfos = response.response?.body?.items?.mapNotNull { item ->
                item?.let { parseUVIndexItem(it) }
            } ?: emptyList<UVIndexInfo>()
            Either.Right(uvIndexInfos)
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("자외선 지수 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    fun getSenTaIndex(areaNo: String, time: String): List<SenTaIndexInfo> {
        val params = SenTaIndexRequestParams(areaNo = areaNo, time = time)
        val senTaIndexClient = SenTaIndexApiClient()

        return when (val result = makeApiRequest(senTaIndexClient, params) { response: SenTaIndexResponse ->
            val senTaIndexInfos = response.response?.body?.items?.mapNotNull { item ->
                item?.let { parseSenTaIndexItem(it) }
            } ?: emptyList<SenTaIndexInfo>()
            Either.Right(senTaIndexInfos)
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("여름철 체감온도 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    fun getAirStagnationIndex(areaNo: String, time: String): List<AirStagnationIndexInfo> {
        val params = AirStagnationIndexRequestParams(areaNo = areaNo, time = time, pageNo = 1, numOfRows = 10, dataType = "json")
        val airStagnationIndexClient = AirStagnationIndexApiClient()

        return when (val result = makeApiRequest(airStagnationIndexClient, params) { response: AirStagnationIndexResponse ->
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                logger.warn("대기정체지수 데이터 없음: time=$time, 이전 시간으로 재시도")
                val previousTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
                val retryParams = AirStagnationIndexRequestParams(areaNo = areaNo, time = previousTime, pageNo = 1, numOfRows = 10, dataType = "json")
                val retryResult = makeApiRequest(airStagnationIndexClient, retryParams) { retryResponse: AirStagnationIndexResponse ->
                    val airStagnationIndexInfos = retryResponse.response?.body?.items?.mapNotNull { item ->
                        item?.let { parseAirStagnationIndexItem(it) }
                    } ?: emptyList<AirStagnationIndexInfo>()
                    Either.Right(airStagnationIndexInfos)
                }
                when (retryResult) {
                    is ApiResult.Success -> Either.Right(retryResult.data)
                    is ApiResult.Error -> Either.Right(emptyList<AirStagnationIndexInfo>())
                }
            } else {
                val airStagnationIndexInfos = response.response?.body?.items?.mapNotNull { item ->
                    item?.let { parseAirStagnationIndexItem(it) }
                } ?: emptyList<AirStagnationIndexInfo>()
                Either.Right(airStagnationIndexInfos)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("대기정체지수 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    private fun parseRealTimeDustItem(item: RealTimeDustItem): RealTimeDustInfo? =
        runCatching {
            RealTimeDustInfo(
                sidoName = item.sidoName ?: throw IllegalArgumentException("시도명이 누락되었습니다"),
                stationName = item.stationName ?: throw IllegalArgumentException("측정소명이 누락되었습니다"),
                pm10Value = item.pm10Value?.takeIf { it != "-" } ?: "N/A",
                pm10Grade = convertGrade(item.pm10Grade),
                pm25Value = item.pm25Value?.takeIf { it != "-" } ?: "N/A",
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

    private fun parseUVIndexItem(item: UVIndexItem): UVIndexInfo? =
        runCatching {
            val values = mutableMapOf<String, String>()
            listOf("h0", "h3", "h6", "h9", "h12", "h15", "h18", "h21", "h24", "h27", "h30", "h33", "h36", "h39", "h42", "h45", "h48", "h51", "h54", "h57", "h60", "h63", "h66", "h69", "h72", "h75").forEach { key ->
                val value = when (key) {
                    "h0" -> item.h0
                    "h3" -> item.h3
                    "h6" -> item.h6
                    "h9" -> item.h9
                    "h12" -> item.h12
                    "h15" -> item.h15
                    "h18" -> item.h18
                    "h21" -> item.h21
                    "h24" -> item.h24
                    "h27" -> item.h27
                    "h30" -> item.h30
                    "h33" -> item.h33
                    "h36" -> item.h36
                    "h39" -> item.h39
                    "h42" -> item.h42
                    "h45" -> item.h45
                    "h48" -> item.h48
                    "h51" -> item.h51
                    "h54" -> item.h54
                    "h57" -> item.h57
                    "h60" -> item.h60
                    "h63" -> item.h63
                    "h66" -> item.h66
                    "h69" -> item.h69
                    "h72" -> item.h72
                    "h75" -> item.h75
                    else -> null
                }
                if (!value.isNullOrEmpty()) {
                    values[key] = value
                } else {
                    logger.debug("UVIndexItem 필드 $key 값이 null 또는 비어있습니다")
                }
            }
            UVIndexInfo(
                date = item.date ?: throw IllegalArgumentException("날짜가 누락되었습니다"),
                values = values
            )
        }.onFailure { e ->
            logger.warn("유효하지 않은 자외선 지수 항목을 건너뜁니다: ${e.message}")
        }.getOrNull()

    private fun parseSenTaIndexItem(item: SenTaIndexItem): SenTaIndexInfo? =
        runCatching {
            val values = mutableMapOf<String, String>()
            listOf("h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8", "h9", "h10", "h11", "h12", "h13", "h14", "h15", "h16", "h17", "h18", "h19", "h20", "h21", "h22", "h23", "h24", "h25", "h26", "h27", "h28", "h29", "h30", "h31").forEach { key ->
                val value = when (key) {
                    "h1" -> item.h1
                    "h2" -> item.h2
                    "h3" -> item.h3
                    "h4" -> item.h4
                    "h5" -> item.h5
                    "h6" -> item.h6
                    "h7" -> item.h7
                    "h8" -> item.h8
                    "h9" -> item.h9
                    "h10" -> item.h10
                    "h11" -> item.h11
                    "h12" -> item.h12
                    "h13" -> item.h13
                    "h14" -> item.h14
                    "h15" -> item.h15
                    "h16" -> item.h16
                    "h17" -> item.h17
                    "h18" -> item.h18
                    "h19" -> item.h19
                    "h20" -> item.h20
                    "h21" -> item.h21
                    "h22" -> item.h22
                    "h23" -> item.h23
                    "h24" -> item.h24
                    "h25" -> item.h25
                    "h26" -> item.h26
                    "h27" -> item.h27
                    "h28" -> item.h28
                    "h29" -> item.h29
                    "h30" -> item.h30
                    "h31" -> item.h31
                    else -> null
                }
                if (!value.isNullOrEmpty()) {
                    values[key] = value
                } else {
                    logger.debug("SenTaIndexItem 필드 $key 값이 null 또는 비어있습니다")
                }
            }
            SenTaIndexInfo(
                date = item.date ?: throw IllegalArgumentException("날짜가 누락되었습니다"),
                values = values
            )
        }.onFailure { e ->
            logger.warn("유효하지 않은 체감온도 항목을 건너뜁니다: ${e.message}")
        }.getOrNull()

    private fun parseAirStagnationIndexItem(item: AirStagnationIndexItem): AirStagnationIndexInfo? =
        runCatching {
            val values = mutableMapOf<String, String>()
            listOf("h3", "h6", "h9", "h12", "h15", "h18", "h21", "h24", "h27", "h30", "h33", "h36", "h39", "h42", "h45", "h48", "h51", "h54", "h57", "h60", "h63", "h66", "h69", "h72", "h75", "h78").forEach { key ->
                val value = when (key) {
                    "h3" -> item.h3
                    "h6" -> item.h6
                    "h9" -> item.h9
                    "h12" -> item.h12
                    "h15" -> item.h15
                    "h18" -> item.h18
                    "h21" -> item.h21
                    "h24" -> item.h24
                    "h27" -> item.h27
                    "h30" -> item.h30
                    "h33" -> item.h33
                    "h36" -> item.h36
                    "h39" -> item.h39
                    "h42" -> item.h42
                    "h45" -> item.h45
                    "h48" -> item.h48
                    "h51" -> item.h51
                    "h54" -> item.h54
                    "h57" -> item.h57
                    "h60" -> item.h60
                    "h63" -> item.h63
                    "h66" -> item.h66
                    "h69" -> item.h69
                    "h72" -> item.h72
                    "h75" -> item.h75
                    "h78" -> item.h78
                    else -> null
                }
                if (!value.isNullOrEmpty()) {
                    values[key] = value
                } else {
                    logger.debug("AirStagnationIndexItem 필드 $key 값이 null 또는 비어있습니다")
                }
            }
            AirStagnationIndexInfo(
                date = item.date ?: throw IllegalArgumentException("날짜가 누락되었습니다"),
                values = values
            )
        }.onFailure { e ->
            logger.warn("유효하지 않은 대기정체지수 항목을 건너뜁니다: ${e.message}")
        }.getOrNull()

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
        // 기본 날씨 데이터 가져오기
        val response = getUltraShortWeather(nx, ny)
        val weatherInfoList = parseWeatherData(response)

        // 실시간 데이터 가져오기
        val sidoName = "서울"
        val areaNo = "1100000000"
        val apiTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val realTimeDust = getRealTimeDust(sidoName)
        val uvIndexData = getUVIndex(areaNo, apiTime)
        val senTaIndexData = getSenTaIndex(areaNo, apiTime)

        // 현재 날짜와 시간
        val (date, time) = DateTimeUtils.getCurrentDateTimeFormatted()
        val currentHour = DateTimeUtils.getCurrentHour()

        // 온도 데이터
        val temperature = weatherInfoList.find { it.category == "T1H" }?.value?.let { "$it°C" } ?: "-1.8°C"
        val humidity = weatherInfoList.find { it.category == "REH" }?.value?.let { "$it%" } ?: "34%"
        val windSpeed = weatherInfoList.find { it.category == "WSD" }?.value?.let { "${it}m/s" } ?: "1km/초(남서) m/s 0"

        // 날씨 조건
        val weatherCondition = weatherInfoList.find { it.category == "PTY" }?.value?.let {
            when (it) {
                "0" -> "맑음"
                "1" -> "비"
                "2" -> "비/눈"
                "3" -> "눈"
                else -> "맑음"
            }
        } ?: "맑음"

        // 미세먼지 데이터 (PM10)
        val airQuality = realTimeDust.firstOrNull()?.let {
            AirQuality(
                title = "미세먼지",
                icon = "yellow-smiley",
                status = it.pm10Grade,
                value = it.pm10Value,
                measurement = "㎍/㎥"
            )
        } ?: createDefaultAirQuality()

        // 초미세먼지 데이터 (PM2.5)
        val uvIndex = realTimeDust.firstOrNull()?.let {
            UVIndex(
                title = "초미세먼지",
                icon = "yellow-smiley",
                status = it.pm25Grade,
                value = it.pm25Value,
                measurement = "㎍/㎥"
            )
        } ?: createDefaultUVIndex()

        // 시간대별 예보 (실제 데이터 기반으로 동적 생성)
        val hourlyForecast = mutableListOf<HourlyForecast>()
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val currentHourIndex = currentHour / 3 * 3 // 3시간 단위로 맞춤
        val hours = listOf(0, 3, 6, 9, 12, 15, 18, 21)

        hours.forEachIndexed { index, hourOffset ->
            val forecastHour = (currentHourIndex + hourOffset) % 24
            val forecastTime = if (hourOffset == 0) "지금" else "${forecastHour}시"
            val forecastIcon = if (forecastHour in 6..18) "sun" else "moon"
            val forecastTemp = senTaIndexData.firstOrNull()?.values?.get("h${hourOffset + 1}")?.let { "$it°C" } ?: "-${index + 1}°C"
            val forecastHumidity = humidity // 실제 데이터로 대체 가능
            hourlyForecast.add(HourlyForecast(forecastTime, forecastIcon, forecastTemp, forecastHumidity))
        }

        return Weather(
            date = date,
            time = time,
            location = "한남동 (용산구)",
            currentTemperature = temperature,
            highLowTemperature = "-7°C / -1°C", // 실제 데이터로 대체 가능
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
            title = "초미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "8",
            measurement = "㎍/㎥"
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
        if (uvIndexBaseUrl.isBlank()) {
            logger.warn("자외선 지수 기본 URL이 비어있습니다. API 호출이 실패할 수 있습니다.")
        }
        if (senTaIndexBaseUrl.isBlank()) {
            logger.warn("여름철 체감온도 기본 URL이 비어있습니다. API 호출이 실패할 수 있습니다.")
        }
        if (airStagnationIndexBaseUrl.isBlank()) {
            logger.warn("대기정체지수 기본 URL이 비어있습니다. API 호출이 실패할 수 있습니다.")
        }
    }
}
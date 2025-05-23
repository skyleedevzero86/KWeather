package com.kweather.domain.weather.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kweather.domain.forecast.dto.DustForecastRequestParams
import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.forecast.dto.ForecastItem
import com.kweather.domain.forecast.dto.ForecastResponse
import com.kweather.domain.weather.dto.*
import com.kweather.domain.weather.entity.Weather
import com.kweather.global.common.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import java.net.SocketTimeoutException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate
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
import java.nio.charset.StandardCharsets

@Service
class WeatherService(
    @Value("\${api.weather.base-url:}") private val weatherBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.base-url:}") private val dustForecastBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.real-time-base-url:}") private val realTimeDustBaseUrl: String,
    @Value("\${api.livingwthridxservice.uv-base-url:}") private val uvIndexBaseUrl: String,
    @Value("\${api.livingwthridxservice.senta-base-url:}") private val senTaIndexBaseUrl: String,
    @Value("\${api.livingwthridxservice.airstagnation-base-url:}") private val airStagnationIndexBaseUrl: String,
    @Value("\${api.service-key:}") private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(WeatherService::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    }

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

            return "${weatherBaseUrl}?serviceKey=$serviceKey" +
                    "&numOfRows=1000" +
                    "&pageNo=1" +
                    "&base_date=$nextDate" +
                    "&base_time=${params.baseTime}" +
                    "&nx=${params.nx}" +
                    "&ny=${params.ny}" +
                    "&dataType=JSON"
        }

        override fun parseResponse(response: String): Either<String, WeatherResponse> =
            runCatching {
                objectMapper.readValue(response, WeatherResponse::class.java)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "날씨 응답 파싱 실패: ${it.message}".left() }
            )
    }

    private inner class DustForecastApiClient : ApiClient<DustForecastRequestParams, ForecastResponse> {
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
                objectMapper.readValue(response, ForecastResponse::class.java)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "미세먼지 예보 응답 파싱 실패: ${it.message}".left() }
            )
    }

    private inner class RealTimeDustApiClient : ApiClient<RealTimeDustRequestParams, RealTimeDustResponse> {
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
                objectMapper.readValue(response, RealTimeDustResponse::class.java)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "실시간 미세먼지 응답 파싱 실패: ${it.message}".left() }
            )
    }

    private inner class UVIndexApiClient : ApiClient<UVIndexRequestParams, UVIndexResponse> {
        override fun buildUrl(params: UVIndexRequestParams): String {
            return "${uvIndexBaseUrl}?serviceKey=$serviceKey" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}"
        }

        override fun parseResponse(response: String): Either<String, UVIndexResponse> =
            runCatching {
                objectMapper.readValue(response, UVIndexResponse::class.java)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "자외선 지수 응답 파싱 실패: ${it.message}".left() }
            )
    }

    private inner class SenTaIndexApiClient : ApiClient<SenTaIndexRequestParams, SenTaIndexResponse> {
        override fun buildUrl(params: SenTaIndexRequestParams): String {
            return "${senTaIndexBaseUrl}?serviceKey=$serviceKey" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}" +
                    "&requestCode=${params.requestCode}"
        }

        override fun parseResponse(response: String): Either<String, SenTaIndexResponse> =
            runCatching {
                objectMapper.readValue(response, SenTaIndexResponse::class.java)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "여름철 체감온도 응답 파싱 실패: ${it.message}".left() }
            )
    }

    private inner class AirStagnationIndexApiClient : ApiClient<AirStagnationIndexRequestParams, AirStagnationIndexResponse> {
        override fun buildUrl(params: AirStagnationIndexRequestParams): String {
            return "${airStagnationIndexBaseUrl}?serviceKey=$serviceKey" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}" +
                    (params.requestCode?.let { "&requestCode=$it" } ?: "")
        }

        override fun parseResponse(response: String): Either<String, AirStagnationIndexResponse> =
            runCatching {
                objectMapper.readValue(response, AirStagnationIndexResponse::class.java)
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
        logger.info("API 요청 시작: $params")
        return try {
            val urlString = client.buildUrl(params)
            val response = fetchDataFromApi(urlString).fold(
                { error -> return ApiResult.Error("데이터 가져오기 실패: $error") },
                { it }
            )
            if (response.trim().startsWith("<")) {
                logger.error("XML 오류 응답 수신: $response")
                return ApiResult.Error("API에서 XML 오류 응답을 반환했습니다")
            }
            client.parseResponse(response).flatMap(transform).fold(
                { error -> ApiResult.Error(error) },
                { result -> ApiResult.Success(result) }
            )
        } catch (e: Exception) {
            logger.error("API 요청 실패: ${e.message}", e)
            ApiResult.Error("API 요청 실패: ${e.message}", e)
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
                    setRequestProperty("User-Agent", "KWeather/1.0")
                }
                val responseCode = conn.responseCode
                val reader = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(conn.inputStream))
                } else {
                    BufferedReader(InputStreamReader(conn.errorStream))
                }
                use(reader) { r -> r.lines().collect(java.util.stream.Collectors.joining()) }
            }
        }.mapLeft { e ->
            logger.error("HTTP 요청 실패: ${e.message}", e)
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
        val params = WeatherRequestParams(baseDate, baseTime, nx, ny)
        val weatherClient = WeatherApiClient()

        return when (val result = makeApiRequest(weatherClient, params) { response ->
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

        return when (val result = makeApiRequest(dustClient, params) { response ->
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                logger.info("미세먼지 예보 데이터 없음, 전날 데이터로 재시도")
                val previousDate = LocalDate.parse(targetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val retryParams = DustForecastRequestParams(previousDate, informCode)
                when (val retryResult = makeApiRequest(dustClient, retryParams) { retryResponse ->
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

        return when (val result = makeApiRequest(realTimeDustClient, params) { response ->
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

    fun getUVIndex(areaNo: String, time: String): List<UVIndexInfo> {
        val params = UVIndexRequestParams(areaNo = areaNo, time = time)
        val uvIndexClient = UVIndexApiClient()

        return when (val result = makeApiRequest(uvIndexClient, params) { response ->
            val uvIndexInfos = response.response?.body?.items?.mapNotNull { item ->
                item?.let { parseUVIndexItem(it) }
            } ?: emptyList()
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
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        if (now.monthValue !in 5..9) {
            logger.info("여름철 체감온도 데이터는 5~9월에만 제공됩니다. 현재 월: ${now.monthValue}")
            return emptyList()
        }

        val params = SenTaIndexRequestParams(areaNo = areaNo, time = time)
        val senTaIndexClient = SenTaIndexApiClient()

        return when (val result = makeApiRequest(senTaIndexClient, params) { response ->
            val items = response.response?.body?.items
            if (items.isNullOrEmpty()) {
                logger.info("체감온도 데이터 없음, 이전 시간으로 재시도")
                val previousTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
                val retryParams = SenTaIndexRequestParams(areaNo = areaNo, time = previousTime)
                when (val retryResult = makeApiRequest(senTaIndexClient, retryParams) { retryResponse ->
                    val senTaIndexInfos = retryResponse.response?.body?.items?.mapNotNull { item ->
                        item?.let { parseSenTaIndexItem(it) }
                    } ?: emptyList()
                    Either.Right(senTaIndexInfos)
                }) {
                    is ApiResult.Success -> Either.Right(retryResult.data)
                    is ApiResult.Error -> Either.Right(emptyList())
                }
            } else {
                val senTaIndexInfos = items.mapNotNull { item -> item?.let { parseSenTaIndexItem(it) } }
                Either.Right(senTaIndexInfos)
            }
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

        return when (val result = makeApiRequest(airStagnationIndexClient, params) { response ->
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                logger.info("대기정체지수 데이터 없음, 이전 시간으로 재시도")
                val previousTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
                val retryParams = AirStagnationIndexRequestParams(areaNo = areaNo, time = previousTime, pageNo = 1, numOfRows = 10, dataType = "json")
                when (val retryResult = makeApiRequest(airStagnationIndexClient, retryParams) { retryResponse ->
                    val airStagnationIndexInfos = retryResponse.response?.body?.items?.mapNotNull { item ->
                        item?.let { parseAirStagnationIndexItem(it) }
                    } ?: emptyList()
                    Either.Right(airStagnationIndexInfos)
                }) {
                    is ApiResult.Success -> Either.Right(retryResult.data)
                    is ApiResult.Error -> Either.Right(emptyList())
                }
            } else {
                val airStagnationIndexInfos = response.response?.body?.items?.mapNotNull { item ->
                    item?.let { parseAirStagnationIndexItem(it) }
                } ?: emptyList()
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

    private fun parseUVIndexItem(item: UVIndexItem): UVIndexInfo? =
        runCatching {
            val values = mutableMapOf<String, String>()
            with(item) {
                listOf(
                    "h0" to h0, "h3" to h3, "h6" to h6, "h9" to h9,
                    "h12" to h12, "h15" to h15, "h18" to h18, "h21" to h21,
                    "h24" to h24, "h27" to h27, "h30" to h30, "h33" to h33,
                    "h36" to h36, "h39" to h39, "h42" to h42, "h45" to h45,
                    "h48" to h48, "h51" to h51, "h54" to h54, "h57" to h57,
                    "h60" to h60, "h63" to h63, "h66" to h66, "h69" to h69,
                    "h72" to h72, "h75" to h75
                ).forEach { (key, value) -> value?.takeIf { it.isNotEmpty() }?.let { values[key] = it } }
            }
            if (values.isEmpty()) return null
            UVIndexInfo(
                date = item.date ?: return null,
                values = values
            )
        }.onFailure { e ->
            logger.warn("자외선 지수 항목 파싱 실패: ${e.message}")
        }.getOrNull()

    private fun parseSenTaIndexItem(item: SenTaIndexItem): SenTaIndexInfo? =
        runCatching {
            val values = mutableMapOf<String, String>()
            with(item) {
                listOf(
                    "h1" to h1, "h2" to h2, "h3" to h3, "h4" to h4,
                    "h5" to h5, "h6" to h6, "h7" to h7, "h8" to h8,
                    "h9" to h9, "h10" to h10, "h11" to h11, "h12" to h12,
                    "h13" to h13, "h14" to h14, "h15" to h15, "h16" to h16,
                    "h17" to h17, "h18" to h18, "h19" to h19, "h20" to h20,
                    "h21" to h21, "h22" to h22, "h23" to h23, "h24" to h24,
                    "h25" to h25, "h26" to h26, "h27" to h27, "h28" to h28,
                    "h29" to h29, "h30" to h30, "h31" to h31, "h32" to h32
                ).forEach { (key, value) -> value?.takeIf { it.isNotEmpty() }?.let { values[key] = it } }
            }
            if (values.isEmpty()) return null
            SenTaIndexInfo(
                date = item.date ?: return null,
                values = values
            )
        }.onFailure { e ->
            logger.warn("체감온도 항목 파싱 실패: ${e.message}")
        }.getOrNull()

    private fun parseAirStagnationIndexItem(item: AirStagnationIndexItem): AirStagnationIndexInfo? =
        runCatching {
            val values = mutableMapOf<String, String>()
            with(item) {
                listOf(
                    "h3" to h3, "h6" to h6, "h9" to h9, "h12" to h12,
                    "h15" to h15, "h18" to h18, "h21" to h21, "h24" to h24,
                    "h27" to h27, "h30" to h30, "h33" to h33, "h36" to h36,
                    "h39" to h39, "h42" to h42, "h45" to h45, "h48" to h48,
                    "h51" to h51, "h54" to h54, "h57" to h57, "h60" to h60,
                    "h63" to h63, "h66" to h66, "h69" to h69, "h72" to h72,
                    "h75" to h75, "h78" to h78
                ).forEach { (key, value) -> value?.takeIf { it.isNotEmpty() }?.let { values[key] = it } }
            }
            if (values.isEmpty()) return null
            AirStagnationIndexInfo(
                date = item.date ?: return null,
                values = values
            )
        }.onFailure { e ->
            logger.warn("대기정체지수 항목 파싱 실패: ${e.message}")
        }.getOrNull()

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

    fun buildWeatherEntity(nx: Int, ny: Int): Weather {
        val response = getUltraShortWeather(nx, ny)
        val weatherInfoList = parseWeatherData(response)
        val sidoName = "서울"
        val areaNo = "1100000000"
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val apiTime = now.minusHours(now.hour % 3L).format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val realTimeDust = getRealTimeDust(sidoName)
        val uvIndexData = getUVIndex(areaNo, apiTime)
        val senTaIndexData = getSenTaIndex(areaNo, apiTime)
        val airStagnationIndexData = getAirStagnationIndex(areaNo, apiTime)

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
        if (serviceKey.isBlank() || weatherBaseUrl.isBlank() || dustForecastBaseUrl.isBlank() ||
            realTimeDustBaseUrl.isBlank() || uvIndexBaseUrl.isBlank() || senTaIndexBaseUrl.isBlank() ||
            airStagnationIndexBaseUrl.isBlank()) {
            logger.error("필수 설정값이 누락되었습니다")
        }
    }
}
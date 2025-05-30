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
import com.kweather.domain.uvi.dto.UVIndex
import com.kweather.domain.weather.dto.*
import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.weather.model.*
import com.kweather.global.common.util.ApiClientUtility
import com.kweather.global.common.util.DateTimeUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

@Service
class GeneralWeatherService(
    @Value("\${api.weather.base-url:}") private val weatherBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.base-url:}") private val dustForecastBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.real-time-base-url:}") private val realTimeDustBaseUrl: String,
    @Value("\${api.service-key:}") private val serviceKey: String,
    private val senTaIndexService: SenTaIndexService,
    private val airStagnationIndexService: AirStagnationIndexService
) {
    private val logger = LoggerFactory.getLogger(GeneralWeatherService::class.java)

    data class RealTimeDustRequestParams(
        val returnType: String = "json",
        val numOfRows: Int = 1000,
        val pageNo: Int = 1,
        val sidoName: String,
        val ver: String = "1.0"
    )

    //추후확인
    private inner class WeatherApiClient : ApiClientUtility.ApiClient<WeatherRequestParams, WeatherResponse> {
        override fun buildUrl(params: WeatherRequestParams): String {
            // "&base_time=${params.baseTime}" +
            val urls = "${weatherBaseUrl}?serviceKey=$serviceKey" +
                    "&numOfRows=1000" +
                    "&pageNo=1" +
                    "&base_date=${params.baseDate}" +
                    "&base_time=0500" +
                    "&nx=${params.nx}" +
                    "&ny=${params.ny}" +
                    "&dataType=JSON"
            logger.info("날씨 API 요청 URL: $urls")
            return urls
        }

        override fun parseResponse(response: String): Either<String, WeatherResponse> {
            logger.info("API 응답 본문: $response")
            return try {
                if (response.trim().startsWith("<")) {
                    val dbFactory = DocumentBuilderFactory.newInstance()
                    val dBuilder = dbFactory.newDocumentBuilder()
                    val doc = dBuilder.parse(response.byteInputStream())
                    doc.documentElement.normalize()
                    val errMsg = doc.getElementsByTagName("errMsg").item(0)?.textContent ?: "Unknown error"
                    val returnAuthMsg = doc.getElementsByTagName("returnAuthMsg").item(0)?.textContent ?: "Unknown auth error"
                    logger.error("XML 오류 응답 수신: $errMsg - $returnAuthMsg")
                    throw IllegalStateException("API 오류: $errMsg - $returnAuthMsg")
                } else if (response.trim().startsWith("{")) {
                    val weatherResponse = ApiClientUtility.getObjectMapper().readValue(response, WeatherResponse::class.java)
                    if (weatherResponse.response?.header?.resultCode != "00") {
                        logger.error("JSON 오류 응답: ${weatherResponse.response?.header?.resultCode} - ${weatherResponse.response?.header?.resultMsg}")
                        throw IllegalStateException("API 오류: ${weatherResponse.response?.header?.resultCode} - ${weatherResponse.response?.header?.resultMsg}")
                    }
                    logger.info("JSON 응답 파싱 성공: ${weatherResponse.response?.body?.items?.size ?: 0} 개 항목")
                    Either.Right(weatherResponse)
                } else {
                    throw IllegalStateException("알 수 없는 응답 형식")
                }
            } catch (e: Exception) {
                Either.Left("날씨 응답 파싱 실패: ${e.message}")
            }
        }
    }

    private inner class DustForecastApiClient : ApiClientUtility.ApiClient<DustForecastRequestParams, ForecastResponse> {
        override fun buildUrl(params: DustForecastRequestParams): String {
            val urls = "${dustForecastBaseUrl}?serviceKey=$serviceKey" +
                    "&returnType=json" +
                    "&numOfRows=1000" +
                    "&pageNo=1" +
                    "&searchDate=${params.searchDate}" +
                    "&dataTerm=DAILY"
            logger.info("미세먼지 예보 API 요청 URL: $urls")
            return urls
        }

        override fun parseResponse(response: String): Either<String, ForecastResponse> =
            runCatching {
                if (response.trim().startsWith("<")) {
                    val dbFactory = DocumentBuilderFactory.newInstance()
                    val dBuilder = dbFactory.newDocumentBuilder()
                    val doc = dBuilder.parse(response.byteInputStream())
                    doc.documentElement.normalize()
                    val errMsg = doc.getElementsByTagName("errMsg").item(0)?.textContent ?: "Unknown error"
                    val returnAuthMsg = doc.getElementsByTagName("returnAuthMsg").item(0)?.textContent ?: "Unknown auth error"
                    throw IllegalStateException("API 오류: $errMsg - $returnAuthMsg")
                }

                val forecastResponse = ApiClientUtility.getObjectMapper().readValue(response, ForecastResponse::class.java)
                if (forecastResponse.response?.header?.resultCode != "00") {
                    throw IllegalStateException("API 오류: ${forecastResponse.response?.header?.resultCode} - ${forecastResponse.response?.header?.resultMsg}")
                }
                forecastResponse
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("미세먼지 예보 응답 파싱 실패: ${it.message}") }
            )
    }

    private inner class RealTimeDustApiClient : ApiClientUtility.ApiClient<RealTimeDustRequestParams, RealTimeDustResponse> {
        override fun buildUrl(params: RealTimeDustRequestParams): String {
            val encodedSidoName = URLEncoder.encode(params.sidoName, "UTF-8")
            val urls = "${realTimeDustBaseUrl}?serviceKey=$serviceKey" +
                    "&returnType=${params.returnType}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&pageNo=${params.pageNo}" +
                    "&sidoName=$encodedSidoName" +
                    "&ver=${params.ver}"
            logger.info("실시간 미세먼지 API 요청 URL: $urls")
            return urls
        }

        override fun parseResponse(response: String): Either<String, RealTimeDustResponse> {
            logger.info("실시간 미세먼지 API 응답 본문: $response")
            return try {
                if (response.trim().startsWith("<")) {
                    val dbFactory = DocumentBuilderFactory.newInstance()
                    val dBuilder = dbFactory.newDocumentBuilder()
                    val doc = dBuilder.parse(response.byteInputStream())
                    doc.documentElement.normalize()
                    val errMsg = doc.getElementsByTagName("errMsg").item(0)?.textContent ?: "Unknown error"
                    val returnAuthMsg = doc.getElementsByTagName("returnAuthMsg").item(0)?.textContent ?: "Unknown auth error"
                    logger.error("XML 오류 응답 수신: $errMsg - $returnAuthMsg")
                    throw IllegalStateException("API 오류: $errMsg - $returnAuthMsg")
                } else if (response.trim().startsWith("{")) {
                    val dustResponse = ApiClientUtility.getObjectMapper().readValue(response, RealTimeDustResponse::class.java)
                    if (dustResponse.response?.header?.resultCode != "00") {
                        logger.error("JSON 오류 응답: ${dustResponse.response?.header?.resultCode} - ${dustResponse.response?.header?.resultMsg}")
                        throw IllegalStateException("API 오류: ${dustResponse.response?.header?.resultCode} - ${dustResponse.response?.header?.resultMsg}")
                    }
                    logger.info("JSON 응답 파싱 성공: ${dustResponse.response?.body?.items?.size ?: 0} 개 항목")
                    Either.Right(dustResponse)
                } else {
                    throw IllegalStateException("알 수 없는 응답 형식")
                }
            } catch (e: Exception) {
                Either.Left("실시간 미세먼지 응답 파싱 실패: ${e.message}")
            }
        }
    }

    fun getUltraShortWeather(nx: Int, ny: Int): WeatherResponse {
        val baseDate = DateTimeUtils.getBaseDate()
        val baseTime = DateTimeUtils.getBaseTime()
        logger.info("초기 요청: baseDate=$baseDate, baseTime=$baseTime, nx=$nx, ny=$ny")
        val params = WeatherRequestParams(baseDate, baseTime, nx, ny)
        val weatherClient = WeatherApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(weatherClient, params) { response ->
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                logger.info("데이터 없음, 이전 시간으로 재시도: $baseDate $baseTime")
                val retryResponse = retryWithEarlierTime(nx, ny, baseDate, maxRetries = 3)
                Either.Right(retryResponse ?: response)
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
        val apiSidoName = convertSidoForApi(sidoName)
        val params = RealTimeDustRequestParams(sidoName = apiSidoName)
        val realTimeDustClient = RealTimeDustApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(realTimeDustClient, params) { response ->
            logger.info("실시간 미세먼지 API 응답: $response")
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                logger.warn("실시간 미세먼지 데이터 없음: $apiSidoName")
                Either.Right(emptyList())
            } else {
                val dustInfos = response.response?.body?.items?.mapNotNull { item ->
                    item?.let { parseRealTimeDustItem(it) }
                } ?: emptyList()
                logger.info("실시간 미세먼지 데이터: $dustInfos")
                Either.Right(dustInfos)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("실시간 미세먼지 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    private fun convertSidoForApi(sidoName: String): String = when (sidoName) {
        "서울특별시" -> "서울"
        "경기도" -> "경기"
        "인천광역시" -> "인천"
        "강원특별자치도" -> "강원"
        "충청북도" -> "충북"
        "충청남도" -> "충남"
        "대전광역시" -> "대전"
        "세종특별자치시" -> "세종"
        "전북특별자치도" -> "전북"
        "전라남도" -> "전남"
        "광주광역시" -> "광주"
        "경상북도" -> "경북"
        "경상남도" -> "경남"
        "대구광역시" -> "대구"
        "부산광역시" -> "부산"
        "울산광역시" -> "울산"
        "제주특별자치도" -> "제주"
        else -> sidoName
    }

    fun parseWeatherData(response: WeatherResponse): List<WeatherInfo> =
        when {
            response.response?.header?.resultCode != "00" -> {
                logger.error("API 응답 오류: ${response.response?.header?.resultCode} - ${response.response?.header?.resultMsg}")
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
                value = item.fcstValue ?: return null,
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
                pm10Value = item.pm10Value?.takeIf { it != "-" } ?: "측정 중",
                pm10Grade = convertGrade(item.pm10Grade),
                pm25Value = item.pm25Value?.takeIf { it != "-" } ?: "측정 중",
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
        else -> "측정 중"
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

    private fun retryWithEarlierTime(nx: Int, ny: Int, baseDate: String, maxRetries: Int = 3): WeatherResponse? {
        var currentRetry = 0
        var currentDate = LocalDate.parse(baseDate, DateTimeFormatter.ofPattern("yyyyMMdd"))
        var retryTime = calculateRetryTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")).hour, LocalDateTime.now(ZoneId.of("Asia/Seoul")).minute)

        while (currentRetry < maxRetries) {
            logger.info("재시도 시도: $currentRetry/$maxRetries, baseDate=$currentDate, baseTime=$retryTime, nx=$nx, ny=$ny")
            val params = WeatherRequestParams(currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")), retryTime, nx, ny)
            val weatherClient = WeatherApiClient()

            when (val result = ApiClientUtility.makeApiRequest(weatherClient, params) { response ->
                Either.Right(response)
            }) {
                is ApiResult.Success -> {
                    if (result.data.response?.header?.resultCode == "00") { // response -> result.data로 변경
                        logger.info("재시도 성공: 데이터 획득, 항목 수=${result.data.response?.body?.items?.size ?: 0}") // response -> result.data로 변경
                        return result.data
                    }
                    logger.warn("재시도 응답: ${result.data.response?.header?.resultCode} - ${result.data.response?.header?.resultMsg}")
                }
                is ApiResult.Error -> {
                    logger.error("재시도 실패: ${result.message}")
                }
            }

            currentRetry++
            // 1시간 전으로 이동, 00:30 이전이면 전날로 이동
            val now = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(currentRetry.toLong())
            retryTime = calculateRetryTime(now.hour, now.minute)
            if (retryTime == "2330" && currentRetry < maxRetries) {
                currentDate = currentDate.minusDays(1)
                retryTime = "2330"
            }
        }
        logger.error("최대 재시도 횟수 초과: 데이터 획득 실패")
        return null
    }

    private fun calculateRetryTime(hour: Int, minute: Int): String {
        val baseHour = if (minute < 40) hour - 1 else hour
        val adjustedHour = if (baseHour < 0) 23 else baseHour % 24
        return String.format("%02d30", adjustedHour)
    }

    private fun createDefaultUVIndex(): UVIndex {
        val currentHour = LocalDateTime.now(ZoneId.of("Asia/Seoul")).hour
        val (uvValue, uvStatus) = when (currentHour) {
            in 6..8 -> "3" to "보통"
            in 9..15 -> "7" to "높음"
            in 16..18 -> "4" to "보통"
            else -> "0" to "낮음"
        }
        return UVIndex(
            title = "자외선 지수",
            icon = when (uvStatus) {
                "낮음" -> "uv-low"
                "보통" -> "uv-moderate"
                "높음" -> "uv-high"
                else -> "uv-low"
            },
            status = uvStatus,
            value = uvValue,
            measurement = "UV Index"
        )
    }

    fun buildWeatherEntity(nx: Int, ny: Int, sidoName: String): Weather {
        // 한남동(용산구)의 정확한 격자 좌표로 수정
        val adjustedNx = if (nx == 60) 60 else nx // 한남동 nx=60
        val adjustedNy = if (ny == 127) 126 else ny // 한남동 ny=126
        val response = getUltraShortWeather(adjustedNx, adjustedNy)
        val weatherInfoList = parseWeatherData(response)
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val apiTime = now.format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val realTimeDust = getRealTimeDust(sidoName)
        val senTaIndexData = senTaIndexService.getSenTaIndex("1100000000", apiTime)
        val airStagnationIndexData = airStagnationIndexService.getAirStagnationIndex("1100000000", apiTime)

        logger.info("buildWeatherEntity - weatherInfoList: $weatherInfoList")
        logger.info("buildWeatherEntity - realTimeDust: $realTimeDust")
        logger.info("buildWeatherEntity - senTaIndexData: $senTaIndexData")
        logger.info("buildWeatherEntity - airStagnationIndexData: $airStagnationIndexData")

        val (date, time) = DateTimeUtils.getCurrentDateTimeFormatted()
        val currentHour = DateTimeUtils.getCurrentHour()

        val temperature = weatherInfoList.find { it.category == "TMP" }?.value?.let { "$it°C" } ?: "0°C"
        val humidity = weatherInfoList.find { it.category == "REH" }?.value?.let { "$it%" } ?: "50%"
        val windSpeed = weatherInfoList.find { it.category == "WSD" }?.value?.let { "${it}m/s" } ?: "0 m/s"
        val skyCondition = weatherInfoList.find { it.category == "SKY" }?.value?.toIntOrNull() ?: 1
        val precipitationType = weatherInfoList.find { it.category == "PTY" }?.value?.toIntOrNull() ?: 0

        val minTemp = weatherInfoList.find { it.category == "TMN" }?.value?.let { "$it°C" } ?: (temperature.dropLast(2).toIntOrNull()?.minus(3)?.let { "$it°C" } ?: "-3°C")
        val maxTemp = weatherInfoList.find { it.category == "TMX" }?.value?.let { "$it°C" } ?: (temperature.dropLast(2).toIntOrNull()?.plus(3)?.let { "$it°C" } ?: "3°C")
        val highLowTemperature = "$minTemp / $maxTemp"

        val weatherCondition = when (precipitationType) {
            0 -> when (skyCondition) {
                1 -> "맑음"
                3 -> "구름 많음"
                4 -> "흐림"
                else -> "맑음"
            }
            1 -> "비"
            2 -> "비/눈"
            3 -> "눈"
            else -> "맑음"
        }

        val airQuality = realTimeDust.firstOrNull()?.let {
            AirQuality(
                title = "미세먼지",
                icon = "yellow-smiley",
                status = it.pm10Grade,
                value = it.pm10Value,
                measurement = "㎍/㎥",
                title2 = "초미세먼지",
                status2 = it.pm25Grade,
                value2 = it.pm25Value,
                measurement2 = "㎍/㎥"
            )
        } ?: createDefaultAirQuality()

        val uvIndex = createDefaultUVIndex()

        val hourlyForecast = mutableListOf<HourlyForecast>()
        val currentHourIndex = currentHour / 3 * 3
        val hours = listOf(0, 3, 6, 9, 12, 15, 18, 21)

        hours.forEachIndexed { index, hourOffset ->
            val forecastHour = (currentHourIndex + hourOffset) % 24
            val forecastTime = if (hourOffset == 0) "지금" else "${forecastHour}시"
            val forecastSky = weatherInfoList.find { it.category == "SKY" && it.baseTime == String.format("%02d00", forecastHour) }?.value?.toIntOrNull() ?: skyCondition
            val forecastPrecip = weatherInfoList.find { it.category == "PTY" && it.baseTime == String.format("%02d00", forecastHour) }?.value?.toIntOrNull() ?: precipitationType
            val forecastTemp = weatherInfoList.find { it.category == "TMP" && it.baseTime == String.format("%02d00", forecastHour) }?.value?.let { "$it°C" } ?: temperature
            val forecastHumidity = weatherInfoList.find { it.category == "REH" && it.baseTime == String.format("%02d00", forecastHour) }?.value?.let { "$it%" } ?: humidity
            val forecastIcon = when (forecastPrecip) {
                0 -> when (forecastSky) {
                    1 -> "sun"
                    3 -> "partly-cloudy"
                    4 -> "cloudy"
                    else -> "sun"
                }
                1 -> "rain"
                2 -> "sleet"
                3 -> "snow"
                else -> "sun"
            }
            hourlyForecast.add(HourlyForecast(forecastTime, forecastIcon, forecastTemp, forecastHumidity))
        }

        return Weather(
            date = date,
            time = time,
            location = "한남동 (용산구)",
            currentTemperature = temperature,
            highLowTemperature = highLowTemperature,
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
            status = "측정 중",
            value = "측정 중",
            measurement = "㎍/㎥",
            title2 = "초미세먼지",
            status2 = "측정 중",
            value2 = "측정 중",
            measurement2 = "㎍/㎥"
        )

    private fun getUnitForCategory(category: String?): String =
        when (category) {
            "TMP", "TMN", "TMX" -> "°C"
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
            logger.error("serviceKey: $serviceKey")
            throw IllegalStateException("필수 설정값이 누락되었습니다: serviceKey, weatherBaseUrl, dustForecastBaseUrl, realTimeDustBaseUrl 중 하나가 비어 있습니다.")
        }
        logger.info("설정값 확인 - serviceKey: $serviceKey")
    }

    // 기존 코드 유지, getPrecipitationData 추가
    fun getPrecipitationData(areaNo: String, time: String): List<PrecipitationInfo> {
        // API 호출 예시 (실제 API에 맞게 수정 필요)
        val baseDate = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val params = WeatherRequestParams(baseDate, "0500", 5, 127)
        val weatherClient = WeatherApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(weatherClient, params) { response ->
            Either.Right(response)
        }) {
            is ApiResult.Success -> {
                val items = result.data.response?.body?.items?.filter { it.category == "PCP" } ?: emptyList()
                val values = (0..23).associate { "h$it" to (items.getOrNull(it)?.fcstValue?.toFloatOrNull() ?: 0f) }
                listOf(PrecipitationInfo(baseDate, values))
            }
            is ApiResult.Error -> {
                logger.error("강수량 데이터 가져오기 실패: ${result.message}")
                val defaultValues = (0..23).associate { "h$it" to 0f }
                listOf(PrecipitationInfo(baseDate, defaultValues))
            }
        }
    }
}
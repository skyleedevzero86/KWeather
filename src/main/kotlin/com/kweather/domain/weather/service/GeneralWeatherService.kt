package com.kweather.domain.weather.service

import arrow.core.Either
import com.kweather.domain.forecast.dto.DustForecastRequestParams
import com.kweather.domain.forecast.dto.ForecastInfo
import com.kweather.domain.forecast.dto.ForecastItem
import com.kweather.domain.forecast.dto.ForecastResponse
import com.kweather.domain.realtime.dto.RealTimeDustInfo
import com.kweather.domain.realtime.dto.RealTimeDustItem
import com.kweather.domain.realtime.dto.RealTimeDustResponse
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
    @Value("\${api.service-key:}") private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(GeneralWeatherService::class.java)

    data class RealTimeDustRequestParams(
        val returnType: String = "json",
        val numOfRows: Int = 1000,
        val pageNo: Int = 1,
        val sidoName: String,
        val ver: String = "1.0"
    )

    private inner class WeatherApiClient : ApiClientUtility.ApiClient<WeatherRequestParams, WeatherResponse> {
        override fun buildUrl(params: WeatherRequestParams): String {
            val urls = "${weatherBaseUrl}?serviceKey=$serviceKey" +
                    "&numOfRows=290" +
                    "&pageNo=1" +
                    "&base_date=${params.baseDate}" +
                    "&base_time=${params.baseTime}" +
                    "&nx=${params.nx}" +
                    "&ny=${params.ny}" +
                    "&dataType=JSON"
            return urls
        }

        override fun parseResponse(response: String): Either<String, WeatherResponse> {
            return try {
                if (response.trim().startsWith("<")) {
                    val dbFactory = DocumentBuilderFactory.newInstance()
                    val dBuilder = dbFactory.newDocumentBuilder()
                    val doc = dBuilder.parse(response.byteInputStream())
                    doc.documentElement.normalize()
                    val errMsg = doc.getElementsByTagName("errMsg").item(0)?.textContent ?: "알 수 없는 오류"
                    val returnAuthMsg = doc.getElementsByTagName("returnAuthMsg").item(0)?.textContent
                        ?: "알 수 없는 인증 오류"
                    throw IllegalStateException("API 오류: $errMsg - $returnAuthMsg")
                } else if (response.trim().startsWith("{")) {
                    val weatherResponse =
                        ApiClientUtility.getObjectMapper().readValue(response, WeatherResponse::class.java)
                    if (weatherResponse.response?.header?.resultCode != "00") {
                        throw IllegalStateException("API 오류: ${weatherResponse.response?.header?.resultCode} - ${weatherResponse.response?.header?.resultMsg}")
                    }
                    Either.Right(weatherResponse)
                } else {
                    throw IllegalStateException("알 수 없는 응답 형식")
                }
            } catch (e: Exception) {
                Either.Left("날씨 응답 파싱 실패: ${e.message}")
            }
        }
    }

    private inner class DustForecastApiClient :
        ApiClientUtility.ApiClient<DustForecastRequestParams, ForecastResponse> {
        override fun buildUrl(params: DustForecastRequestParams): String {
            val urls = "${dustForecastBaseUrl}?serviceKey=$serviceKey" +
                    "&returnType=json" +
                    "&numOfRows=1000" +
                    "&pageNo=1" +
                    "&searchDate=${params.searchDate}" +
                    "&dataTerm=DAILY"
            return urls
        }

        override fun parseResponse(response: String): Either<String, ForecastResponse> =
            runCatching {
                if (response.trim().startsWith("<")) {
                    val dbFactory = DocumentBuilderFactory.newInstance()
                    val dBuilder = dbFactory.newDocumentBuilder()
                    val doc = dBuilder.parse(response.byteInputStream())
                    doc.documentElement.normalize()
                    val errMsg = doc.getElementsByTagName("errMsg").item(0)?.textContent ?: "알 수 없는 오류"
                    val returnAuthMsg = doc.getElementsByTagName("returnAuthMsg").item(0)?.textContent
                        ?: "알 수 없는 인증 오류"
                    throw IllegalStateException("API 오류: $errMsg - $returnAuthMsg")
                }
                val forecastResponse =
                    ApiClientUtility.getObjectMapper().readValue(response, ForecastResponse::class.java)
                if (forecastResponse.response?.header?.resultCode != "00") {
                    throw IllegalStateException("API 오류: ${forecastResponse.response?.header?.resultCode} - ${forecastResponse.response?.header?.resultMsg}")
                }
                forecastResponse
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("미세먼지 예보 응답 파싱 실패: ${it.message}") }
            )
    }

    private inner class RealTimeDustApiClient :
        ApiClientUtility.ApiClient<RealTimeDustRequestParams, RealTimeDustResponse> {
        override fun buildUrl(params: RealTimeDustRequestParams): String {
            val encodedSidoName = URLEncoder.encode(params.sidoName, "UTF-8")
            val urls = "${realTimeDustBaseUrl}?serviceKey=$serviceKey" +
                    "&returnType=${params.returnType}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&pageNo=${params.pageNo}" +
                    "&sidoName=$encodedSidoName" +
                    "&ver=${params.ver}"
            return urls
        }

        override fun parseResponse(response: String): Either<String, RealTimeDustResponse> {
            return try {
                if (response.trim().startsWith("<")) {
                    val dbFactory = DocumentBuilderFactory.newInstance()
                    val dBuilder = dbFactory.newDocumentBuilder()
                    val doc = dBuilder.parse(response.byteInputStream())
                    doc.documentElement.normalize()
                    val errMsg = doc.getElementsByTagName("errMsg").item(0)?.textContent ?: "알 수 없는 오류"
                    val returnAuthMsg = doc.getElementsByTagName("returnAuthMsg").item(0)?.textContent
                        ?: "알 수 없는 인증 오류"
                    throw IllegalStateException("API 오류: $errMsg - $returnAuthMsg")
                } else if (response.trim().startsWith("{")) {
                    val dustResponse =
                        ApiClientUtility.getObjectMapper().readValue(response, RealTimeDustResponse::class.java)
                    if (dustResponse.response?.header?.resultCode != "00") {
                        throw IllegalStateException("API 오류: ${dustResponse.response?.header?.resultCode} - ${dustResponse.response?.header?.resultMsg}")
                    }
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
        val (baseDate, baseTime) = DateTimeUtils.getBaseDateTimeForShortTerm()
        val params = WeatherRequestParams(baseDate, baseTime, nx, ny)
        val weatherClient = WeatherApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(weatherClient, params) { response ->
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                val retryResponse = retryWithEarlierTime(nx, ny, baseDate, maxRetries = 3)
                Either.Right(retryResponse ?: response)
            } else {
                Either.Right(response)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
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
        val targetDate =
            if (currentHour < 6) now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) else searchDate
        val params = DustForecastRequestParams(targetDate, informCode)
        val dustClient = DustForecastApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(dustClient, params) { response ->
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                val previousDate = LocalDate.parse(targetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val retryParams = DustForecastRequestParams(previousDate, informCode)
                when (val retryResult = ApiClientUtility.makeApiRequest(dustClient, retryParams) { retryResponse ->
                    val forecastInfos =
                        retryResponse.response?.body?.items?.mapNotNull { item -> item?.let { parseForecastItem(it, informCode) } }
                            ?: emptyList()
                    Either.Right(forecastInfos)
                }) {
                    is ApiResult.Success -> Either.Right(retryResult.data)
                    is ApiResult.Error -> Either.Right(emptyList())
                }
            } else {
                val forecastInfos =
                    response.response?.body?.items?.mapNotNull { item -> item?.let { parseForecastItem(it, informCode) } }
                        ?: emptyList()
                Either.Right(forecastInfos)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                emptyList()
            }
        }
    }

    fun getRealTimeDust(sidoName: String): List<RealTimeDustInfo> {
        val apiSidoName = convertSidoForApi(sidoName)
        val params = RealTimeDustRequestParams(sidoName = apiSidoName)
        val realTimeDustClient = RealTimeDustApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(realTimeDustClient, params) { response ->
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                Either.Right(emptyList())
            } else {
                val dustInfos =
                    response.response?.body?.items?.mapNotNull { item -> item?.let { parseRealTimeDustItem(it) } }
                        ?: emptyList()
                Either.Right(dustInfos)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
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

    private fun parseWeatherItem(item: WeatherItem): WeatherInfo? =
        runCatching {
            WeatherInfo(
                baseDate = item.baseDate ?: return null,
                baseTime = item.baseTime ?: return null,
                fcstTime = item.fcstTime ?: return null,
                category = item.category ?: return null,
                value = item.fcstValue ?: return null,
                unit = getUnitForCategory(item.category)
            )
        }.onFailure { e ->
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
        }.getOrNull()

    private fun retryWithEarlierTime(nx: Int, ny: Int, baseDate: String, maxRetries: Int = 3): WeatherResponse? {
        var currentRetry = 0
        var currentDate = LocalDate.parse(baseDate, DateTimeFormatter.ofPattern("yyyyMMdd"))
        var retryTimeIndex = DateTimeUtils.getForecastTimes().indexOf(DateTimeUtils.getBaseDateTimeForShortTerm().second)

        while (currentRetry < maxRetries) {
            retryTimeIndex = if (retryTimeIndex > 0) retryTimeIndex - 1 else DateTimeUtils.getForecastTimes().size - 1
            val retryTime = DateTimeUtils.getForecastTimes()[retryTimeIndex]
            val params = WeatherRequestParams(
                currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                retryTime,
                nx,
                ny
            )
            val weatherClient = WeatherApiClient()

            when (val result = ApiClientUtility.makeApiRequest(weatherClient, params) { response ->
                Either.Right(response)
            }) {
                is ApiResult.Success -> {
                    if (result.data.response?.header?.resultCode == "00") {
                        return result.data
                    }
                }
                is ApiResult.Error -> {}
            }

            currentRetry++
            if (retryTimeIndex == 0) {
                currentDate = currentDate.minusDays(1)
                retryTimeIndex = DateTimeUtils.getForecastTimes().size
            }
        }
        return null
    }

    fun buildWeatherEntity(nx: Int, ny: Int, sidoName: String): Weather {
        val adjustedNx = if (nx == 60) 60 else nx
        val adjustedNy = if (ny == 127) 126 else ny
        val response = getUltraShortWeather(adjustedNx, adjustedNy)
        val weatherInfoList = parseWeatherData(response)
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val realTimeDust = getRealTimeDust(sidoName)

        val (date, time) = DateTimeUtils.getCurrentDateTimeFormatted()
        val currentHour = DateTimeUtils.getCurrentHour()

        val forecastTimes =
            weatherInfoList.mapNotNull { it.fcstTime?.substring(0, 2)?.toIntOrNull() }.distinct().sorted()
        val closestHour = forecastTimes.minByOrNull { Math.abs(it - currentHour) } ?: currentHour

        val closestFcstTime = String.format("%02d00", closestHour)
        val temperature = weatherInfoList.find { it.category == "TMP" && it.fcstTime == closestFcstTime }?.value?.let { "$it°C" }
            ?: weatherInfoList.find { it.category == "TMP" }?.value?.let { "$it°C" } ?: "0°C"
        val humidity = weatherInfoList.find { it.category == "REH" && it.fcstTime == closestFcstTime }?.value?.let { "$it%" }
            ?: weatherInfoList.find { it.category == "REH" }?.value?.let { "$it%" } ?: "50%"
        val windSpeed = weatherInfoList.find { it.category == "WSD" && it.fcstTime == closestFcstTime }?.value?.let { "${it}m/s" }
            ?: weatherInfoList.find { it.category == "WSD" }?.value?.let { "${it}m/s" } ?: "0 m/s"
        val skyCondition =
            weatherInfoList.find { it.category == "SKY" && it.fcstTime == closestFcstTime }?.value?.toIntOrNull()
                ?: weatherInfoList.find { it.category == "SKY" }?.value?.toIntOrNull() ?: 1
        val precipitationType =
            weatherInfoList.find { it.category == "PTY" && it.fcstTime == closestFcstTime }?.value?.toIntOrNull()
                ?: weatherInfoList.find { it.category == "PTY" }?.value?.toIntOrNull() ?: 0

        val minTemp = weatherInfoList.find { it.category == "TMN" }?.value?.let { "$it°C" } ?: "-3°C"
        val maxTemp = weatherInfoList.find { it.category == "TMX" }?.value?.let { "$it°C" } ?: "3°C"
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

        val hourlyForecast = mutableListOf<HourlyForecast>()

        if (forecastTimes.isEmpty()) {
            val defaultTimes = listOf(currentHour, (currentHour + 1) % 24, (currentHour + 2) % 24, (currentHour + 3) % 24)
            defaultTimes.forEach { hour ->
                val fcstTime = String.format("%02d00", hour)
                val forecastTime = if (hour == currentHour) "지금" else "${hour}시"
                val forecastIcon = when (precipitationType) {
                    0 -> when (skyCondition) {
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
                hourlyForecast.add(HourlyForecast(forecastTime, forecastIcon, temperature, humidity))
            }
        } else {
            forecastTimes.forEach { hour ->
                val fcstTime = String.format("%02d00", hour)
                val forecastTime = if (hour == currentHour) "지금" else "${hour}시"
                val forecastSky =
                    weatherInfoList.find { it.category == "SKY" && it.fcstTime == fcstTime }?.value?.toIntOrNull()
                        ?: skyCondition
                val forecastPrecip =
                    weatherInfoList.find { it.category == "PTY" && it.fcstTime == fcstTime }?.value?.toIntOrNull()
                        ?: precipitationType
                val forecastTemp =
                    weatherInfoList.find { it.category == "TMP" && it.fcstTime == fcstTime }?.value?.let { "$it°C" }
                        ?: temperature
                val forecastHumidity =
                    weatherInfoList.find { it.category == "REH" && it.fcstTime == fcstTime }?.value?.let { "$it%" }
                        ?: humidity

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
        }

        val uvIndex = createDefaultUVIndex()

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

    fun getHourlyTemperature(areaNo: String, time: String): Map<String, Any> {
        val (baseDate, baseTime) = DateTimeUtils.getBaseDateTimeForShortTerm()
        val params = WeatherRequestParams(baseDate, baseTime, 60, 127)
        val weatherClient = WeatherApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(weatherClient, params) { response ->
            Either.Right(response)
        }) {
            is ApiResult.Success -> {
                val items = result.data.response?.body?.items?.filter { it.category == "TMP" } ?: emptyList()
                val temperatures = mutableMapOf<String, String>()
                items.forEachIndexed { index, item ->
                    val hour = (index + 1).toString()
                    temperatures["h$hour"] = item.fcstValue ?: "0"
                }

                for (i in 1..72) {
                    if (!temperatures.containsKey("h$i")) {
                        temperatures["h$i"] = "15"
                    }
                }

                mapOf(
                    "code" to "A41",
                    "areaNo" to areaNo,
                    "date" to baseDate + baseTime,
                    *temperatures.entries.map { it.key to it.value }.toTypedArray()
                )
            }
            is ApiResult.Error -> {
                val defaultTemps = (1..72).associate { "h$it" to "15" }
                mapOf(
                    "code" to "A41",
                    "areaNo" to areaNo,
                    "date" to baseDate + baseTime,
                    *defaultTemps.entries.map { it.key to it.value }.toTypedArray()
                )
            }
        }
    }

    private fun createDefaultUVIndex(): UVIndex {
        return UVIndex(
            title = "자외선 지수",
            icon = "uv-moderate",
            status = "보통",
            value = "3",
            measurement = "UV Index"
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
            throw IllegalStateException("필수 설정값이 누락되었습니다.")
        }
    }

    fun getPrecipitationData(areaNo: String, time: String): List<PrecipitationInfo> {
        val (baseDate, baseTime) = DateTimeUtils.getBaseDateTimeForShortTerm()
        val params = WeatherRequestParams(baseDate, baseTime, 5, 127)
        val weatherClient = WeatherApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(weatherClient, params) { response ->
            Either.Right(response)
        }) {
            is ApiResult.Success -> {
                val items = result.data.response?.body?.items?.filter { it.category == "PCP" } ?: emptyList()
                val values = (0..23).associate { "h$it" to 0f }.toMutableMap()

                items.forEach { item ->
                    val fcstTime = item.fcstTime?.substring(0, 2)?.toIntOrNull() ?: return@forEach
                    val precipValue = when (val rawValue = item.fcstValue) {
                        "강수없음" -> 0f
                        else -> rawValue?.toFloatOrNull() ?: 0f
                    }
                    values["h$fcstTime"] = precipValue
                }

                values["h12"] = 2.5f
                values["h13"] = 1.8f
                values["h14"] = 0.5f

                listOf(PrecipitationInfo(baseDate, values))
            }
            is ApiResult.Error -> {
                val defaultValues = (0..23).associate { "h$it" to 0f }
                listOf(PrecipitationInfo(baseDate, defaultValues))
            }
        }
    }
}
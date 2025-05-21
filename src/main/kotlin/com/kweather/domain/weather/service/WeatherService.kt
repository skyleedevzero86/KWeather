package com.kweather.domain.weather.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class WeatherService(
    private val objectMapper: ObjectMapper,
    @Value("\${api.weather.base-url:}")
    private val weatherBaseUrl: String,
    @Value("\${api.arpltninforinqiresvc.base-url:}") // 키 수정
    private val dustForecastBaseUrl: String,
    @Value("\${api.service-key:}")
    private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(WeatherService::class.java)

    init {
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

    fun getUltraShortWeather(nx: Int, ny: Int): WeatherResponse {
        val baseDate = DateTimeUtils.getBaseDate()
        val baseTime = DateTimeUtils.getBaseTime()

        logger.info("Using baseDate: $baseDate, baseTime: $baseTime")

        val urlString = buildWeatherApiUrl(baseDate, baseTime, nx, ny)
        logger.info("Making API request to: {} (redacted service key)", urlString.replace(serviceKey, "[REDACTED]"))

        return try {
            val response = fetchDataFromApi(urlString)
            logger.info("Raw API Response: {}", response.take(200))

            if (response.trim().startsWith("<")) {
                if (response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") || response.contains("SERVICE ERROR")) {
                    logger.error("API key error: {}", response)
                    throw IllegalStateException("API service key error: Check if the key is valid and properly formatted")
                }
                logger.error("Received XML error response: {}", response.take(500))
                throw IllegalStateException("API returned an error response")
            }

            val weatherResponse = objectMapper.readValue<WeatherResponse>(response)
            logger.info("Parsed Weather Response: $weatherResponse")

            if (weatherResponse.response?.header?.resultCode == "03" && weatherResponse.response.header.resultMsg == "NO_DATA") {
                logger.warn("No weather data available for $baseDate $baseTime, retrying with earlier time")
                return retryWithEarlierTime(nx, ny, baseDate) ?: weatherResponse
            }

            weatherResponse
        } catch (e: Exception) {
            logger.error("Failed to fetch weather data", e)
            WeatherResponse(WeatherResponseData(header = Header("ERROR", "Failed to fetch weather data: ${e.message}"), body = null))
        }
    }

    fun getDustForecast(searchDate: String, informCode: String): List<ForecastInfo> {
        val urlString = buildDustForecastApiUrl(searchDate, informCode)
        logger.info("Making Dust Forecast API request to: {} (redacted service key)", urlString.replace(serviceKey, "[REDACTED]"))

        return try {
            val response = fetchDataFromApi(urlString)
            logger.info("Raw Dust Forecast API Response: {}", response.take(200))

            if (response.trim().startsWith("<")) {
                logger.error("Received XML error response: {}", response.take(500))
                // XML 파싱 시도 (간단한 예외 처리용)
                if (response.contains("SERVICE ERROR") && response.contains("returnReasonCode>01")) {
                    throw IllegalStateException("API returned service error (Code: 01) - Check request parameters or service key")
                }
                throw IllegalStateException("Dust Forecast API returned an unexpected XML response")
            }

            val dustResponse = objectMapper.readValue<ForecastResponse>(response)
            logger.info("Parsed Dust Forecast Response: {}", dustResponse)

            dustResponse.response?.body?.items?.mapNotNull { item: ForecastItem ->
                try {
                    ForecastInfo(
                        date = item.dataTime ?: throw IllegalArgumentException("dataTime is missing"),
                        type = item.informCode ?: informCode, // API 응답에 없으면 요청값 사용
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
                } catch (e: IllegalArgumentException) {
                    logger.warn("Skipping invalid item due to: ${e.message}")
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to fetch dust forecast data", e)
            throw IllegalStateException("Failed to fetch dust forecast data: ${e.message}")
        }
    }

    private fun retryWithEarlierTime(nx: Int, ny: Int, baseDate: String): WeatherResponse? {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val currentHour = now.hour
        val currentMinute = now.minute
        val retryTime = if (currentHour >= 12 && currentMinute >= 5) {
            "0000"
        } else {
            when {
                currentMinute < 30 -> String.format("%02d00", if (currentHour == 0) 23 else currentHour - 1)
                else -> String.format("%02d30", if (currentMinute < 45) currentHour else if (currentHour == 0) 23 else currentHour - 1)
            }
        }

        logger.info("Retrying with baseTime: $retryTime")

        val urlString = buildWeatherApiUrl(baseDate, retryTime, nx, ny)
        logger.info("Retry making API request to: {} (redacted service key)", urlString.replace(serviceKey, "[REDACTED]"))

        try {
            val response = fetchDataFromApi(urlString)
            logger.info("Retry Raw API Response: {}", response.take(200))

            if (response.trim().startsWith("<")) {
                return null
            }

            return objectMapper.readValue<WeatherResponse>(response)
        } catch (e: Exception) {
            logger.error("Retry failed: ${e.message}")
            return null
        }
    }

    private fun fetchDataFromApi(urlString: String): String {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            logger.info("Response code: $responseCode")

            reader = if (responseCode >= 200 && responseCode < 300) {
                BufferedReader(InputStreamReader(connection.inputStream))
            } else {
                BufferedReader(InputStreamReader(connection.errorStream))
            }

            val response = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }

            return response.toString()
        } finally {
            reader?.close()
            connection?.disconnect()
        }
    }

    private fun buildWeatherApiUrl(baseDate: String, baseTime: String, nx: Int, ny: Int): String {
        return "${weatherBaseUrl}?serviceKey=${serviceKey}" +
                "&numOfRows=10" +
                "&pageNo=1" +
                "&base_date=${baseDate}" +
                "&base_time=${baseTime}" +
                "&nx=${nx}" +
                "&ny=${ny}" +
                "&dataType=JSON"
    }

    private fun buildDustForecastApiUrl(searchDate: String, informCode: String): String {
        return "${dustForecastBaseUrl}?serviceKey=${serviceKey}" +
                "&returnType=json" +
                "&numOfRows=100" +
                "&pageNo=1" +
                "&searchDate=${searchDate}" +
                "&dataTerm=DAILY" +
                "&informCode=${informCode}" // informCode 추가
    }

    fun parseWeatherData(response: WeatherResponse): List<WeatherInfo> {
        if (response.response?.header?.resultCode != "00" && response.response?.header?.resultCode != null) {
            return listOf(
                WeatherInfo(
                    baseDate = "",
                    baseTime = "",
                    category = "ERROR",
                    value = "API Error: ${response.response.header.resultCode} - ${response.response.header.resultMsg}",
                    unit = ""
                )
            )
        }

        val items = response.response?.body?.items?.item
        if (items == null) {
            logger.warn("Weather response does not contain items. Response: $response")
            return emptyList()
        }

        return items.mapNotNull { item ->
            item?.let {
                try {
                    WeatherInfo(
                        baseDate = it.baseDate ?: throw IllegalArgumentException("baseDate is missing"),
                        baseTime = it.baseTime ?: throw IllegalArgumentException("baseTime is missing"),
                        category = it.category ?: throw IllegalArgumentException("category is missing"),
                        value = it.obsrValue ?: throw IllegalArgumentException("obsrValue is missing"),
                        unit = getUnitForCategory(it.category)
                    )
                } catch (e: IllegalArgumentException) {
                    logger.warn("Skipping invalid item due to: ${e.message}")
                    null
                }
            }
        }
    }

    fun buildWeatherEntity(nx: Int, ny: Int): Weather {
        val response = getUltraShortWeather(nx, ny)
        val weatherInfoList = parseWeatherData(response)

        val currentDateTime = DateTimeUtils.getCurrentDateTimeFormatted()
        val hour = LocalDateTime.now(ZoneId.of("Asia/Seoul")).hour

        val airQuality = AirQuality(
            title = "미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "20",
            measurement = "㎍/㎥"
        )
        val uvIndex = UVIndex(
            title = "초미세먼지",
            icon = "yellow-smiley",
            status = "좋음",
            value = "8",
            measurement = "㎍/㎥"
        )
        val hourlyForecast = listOf(
            HourlyForecast("지금", "moon", "-1.8°C", "34%"),
            HourlyForecast("0시", "moon", "-6°C", "55%"),
            HourlyForecast("3시", "moon", "-6°C", "60%"),
            HourlyForecast("6시", "moon", "-7°C", "67%"),
            HourlyForecast("9시", "sun", "-6°C", "55%")
        )

        val temperature = weatherInfoList.find { it.category == "T1H" }?.value ?: "-1.8°C"
        val weatherCondition = "맑음"
        val windSpeed = "1km/초(남서) m/s 0"

        return Weather(
            date = currentDateTime.first,
            time = currentDateTime.second,
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

    private fun getUnitForCategory(category: String?): String {
        return when (category) {
            "T1H" -> "°C"
            "RN1" -> "mm"
            "UUU" -> "m/s"
            "VVV" -> "m/s"
            "REH" -> "%"
            "PTY" -> ""
            "VEC" -> "deg"
            "WSD" -> "m/s"
            else -> ""
        }
    }
}
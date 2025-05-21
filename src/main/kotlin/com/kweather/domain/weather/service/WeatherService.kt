package com.kweather.domain.weather.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kweather.domain.weather.dto.*
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
    private val baseUrl: String,
    @Value("\${api.service-key:}")
    private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(WeatherService::class.java)

    init {
        require(serviceKey.isNotBlank()) { "Service key must not be blank" }
        require(baseUrl.isNotBlank()) { "Base URL must not be blank" }
    }

    fun getUltraShortWeather(nx: Int, ny: Int): WeatherResponse {
        val baseDate = DateTimeUtils.getBaseDate()
        val baseTime = DateTimeUtils.getBaseTime()

        logger.info("Using baseDate: $baseDate, baseTime: $baseTime")

        val urlString = buildApiUrl(baseDate, baseTime, nx, ny)
        logger.info("Making API request to: {} (redacted service key)", urlString.replace(serviceKey, "[REDACTED]"))

        return try {
            val response = fetchDataFromApi(urlString)
            logger.info("Raw API Response: {}", response.take(200))

            // XML 오류 응답 여부 확인
            if (response.trim().startsWith("<")) {
                if (response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") || response.contains("SERVICE ERROR")) {
                    logger.error("API key error: {}", response)
                    throw IllegalStateException("API service key error: Check if the key is valid and properly formatted")
                }
                logger.error("Received XML error response: {}", response.take(500))
                throw IllegalStateException("API returned an error response")
            }

            // JSON으로 파싱
            val weatherResponse = objectMapper.readValue<WeatherResponse>(response)
            logger.info("Parsed Weather Response: $weatherResponse")

            // NO_DATA 경우 처리 및 재시도
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

    private fun retryWithEarlierTime(nx: Int, ny: Int, baseDate: String): WeatherResponse? {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val currentHour = now.hour
        val currentMinute = now.minute
        val retryTime = if (currentHour >= 12 && currentMinute >= 5) {
            "0000" // 12:05 이후에는 0000으로 고정
        } else {
            when {
                currentMinute < 30 -> String.format("%02d00", if (currentHour == 0) 23 else currentHour - 1)
                else -> String.format("%02d30", if (currentMinute < 45) currentHour else if (currentHour == 0) 23 else currentHour - 1)
            }
        }

        logger.info("Retrying with baseTime: $retryTime")

        val urlString = buildApiUrl(baseDate, retryTime, nx, ny)
        logger.info("Retry making API request to: {} (redacted service key)", urlString.replace(serviceKey, "[REDACTED]"))

        try {
            val response = fetchDataFromApi(urlString)
            logger.info("Retry Raw API Response: {}", response.take(200))

            if (response.trim().startsWith("<")) {
                return null // XML 에러 응답이면 null 반환
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

    private fun buildApiUrl(baseDate: String, baseTime: String, nx: Int, ny: Int): String {
        return "${baseUrl}?serviceKey=${serviceKey}" +
                "&numOfRows=10" +
                "&pageNo=1" +
                "&base_date=${baseDate}" +
                "&base_time=${baseTime}" +
                "&nx=${nx}" +
                "&ny=${ny}" +
                "&dataType=JSON"
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

    private fun getUnitForCategory(category: String?): String {
        return when (category) {
            "T1H" -> "°C"    // 기온
            "RN1" -> "mm"    // 1시간 강수량
            "UUU" -> "m/s"   // 동서바람성분
            "VVV" -> "m/s"   // 남북바람성분
            "REH" -> "%"     // 습도
            "PTY" -> ""      // 강수형태
            "VEC" -> "deg"   // 풍향
            "WSD" -> "m/s"   // 풍속
            else -> ""
        }
    }

    private fun String.substringBetween(start: String, end: String): String? {
        val startIndex = this.indexOf(start)
        if (startIndex < 0) return null

        val endIndex = this.indexOf(end, startIndex + start.length)
        if (endIndex < 0) return null

        return this.substring(startIndex + start.length, endIndex)
    }
}
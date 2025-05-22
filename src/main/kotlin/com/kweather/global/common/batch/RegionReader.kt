package com.kweather.global.common.batch

import com.fasterxml.jackson.databind.ObjectMapper
import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.dto.StanReginCdResponse
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.support.ListItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

@Component
class RegionReader(
    @Value("\${api.service-key}") private val serviceKey: String,
    @Value("\${api.region.base-url}") private val baseUrl: String,
    private val objectMapper: ObjectMapper,
    private val regionCacheLoader: RegionCacheLoader
) {
    private val logger = LoggerFactory.getLogger(RegionReader::class.java)

    init {
        require(serviceKey.isNotBlank()) { "Service key must not be blank" }
        require(baseUrl.isNotBlank()) { "Base URL must not be blank" }
    }

    fun reader(pageNo: Int = 1, numOfRows: Int = 50): ItemReader<RegionDto> { // numOfRows를 50으로 줄임
        val allRegions = mutableListOf<RegionDto>()

        try {
            fetchRegionsFromApi(allRegions, pageNo, numOfRows)

            if (allRegions.isEmpty()) {
                logger.warn("No data fetched from API, attempting to load from cache")
                allRegions.addAll(regionCacheLoader.loadRegionsFromCache())
            }

            logger.info("Total loaded {} region data items", allRegions.size)
            return ListItemReader(allRegions)
        } catch (e: Exception) {
            logger.error("Error loading region data from API, attempting to load from cache", e)
            val cachedRegions = regionCacheLoader.loadRegionsFromCache()
            logger.info("Loaded {} region data items from cache", cachedRegions.size)
            return ListItemReader(cachedRegions)
        }
    }

    @Retryable(
        value = [SocketTimeoutException::class, IOException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, multiplier = 1.5)
    )
    private fun fetchRegionsFromApi(allRegions: MutableList<RegionDto>, pageNo: Int, numOfRows: Int) {
        var currentPage = pageNo

        while (true) {
            val urlString = buildApiUrl(currentPage, numOfRows)
            logger.info("Making API request to: {} (page {})", urlString.replace(serviceKey, "[REDACTED]"), currentPage)

            val response = fetchDataFromApi(urlString)
            logger.info("Response first 200 chars: {}", response.take(200))

            // 응답이 XML 형식인지 확인 (에러 응답인 경우)
            if (response.trim().startsWith("<")) {
                if (response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") ||
                    response.contains("SERVICE ERROR")) {
                    logger.error("API key error: {}", response)
                    throw IllegalStateException("API service key error: Check if the key is valid and properly formatted")
                }
                logger.error("Received XML error response: {}", response.take(500))
                throw IllegalStateException("API returned an error response")
            }

            // JSON 응답 처리
            val stanReginCdResponse = objectMapper.readValue(response, StanReginCdResponse::class.java)
            val regions = stanReginCdResponse.StanReginCd?.flatMap { it.row.orEmpty() } ?: emptyList()

            if (regions.isEmpty()) {
                logger.info("No more data to fetch at page {}", currentPage)
                break
            }

            allRegions.addAll(regions)
            logger.info("Loaded {} region data items from page {}", regions.size, currentPage)
            currentPage++
        }
    }

    @Retryable(
        value = [SocketTimeoutException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, multiplier = 1.5)
    )
    private fun fetchDataFromApi(urlString: String): String {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000 // 타임아웃을 15초로 증가
            connection.readTimeout = 15000    // 타임아웃을 15초로 증가

            val responseCode = connection.responseCode
            logger.info("Response code: {}", responseCode)

            reader = if (responseCode >= 200 && responseCode < 300) {
                BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
            } else {
                BufferedReader(InputStreamReader(connection.errorStream, "UTF-8"))
            }

            val response = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }

            return response.toString()
        } catch (e: Exception) {
            logger.error("Failed to fetch data from API: {}", e.message)
            throw e
        } finally {
            reader?.close()
            connection?.disconnect()
        }
    }

    private fun buildApiUrl(pageNo: Int, numOfRows: Int): String {
        return "${baseUrl}?ServiceKey=${serviceKey}" +
                "&pageNo=${pageNo}" +
                "&numOfRows=${numOfRows}" +
                "&type=json" +
                "&flag=Y"
    }
}
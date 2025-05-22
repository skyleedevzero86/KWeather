package com.kweather.global.common.batch

import com.fasterxml.jackson.databind.ObjectMapper
import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.dto.StanReginCdResponse
import com.kweather.domain.region.service.HierarchyService
import com.kweather.global.common.constants.BatchConstants
import com.kweather.global.common.exception.ApiServiceException
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
    private val regionCacheLoader: RegionCacheLoader,
    private val hierarchyService: HierarchyService
) {
    private val logger = LoggerFactory.getLogger(RegionReader::class.java)

    init {
        require(serviceKey.isNotBlank()) { BatchConstants.ErrorMessages.SERVICE_KEY_EMPTY }
        require(baseUrl.isNotBlank()) { BatchConstants.ErrorMessages.BASE_URL_EMPTY }
    }

    fun reader(pageNo: Int = 1, numOfRows: Int = BatchConstants.DEFAULT_PAGE_SIZE): ItemReader<RegionDto> {
        val allRegions = mutableListOf<RegionDto>()

        return try {
            fetchRegionsFromApi(allRegions, pageNo, numOfRows)

            if (allRegions.isEmpty()) {
                logger.warn(BatchConstants.LogMessages.API_REQUEST_FAILED)
                allRegions.addAll(regionCacheLoader.loadRegionsFromCache())
            }

            // 계층 구조 데이터 미리 로드
            logger.info(BatchConstants.LogMessages.HIERARCHY_DATA_PRELOAD)
            hierarchyService.loadHierarchyData(allRegions)
            logger.info(BatchConstants.LogMessages.HIERARCHY_DATA_PRELOAD_COMPLETE)

            logger.info(BatchConstants.LogMessages.TOTAL_DATA_LOADED, allRegions.size)
            ListItemReader(allRegions)
        } catch (e: Exception) {
            logger.error(BatchConstants.LogMessages.API_REQUEST_FAILED, e)
            handleApiFailure()
        }
    }

    private fun handleApiFailure(): ItemReader<RegionDto> {
        val cachedRegions = regionCacheLoader.loadRegionsFromCache()
        logger.info(BatchConstants.LogMessages.HIERARCHY_DATA_PRELOAD)
        hierarchyService.loadHierarchyData(cachedRegions)
        logger.info(BatchConstants.LogMessages.HIERARCHY_DATA_PRELOAD_COMPLETE)
        logger.info(BatchConstants.LogMessages.CACHE_LOAD_SUCCESS, cachedRegions.size)
        return ListItemReader(cachedRegions)
    }

    @Retryable(
        value = [SocketTimeoutException::class, IOException::class],
        maxAttempts = BatchConstants.MAX_RETRY_ATTEMPTS,
        backoff = Backoff(delay = BatchConstants.RETRY_DELAY_MS, multiplier = BatchConstants.RETRY_MULTIPLIER)
    )
    private fun fetchRegionsFromApi(allRegions: MutableList<RegionDto>, pageNo: Int, numOfRows: Int) {
        var currentPage = pageNo

        while (true) {
            val urlString = buildApiUrl(currentPage, numOfRows)
            logger.info(BatchConstants.LogMessages.API_REQUEST_START, currentPage)

            val response = fetchDataFromApi(urlString)
            validateApiResponse(response)

            val stanReginCdResponse = objectMapper.readValue(response, StanReginCdResponse::class.java)
            val regions = stanReginCdResponse.StanReginCd?.flatMap { it.row.orEmpty() } ?: emptyList()

            if (regions.isEmpty()) {
                logger.info(BatchConstants.LogMessages.NO_MORE_DATA, currentPage)
                break
            }

            allRegions.addAll(regions)
            logger.info(BatchConstants.LogMessages.API_RESPONSE_SUCCESS, currentPage, regions.size)
            currentPage++
        }
    }

    private fun validateApiResponse(response: String) {
        if (response.trim().startsWith("<")) {
            when {
                response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") ||
                        response.contains("SERVICE ERROR") -> {
                    logger.error(BatchConstants.LogMessages.API_KEY_ERROR, response)
                    throw ApiServiceException(BatchConstants.ErrorMessages.API_SERVICE_KEY_ERROR)
                }
                else -> {
                    logger.error(BatchConstants.LogMessages.XML_ERROR_RESPONSE, response.take(500))
                    throw ApiServiceException(BatchConstants.ErrorMessages.API_ERROR_RESPONSE)
                }
            }
        }
    }

    @Retryable(
        value = [SocketTimeoutException::class],
        maxAttempts = BatchConstants.MAX_RETRY_ATTEMPTS,
        backoff = Backoff(delay = BatchConstants.RETRY_DELAY_MS, multiplier = BatchConstants.RETRY_MULTIPLIER)
    )
    private fun fetchDataFromApi(urlString: String): String {
        var connection: HttpURLConnection? = null

        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = BatchConstants.API_TIMEOUT_MS
                readTimeout = BatchConstants.API_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "KWeather-BatchProcessor/1.0")
            }

            val responseCode = connection.responseCode
            logger.debug(BatchConstants.LogMessages.API_RESPONSE_CODE, responseCode)

            val inputStream = if (responseCode in BatchConstants.HttpStatus.OK_MIN..BatchConstants.HttpStatus.OK_MAX) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                reader.readText()
            }
        } catch (e: Exception) {
            logger.error(BatchConstants.LogMessages.API_DATA_FETCH_FAILED, e.message)
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildApiUrl(pageNo: Int, numOfRows: Int): String {
        return buildString {
            append(baseUrl)
            append("?ServiceKey=").append(serviceKey)
            append("&pageNo=").append(pageNo)
            append("&numOfRows=").append(numOfRows)
            append("&type=json")
            append("&flag=Y")
        }
    }
}
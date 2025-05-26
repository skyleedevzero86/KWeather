package com.kweather.global.common.batch

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.dto.StanReginCdResponse
import com.kweather.domain.region.service.HierarchyService
import com.kweather.domain.weather.model.ApiResult
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
        check(serviceKey.isNotBlank()) { BatchConstants.ErrorMessages.SERVICE_KEY_EMPTY }
        check(baseUrl.isNotBlank()) { BatchConstants.ErrorMessages.BASE_URL_EMPTY }
        logger.info("RegionReader 초기화 완료 - 서비스 키: [masked]")
    }

    data class RegionRequestParams(
        val pageNo: Int = 1,
        val numOfRows: Int = BatchConstants.DEFAULT_PAGE_SIZE,
        val type: String = "json",
        val flag: String = "Y"
    )

    private interface ApiClient<P, T> {
        fun buildUrl(params: P): String
        fun parseResponse(response: String): Either<String, T>
    }

    private inner class RegionApiClient : ApiClient<RegionRequestParams, StanReginCdResponse> {
        override fun buildUrl(params: RegionRequestParams): String =
            "$baseUrl?serviceKey=$serviceKey" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&type=${params.type}" +
                    "&flag=${params.flag}"

        override fun parseResponse(response: String): Either<String, StanReginCdResponse> =
            runCatching {
                objectMapper.readValue(response, StanReginCdResponse::class.java)
            }.fold(
                onSuccess = { it.right() },
                onFailure = { "지역 응답 파싱 실패: ${it.message}".left() }
            )
    }

    fun reader(maxPage: Int = BatchConstants.DEFAULT_MAX_PAGE, numOfRows: Int = BatchConstants.DEFAULT_PAGE_SIZE): ItemReader<RegionDto> {
        val allRegions = mutableListOf<RegionDto>()
        val regionClient = RegionApiClient()

        for (currentPage in 1..maxPage) {
            val params = RegionRequestParams(pageNo = currentPage, numOfRows = numOfRows)
            logger.info("페이지 {} API 요청 시작", currentPage)

            when (val result = fetchRegions(regionClient, params)) {
                is ApiResult.Success -> {
                    if (result.data.isEmpty()) {
                        logger.info("페이지 {}: 더 이상 데이터 없음", currentPage)
                        break
                    }
                    saveRegions(result.data)?.let { allRegions.addAll(it) }
                }
                is ApiResult.Error -> {
                    logger.error("페이지 {} 요청 실패: {}", currentPage, result.message)
                    continue
                }
            }
        }

        return if (allRegions.isEmpty()) {
            logger.warn("API 요청 실패, 캐시에서 데이터 로드")
            loadFromCache()
        } else {
            logger.info("총 {}개의 지역 데이터 로드 완료", allRegions.size)
            ListItemReader(allRegions)
        }
    }

    private fun fetchRegions(client: ApiClient<RegionRequestParams, StanReginCdResponse>, params: RegionRequestParams): ApiResult<List<RegionDto>> {
        return makeApiRequest(client, params) { response ->
            val regions = response.stanReginCd
                ?.find { it.row != null }
                ?.row
                ?.filter { it.isValid() }
                ?: emptyList()
            logger.info("페이지 {}: {}개의 지역 데이터 수신", params.pageNo, regions.size)
            regions.right()
        }
    }

    private fun saveRegions(regions: List<RegionDto>): List<RegionDto>? = try {
        logger.info("계층 데이터 저장 시작")
        hierarchyService.loadHierarchyData(regions)
        logger.info("계층 데이터 저장 완료")
        regions
    } catch (e: Exception) {
        logger.error("데이터 저장 실패: {}", e.message, e)
        null
    }

    private fun loadFromCache(): ItemReader<RegionDto> {
        val cachedRegions = regionCacheLoader.loadRegionsFromCache()
        if (cachedRegions.isEmpty()) {
            logger.error("캐시 데이터 없음, 빈 리스트 반환")
            return ListItemReader(emptyList())
        }
        return saveRegions(cachedRegions)?.let { ListItemReader(it) } ?: run {
            logger.error("캐시 데이터 처리 실패, 빈 리스트 반환")
            ListItemReader(emptyList())
        }
    }

    @Retryable(
        value = [SocketTimeoutException::class, IOException::class, ApiServiceException::class],
        maxAttempts = BatchConstants.MAX_RETRY_ATTEMPTS,
        backoff = Backoff(delay = BatchConstants.RETRY_DELAY_MS, multiplier = BatchConstants.RETRY_MULTIPLIER)
    )
    private fun <P, T, R> makeApiRequest(
        client: ApiClient<P, T>,
        params: P,
        transform: (T) -> Either<String, R>
    ): ApiResult<R> {
        val urlString = client.buildUrl(params)
        logger.debug("API 요청 URL: {}", urlString)

        return fetchDataFromApi(urlString).fold(
            { error -> ApiResult.Error("데이터 가져오기 실패: $error") },
            { response ->
                if (response.trim().startsWith("<")) {
                    handleXmlErrorResponse(response)
                } else {
                    client.parseResponse(response).fold(
                        { ApiResult.Error(it) },
                        { transform(it).fold({ ApiResult.Error(it) }, { ApiResult.Success(it) }) }
                    )
                }
            }
        )
    }

    private fun handleXmlErrorResponse(response: String): ApiResult.Error {
        return when {
            response.contains("HTTP ROUTING ERROR") -> {
                logger.error("HTTP 라우팅 오류: {}", response.take(100))
                ApiResult.Error("HTTP 라우팅 오류")
            }
            response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") || response.contains("SERVICE ERROR") -> {
                logger.error("API 키 오류")
                ApiResult.Error(BatchConstants.ErrorMessages.API_SERVICE_KEY_ERROR)
            }
            else -> {
                logger.error("알 수 없는 XML 오류: {}", response.take(100))
                ApiResult.Error(BatchConstants.ErrorMessages.API_ERROR_RESPONSE)
            }
        }
    }

    @Retryable(
        value = [SocketTimeoutException::class],
        maxAttempts = BatchConstants.MAX_RETRY_ATTEMPTS,
        backoff = Backoff(delay = BatchConstants.RETRY_DELAY_MS, multiplier = BatchConstants.RETRY_MULTIPLIER)
    )
    private fun fetchDataFromApi(urlString: String): Either<String, String> =
        Either.catch {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            try {
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "KWeather/1.0")
                }

                val responseCode = connection.responseCode
                logger.debug("응답 코드: {}", responseCode)

                val reader = if (responseCode in BatchConstants.HttpStatus.OK_MIN..BatchConstants.HttpStatus.OK_MAX) {
                    BufferedReader(InputStreamReader(connection.inputStream))
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream))
                }
                reader.use { it.lines().collect(java.util.stream.Collectors.joining()) }
            } finally {
                connection.disconnect()
            }
        }.mapLeft { e ->
            logger.error("HTTP 요청 실패: {}", e.message, e)
            "HTTP 요청 실패: ${e.message}"
        }

    private inline fun <T : AutoCloseable, R> use(resource: T, block: (T) -> R): R {
        try {
            return block(resource)
        } finally {
            try {
                resource.close()
            } catch (e: IOException) {
                logger.warn("리소스 닫기 실패: {}", e.message)
            }
        }
    }
}
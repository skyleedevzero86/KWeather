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
        require(serviceKey.isNotBlank()) { BatchConstants.ErrorMessages.SERVICE_KEY_EMPTY }
        require(baseUrl.isNotBlank()) { BatchConstants.ErrorMessages.BASE_URL_EMPTY }
        logger.info("RegionReader 초기화 - 사용 중인 서비스 키: $serviceKey")
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
        override fun buildUrl(params: RegionRequestParams): String {
            val url = "${baseUrl}?serviceKey=${serviceKey}" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&type=${params.type}" +
                    "&flag=${params.flag}"
            logger.info("지역 API URL 생성 완료: $url")
            return url
        }

        override fun parseResponse(response: String): Either<String, StanReginCdResponse> =
            runCatching {
                logger.debug("파싱 전 응답 데이터: ${response.take(1000)}")
                objectMapper.readValue(response, StanReginCdResponse::class.java)
            }.fold(
                onSuccess = {
                    logger.debug("파싱 성공: ${it}")
                    it.right()
                },
                onFailure = {
                    logger.error("파싱 실패: ${it.message}")
                    "지역 응답 파싱 실패: ${it.message}".left()
                }
            )
    }

    fun reader(pageNo: Int = 1, numOfRows: Int = BatchConstants.DEFAULT_PAGE_SIZE): ItemReader<RegionDto> {
        val allRegions = mutableListOf<RegionDto>()
        val params = RegionRequestParams(pageNo, numOfRows)
        val regionClient = RegionApiClient()

        return when (val result = makeApiRequest(regionClient, params) { response ->
            logger.debug("StanReginCd 컨테이너: ${response.stanReginCd}")

            // 배열의 두 번째 요소에서 row 데이터 추출
            val regions: List<RegionDto> = response.stanReginCd
                ?.find { it.row != null } // row가 있는 컨테이너 찾기
                ?.row
                ?.filter { it.isValid() }
                ?: emptyList()

            logger.info("유효한 지역 데이터 ${regions.size}개 추출")
            regions.right()
        }) {
            is ApiResult.Success -> {
                allRegions.addAll(result.data)
                if (allRegions.isEmpty()) {
                    logger.warn(BatchConstants.LogMessages.API_REQUEST_FAILED)
                    allRegions.addAll(regionCacheLoader.loadRegionsFromCache())
                }
                logger.info(BatchConstants.LogMessages.HIERARCHY_DATA_PRELOAD)
                hierarchyService.loadHierarchyData(allRegions)
                logger.info(BatchConstants.LogMessages.HIERARCHY_DATA_PRELOAD_COMPLETE)
                logger.info(BatchConstants.LogMessages.TOTAL_DATA_LOADED, allRegions.size)
                ListItemReader(allRegions)
            }
            is ApiResult.Error -> {
                logger.error("API 요청 실패: ${result.message}")
                handleApiFailure()
            }
        }
    }

    private fun handleApiFailure(): ItemReader<RegionDto> {
        val cachedRegions = regionCacheLoader.loadRegionsFromCache()
        if (cachedRegions.isEmpty()) {
            logger.error("캐시 데이터도 없음, 빈 리스트 반환")
            return ListItemReader(emptyList())
        }
        logger.info(BatchConstants.LogMessages.HIERARCHY_DATA_PRELOAD)
        try {
            hierarchyService.loadHierarchyData(cachedRegions)
            logger.info(BatchConstants.LogMessages.HIERARCHY_DATA_PRELOAD_COMPLETE)
            logger.info(BatchConstants.LogMessages.CACHE_LOAD_SUCCESS, cachedRegions.size)
            return ListItemReader(cachedRegions)
        } catch (e: Exception) {
            logger.error("캐시 데이터 처리 중 오류: ${e.message}", e)
            return ListItemReader(emptyList())
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
        logger.info("API 요청 시작 - 파라미터: $params")
        logger.info("사용 중인 서비스키: $serviceKey")

        val urlString = client.buildUrl(params)
        logger.info("최종 요청 URL: $urlString")

        return fetchDataFromApi(urlString).fold(
            { error -> ApiResult.Error("데이터 가져오기 실패: $error") },
            { response ->
                logger.info("응답 수신 완료: ${response.take(500)}")
                if (response.trim().startsWith("<")) {
                    handleXmlErrorResponse(response)
                } else {
                    when (val parseResult = client.parseResponse(response)) {
                        is Either.Left -> ApiResult.Error(parseResult.value)
                        is Either.Right -> when (val transformResult = transform(parseResult.value)) {
                            is Either.Left -> ApiResult.Error(transformResult.value)
                            is Either.Right -> ApiResult.Success(transformResult.value)
                        }
                    }
                }
            }
        )
    }

    private fun handleXmlErrorResponse(response: String): ApiResult.Error {
        when {
            response.contains("HTTP ROUTING ERROR") -> {
                logger.error("HTTP 라우팅 오류: {}", response)
                return ApiResult.Error("HTTP 라우팅 오류: $response")
            }
            response.contains("SERVICE_KEY_IS_NOT_REGISTERED_ERROR") || response.contains("SERVICE ERROR") -> {
                logger.error("API 키 오류: {}", response)
                return ApiResult.Error(BatchConstants.ErrorMessages.API_SERVICE_KEY_ERROR)
            }
            else -> {
                logger.error("XML 오류 응답 수신: {}", response.take(500))
                return ApiResult.Error(BatchConstants.ErrorMessages.API_ERROR_RESPONSE)
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
            URL(urlString).openConnection().let { conn ->
                (conn as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "KWeather/1.0 (your.email@example.com)")
                }

                val responseCode = conn.responseCode
                logger.info("응답 코드: $responseCode")

                val reader = if (responseCode in BatchConstants.HttpStatus.OK_MIN..BatchConstants.HttpStatus.OK_MAX) {
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
}
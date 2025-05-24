package com.kweather.domain.uvi.service

import arrow.core.Either
import com.kweather.domain.uvi.dto.UVIndexInfo
import com.kweather.domain.uvi.dto.UVIndexItem
import com.kweather.domain.uvi.dto.UVIndexRequestParams
import com.kweather.domain.uvi.dto.UVIndexResponse
import com.kweather.domain.weather.model.ApiResult
import com.kweather.global.common.util.ApiClientUtility
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class UVIndexService(
    @Value("\${api.livingwthridxservice.uv-base-url:}") private val uvIndexBaseUrl: String,
    @Value("\${api.service-key:}") private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(UVIndexService::class.java)

    private inner class UVIndexApiClient : ApiClientUtility.ApiClient<UVIndexRequestParams, UVIndexResponse> {
        override fun buildUrl(params: UVIndexRequestParams): String {
            val url = "${uvIndexBaseUrl}?serviceKey=$serviceKey" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}"
            logger.info("생성된 자외선 지수 API URL: $url")
            return url
        }

        override fun parseResponse(response: String): Either<String, UVIndexResponse> =
            runCatching {
                ApiClientUtility.getObjectMapper().readValue(response, UVIndexResponse::class.java)
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("자외선 지수 응답 파싱 실패: ${it.message}") }
            )
    }

    fun getUVIndex(areaNo: String, time: String): List<UVIndexInfo> {
        val adjustedTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
            .minusHours(1)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val params = UVIndexRequestParams(areaNo = areaNo, time = adjustedTime)
        val uvIndexClient = UVIndexApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(uvIndexClient, params) { response ->
            logger.info("자외선 지수 API 원시 응답: $response")
            val items = response.response?.body?.items?.item
            if (items.isNullOrEmpty() || response.response?.header?.resultCode == "03") {
                logger.info("자외선 지수 데이터 없음, 이전 시간으로 재시도")
                val previousTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .minusHours(2)
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
                val retryParams = UVIndexRequestParams(areaNo = areaNo, time = previousTime)
                when (val retryResult = ApiClientUtility.makeApiRequest(uvIndexClient, retryParams) { retryResponse ->
                    val uvIndexInfos = retryResponse.response?.body?.items?.item?.mapNotNull { item ->
                        item?.let { parseUVIndexItem(it) }
                    } ?: emptyList()
                    Either.Right(uvIndexInfos)
                }) {
                    is ApiResult.Success -> Either.Right(retryResult.data)
                    is ApiResult.Error -> Either.Right(emptyList())
                }
            } else {
                val uvIndexInfos = items.mapNotNull { item -> item?.let { parseUVIndexItem(it) } }
                Either.Right(uvIndexInfos)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("자외선 지수 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    private fun parseUVIndexItem(item: UVIndexItem): UVIndexInfo? =
        runCatching {
            logger.debug("파싱 전 UVIndexItem: $item")
            val values = mutableMapOf<String, String>().apply {
                item.h0?.let { put("h0", it) }
                item.h3?.let { put("h3", it) }
                item.h6?.let { put("h6", it) }
                item.h9?.let { put("h9", it) }
                item.h12?.let { put("h12", it) }
                item.h15?.let { put("h15", it) }
                item.h18?.let { put("h18", it) }
                item.h21?.let { put("h21", it) }
                item.h24?.let { put("h24", it) }
                item.h27?.let { put("h27", it) }
                item.h30?.let { put("h30", it) }
                item.h33?.let { put("h33", it) }
                item.h36?.let { put("h36", it) }
                item.h39?.let { put("h39", it) }
                item.h42?.let { put("h42", it) }
                item.h45?.let { put("h45", it) }
                item.h48?.let { put("h48", it) }
                item.h51?.let { put("h51", it) }
                item.h54?.let { put("h54", it) }
                item.h57?.let { put("h57", it) }
                item.h60?.let { put("h60", it) }
                item.h63?.let { put("h63", it) }
                item.h66?.let { put("h66", it) }
                item.h69?.let { put("h69", it) }
                item.h72?.let { put("h72", it) }
                item.h75?.let { put("h75", it) }
            }
            if (values.isEmpty()) {
                logger.warn("파싱된 values가 비어 있습니다: $values")
                return null
            }
            UVIndexInfo(
                date = item.date ?: run {
                    logger.warn("date 필드가 null입니다")
                    return null
                },
                values = values
            )
        }.onFailure { e ->
            logger.warn("자외선 지수 항목 파싱 실패: ${e.message}")
        }.getOrNull()

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serviceKey.isBlank() || uvIndexBaseUrl.isBlank()) {
            logger.error("자외선 지수 서비스: 필수 설정값이 누락되었습니다")
        }
    }
}
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
                // 빈 문자열이나 null 값은 추가하지 않음
                if (!item.h0.isNullOrBlank()) put("h0", item.h0)
                if (!item.h3.isNullOrBlank()) put("h3", item.h3)
                if (!item.h6.isNullOrBlank()) put("h6", item.h6)
                if (!item.h9.isNullOrBlank()) put("h9", item.h9)
                if (!item.h12.isNullOrBlank()) put("h12", item.h12)
                if (!item.h15.isNullOrBlank()) put("h15", item.h15)
                if (!item.h18.isNullOrBlank()) put("h18", item.h18)
                if (!item.h21.isNullOrBlank()) put("h21", item.h21)
                if (!item.h24.isNullOrBlank()) put("h24", item.h24)
                if (!item.h27.isNullOrBlank()) put("h27", item.h27)
                if (!item.h30.isNullOrBlank()) put("h30", item.h30)
                if (!item.h33.isNullOrBlank()) put("h33", item.h33)
                if (!item.h36.isNullOrBlank()) put("h36", item.h36)
                if (!item.h39.isNullOrBlank()) put("h39", item.h39)
                if (!item.h42.isNullOrBlank()) put("h42", item.h42)
                if (!item.h45.isNullOrBlank()) put("h45", item.h45)
                if (!item.h48.isNullOrBlank()) put("h48", item.h48)
                if (!item.h51.isNullOrBlank()) put("h51", item.h51)
                if (!item.h54.isNullOrBlank()) put("h54", item.h54)
                if (!item.h57.isNullOrBlank()) put("h57", item.h57)
                if (!item.h60.isNullOrBlank()) put("h60", item.h60)
                if (!item.h63.isNullOrBlank()) put("h63", item.h63)
                if (!item.h66.isNullOrBlank()) put("h66", item.h66)
                if (!item.h69.isNullOrBlank()) put("h69", item.h69)
                if (!item.h72.isNullOrBlank()) put("h72", item.h72)
                if (!item.h75.isNullOrBlank()) put("h75", item.h75)
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
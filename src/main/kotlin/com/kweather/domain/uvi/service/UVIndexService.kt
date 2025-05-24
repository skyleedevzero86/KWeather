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

@Service
class UVIndexService(
    @Value("\${api.livingwthridxservice.uv-base-url:}") private val uvIndexBaseUrl: String,
    @Value("\${api.service-key:}") private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(UVIndexService::class.java)

    // API 클라이언트 구현
    private inner class UVIndexApiClient : ApiClientUtility.ApiClient<UVIndexRequestParams, UVIndexResponse> {
        override fun buildUrl(params: UVIndexRequestParams): String {
            return "${uvIndexBaseUrl}?serviceKey=$serviceKey" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}"
        }

        override fun parseResponse(response: String): Either<String, UVIndexResponse> =
            runCatching {
                ApiClientUtility.getObjectMapper().readValue(response, UVIndexResponse::class.java)
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("자외선 지수 응답 파싱 실패: ${it.message}") }
            )
    }

    // 자외선 지수 데이터 가져오기
    fun getUVIndex(areaNo: String, time: String): List<UVIndexInfo> {
        val params = UVIndexRequestParams(areaNo = areaNo, time = time)
        val uvIndexClient = UVIndexApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(uvIndexClient, params) { response ->
            val uvIndexInfos = response.response?.body?.items?.mapNotNull { item ->
                item?.let { parseUVIndexItem(it) }
            } ?: emptyList()
            Either.Right(uvIndexInfos)
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("자외선 지수 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    // 자외선 지수 항목 파싱
    private fun parseUVIndexItem(item: UVIndexItem): UVIndexInfo? =
        runCatching {
            val values = mutableMapOf<String, String>()
            with(item) {
                listOf(
                    "h0" to h0, "h3" to h3, "h6" to h6, "h9" to h9,
                    "h12" to h12, "h15" to h15, "h18" to h18, "h21" to h21,
                    "h24" to h24, "h27" to h27, "h30" to h30, "h33" to h33,
                    "h36" to h36, "h39" to h39, "h42" to h42, "h45" to h45,
                    "h48" to h48, "h51" to h51, "h54" to h54, "h57" to h57,
                    "h60" to h60, "h63" to h63, "h66" to h66, "h69" to h69,
                    "h72" to h72, "h75" to h75
                ).forEach { (key, value) -> value?.takeIf { it.isNotEmpty() }?.let { values[key] = it } }
            }
            if (values.isEmpty()) return null
            UVIndexInfo(
                date = item.date ?: return null,
                values = values
            )
        }.onFailure { e ->
            logger.warn("자외선 지수 항목 파싱 실패: ${e.message}")
        }.getOrNull()

    init {
        validateConfiguration()
    }

    // 설정값 검증
    private fun validateConfiguration() {
        if (serviceKey.isBlank() || uvIndexBaseUrl.isBlank()) {
            logger.error("자외선 지수 서비스: 필수 설정값이 누락되었습니다")
        }
    }
}
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
            val items = response.response?.body?.items?.item
            if (items.isNullOrEmpty() || response.response?.header?.resultCode == "03") {
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
                emptyList()
            }
        }
    }

    private fun parseUVIndexItem(item: UVIndexItem): UVIndexInfo? =
        runCatching {
            val values = mutableMapOf<String, Float>().apply {
                put("h0", item.h0?.toFloatOrNull() ?: 0.0f)
                put("h3", item.h3?.toFloatOrNull() ?: 0.0f)
                put("h6", item.h6?.toFloatOrNull() ?: 0.0f)
                put("h9", item.h9?.toFloatOrNull() ?: 0.0f)
                put("h12", item.h12?.toFloatOrNull() ?: 0.0f)
                put("h15", item.h15?.toFloatOrNull() ?: 0.0f)
                put("h18", item.h18?.toFloatOrNull() ?: 0.0f)
                put("h21", item.h21?.toFloatOrNull() ?: 0.0f)
                put("h24", item.h24?.toFloatOrNull() ?: 0.0f)
                put("h27", item.h27?.toFloatOrNull() ?: 0.0f)
                put("h30", item.h30?.toFloatOrNull() ?: 0.0f)
                put("h33", item.h33?.toFloatOrNull() ?: 0.0f)
                put("h36", item.h36?.toFloatOrNull() ?: 0.0f)
                put("h39", item.h39?.toFloatOrNull() ?: 0.0f)
                put("h42", item.h42?.toFloatOrNull() ?: 0.0f)
                put("h45", item.h45?.toFloatOrNull() ?: 0.0f)
                put("h48", item.h48?.toFloatOrNull() ?: 0.0f)
                put("h51", item.h51?.toFloatOrNull() ?: 0.0f)
                put("h54", item.h54?.toFloatOrNull() ?: 0.0f)
                put("h57", item.h57?.toFloatOrNull() ?: 0.0f)
                put("h60", item.h60?.toFloatOrNull() ?: 0.0f)
                put("h63", item.h63?.toFloatOrNull() ?: 0.0f)
                put("h66", item.h66?.toFloatOrNull() ?: 0.0f)
                put("h69", item.h69?.toFloatOrNull() ?: 0.0f)
                put("h72", item.h72?.toFloatOrNull() ?: 0.0f)
                put("h75", item.h75?.toFloatOrNull() ?: 0.0f)
            }
            if (values.isEmpty()) {
                return null
            }
            UVIndexInfo(
                date = item.date ?: return null,
                values = values.toMap()
            )
        }.onFailure { e ->
        }.getOrNull()

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serviceKey.isBlank() || uvIndexBaseUrl.isBlank()) {
        }
    }
}
package com.kweather.domain.airstagnation.service

import com.kweather.global.common.util.ApiClientUtility
import arrow.core.Either
import com.kweather.domain.airstagnation.dto.AirStagnationIndexInfo
import com.kweather.domain.airstagnation.dto.AirStagnationIndexItem
import com.kweather.domain.airstagnation.dto.AirStagnationIndexRequestParams
import com.kweather.domain.airstagnation.dto.AirStagnationIndexResponse
import com.kweather.domain.weather.model.ApiResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AirStagnationIndexService(
    @Value("\${api.livingwthridxservice.airstagnation-base-url:}") private val airStagnationIndexBaseUrl: String,
    @Value("\${api.service-key:}") private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(AirStagnationIndexService::class.java)

    private inner class AirStagnationIndexApiClient : ApiClientUtility.ApiClient<AirStagnationIndexRequestParams, AirStagnationIndexResponse> {
        override fun buildUrl(params: AirStagnationIndexRequestParams): String {
            val url = "${airStagnationIndexBaseUrl}?serviceKey=$serviceKey" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}" +
                    (params.requestCode?.let { "&requestCode=$it" } ?: "")
            logger.info("대기정체지수 API 요청 URL: $url")
            return url
        }

        override fun parseResponse(response: String): Either<String, AirStagnationIndexResponse> =
            runCatching {
                ApiClientUtility.getObjectMapper().readValue(response, AirStagnationIndexResponse::class.java)
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("대기정체지수 응답 파싱 실패: ${it.message}") }
            )
    }

    fun getAirStagnationIndex(areaNo: String, time: String): List<AirStagnationIndexInfo> {
        val airStagnationIndexClient = AirStagnationIndexApiClient()
        var currentTime = time
        repeat(3) { attempt ->
            val params = AirStagnationIndexRequestParams(areaNo, currentTime, 1, 10, "json")
            when (val result = ApiClientUtility.makeApiRequest(airStagnationIndexClient, params) { response ->
                logger.info("대기정체지수 API 응답: $response")
                if (response.response?.header?.resultCode == "03" && attempt < 2) {
                    logger.info("대기정체지수 데이터 없음 (resultCode: 03), 시도 ${attempt + 1}")
                    Either.Left("NO_DATA")
                } else {
                    val items = response.response?.body?.items?.item
                    if (items.isNullOrEmpty()) {
                        logger.warn("대기정체지수 데이터가 비어 있습니다: $response")
                        Either.Right(emptyList())
                    } else {
                        val infos = items.mapNotNull { parseAirStagnationIndexItem(it) }
                        logger.info("파싱된 대기정체지수 데이터: $infos")
                        Either.Right(infos)
                    }
                }
            }) {
                is ApiResult.Success -> return result.data
                is ApiResult.Error -> {
                    logger.error("API 요청 실패 (시도 ${attempt + 1}): ${result.message}")
                    if (result.message.contains("NO_DATA")) {
                        currentTime = LocalDateTime.parse(currentTime, DateTimeFormatter.ofPattern("yyyyMMddHH"))
                            .minusHours(1)
                            .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
                    } else {
                        return emptyList()
                    }
                }
            }
        }
        logger.warn("모든 시도 후에도 대기정체지수 데이터가 없음")
        return emptyList()
    }

    private fun parseAirStagnationIndexItem(item: AirStagnationIndexItem): AirStagnationIndexInfo? =
        runCatching {
            logger.debug("파싱 전 AirStagnationIndexItem: $item")
            val values = mutableMapOf<String, String>()
            with(item) {
                listOf(
                    "h3" to h3, "h6" to h6, "h9" to h9, "h12" to h12,
                    "h15" to h15, "h18" to h18, "h21" to h21, "h24" to h24,
                    "h27" to h27, "h30" to h30, "h33" to h33, "h36" to h36,
                    "h39" to h39, "h42" to h42, "h45" to h45, "h48" to h48,
                    "h51" to h51, "h54" to h54, "h57" to h57, "h60" to h60,
                    "h63" to h63, "h66" to h66, "h69" to h69, "h72" to h72,
                    "h75" to h75, "h78" to h78
                ).forEach { (key, value) -> value?.takeIf { it.isNotEmpty() }?.let { values[key] = it } }
            }
            if (values.isEmpty()) {
                logger.warn("파싱된 values가 비어 있습니다: $item")
                return null
            }
            AirStagnationIndexInfo(
                date = item.date ?: run {
                    logger.warn("date 필드가 null입니다: $item")
                    return null
                },
                values = values
            )
        }.onFailure { e ->
            logger.warn("대기정체지수 항목 파싱 실패: ${e.message}", e)
        }.getOrNull()

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serviceKey.isBlank() || airStagnationIndexBaseUrl.isBlank()) {
            logger.error("대기정체지수 서비스: 필수 설정값이 누락되었습니다")
        }
    }
}
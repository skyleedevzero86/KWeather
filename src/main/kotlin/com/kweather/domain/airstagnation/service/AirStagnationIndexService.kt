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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class AirStagnationIndexService(
    @Value("\${api.livingwthridxservice.airstagnation-base-url:}") private val airStagnationIndexBaseUrl: String,
    @Value("\${api.service-key:}") private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(AirStagnationIndexService::class.java)

    // API 클라이언트 구현
    private inner class AirStagnationIndexApiClient : ApiClientUtility.ApiClient<AirStagnationIndexRequestParams, AirStagnationIndexResponse> {
        override fun buildUrl(params: AirStagnationIndexRequestParams): String {
            return "${airStagnationIndexBaseUrl}?serviceKey=$serviceKey" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}" +
                    (params.requestCode?.let { "&requestCode=$it" } ?: "")
        }

        override fun parseResponse(response: String): Either<String, AirStagnationIndexResponse> =
            runCatching {
                ApiClientUtility.getObjectMapper().readValue(response, AirStagnationIndexResponse::class.java)
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("대기정체지수 응답 파싱 실패: ${it.message}") }
            )
    }

    // 대기정체지수 데이터 가져오기
    fun getAirStagnationIndex(areaNo: String, time: String): List<AirStagnationIndexInfo> {
        val params = AirStagnationIndexRequestParams(areaNo = areaNo, time = time, pageNo = 1, numOfRows = 10, dataType = "json")
        val airStagnationIndexClient = AirStagnationIndexApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(airStagnationIndexClient, params) { response ->
            if (response.response?.header?.resultCode == "03" && response.response.header.resultMsg == "NO_DATA") {
                logger.info("대기정체지수 데이터 없음, 이전 시간으로 재시도")
                val previousTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
                val retryParams = AirStagnationIndexRequestParams(areaNo = areaNo, time = previousTime, pageNo = 1, numOfRows = 10, dataType = "json")
                when (val retryResult = ApiClientUtility.makeApiRequest(airStagnationIndexClient, retryParams) { retryResponse ->
                    val airStagnationIndexInfos = retryResponse.response?.body?.items?.mapNotNull { item ->
                        item?.let { parseAirStagnationIndexItem(it) }
                    } ?: emptyList()
                    Either.Right(airStagnationIndexInfos)
                }) {
                    is ApiResult.Success -> Either.Right(retryResult.data)
                    is ApiResult.Error -> Either.Right(emptyList())
                }
            } else {
                val airStagnationIndexInfos = response.response?.body?.items?.mapNotNull { item ->
                    item?.let { parseAirStagnationIndexItem(it) }
                } ?: emptyList()
                Either.Right(airStagnationIndexInfos)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("대기정체지수 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    // 대기정체지수 항목 파싱
    private fun parseAirStagnationIndexItem(item: AirStagnationIndexItem): AirStagnationIndexInfo? =
        runCatching {
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
            if (values.isEmpty()) return null
            AirStagnationIndexInfo(
                date = item.date ?: return null,
                values = values
            )
        }.onFailure { e ->
            logger.warn("대기정체지수 항목 파싱 실패: ${e.message}")
        }.getOrNull()

    init {
        validateConfiguration()
    }

    // 설정값 검증
    private fun validateConfiguration() {
        if (serviceKey.isBlank() || airStagnationIndexBaseUrl.isBlank()) {
            logger.error("대기정체지수 서비스: 필수 설정값이 누락되었습니다")
        }
    }
}
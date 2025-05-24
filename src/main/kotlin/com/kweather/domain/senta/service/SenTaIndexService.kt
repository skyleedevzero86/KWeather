package com.kweather.domain.senta.service

import arrow.core.Either
import com.kweather.domain.senta.dto.SenTaIndexInfo
import com.kweather.domain.senta.dto.SenTaIndexItem
import com.kweather.domain.senta.dto.SenTaIndexRequestParams
import com.kweather.domain.senta.dto.SenTaIndexResponse
import com.kweather.domain.weather.model.ApiResult
import com.kweather.global.common.util.ApiClientUtility
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SenTaIndexService(
    @Value("\${api.livingwthridxservice.senta-base-url:}") private val senTaIndexBaseUrl: String,
    @Value("\${api.service-key:}") private val serviceKey: String
) {
    private val logger = LoggerFactory.getLogger(SenTaIndexService::class.java)

    // API 클라이언트 구현
    private inner class SenTaIndexApiClient : ApiClientUtility.ApiClient<SenTaIndexRequestParams, SenTaIndexResponse> {
        override fun buildUrl(params: SenTaIndexRequestParams): String {
            return "${senTaIndexBaseUrl}?serviceKey=$serviceKey" +
                    "&pageNo=${params.pageNo}" +
                    "&numOfRows=${params.numOfRows}" +
                    "&dataType=${params.dataType}" +
                    "&areaNo=${params.areaNo}" +
                    "&time=${params.time}" +
                    "&requestCode=${params.requestCode}"
        }

        override fun parseResponse(response: String): Either<String, SenTaIndexResponse> =
            runCatching {
                ApiClientUtility.getObjectMapper().readValue(response, SenTaIndexResponse::class.java)
            }.fold(
                onSuccess = { Either.Right(it) },
                onFailure = { Either.Left("여름철 체감온도 응답 파싱 실패: ${it.message}") }
            )
    }

    // 여름철 체감온도 데이터 가져오기
    fun getSenTaIndex(areaNo: String, time: String): List<SenTaIndexInfo> {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        if (now.monthValue !in 5..9) {
            logger.info("여름철 체감온도 데이터는 5~9월에만 제공됩니다. 현재 월: ${now.monthValue}")
            return emptyList()
        }

        val params = SenTaIndexRequestParams(areaNo = areaNo, time = time)
        val senTaIndexClient = SenTaIndexApiClient()

        return when (val result = ApiClientUtility.makeApiRequest(senTaIndexClient, params) { response ->
            val items = response.response?.body?.items
            if (items.isNullOrEmpty()) {
                logger.info("체감온도 데이터 없음, 이전 시간으로 재시도")
                val previousTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
                val retryParams = SenTaIndexRequestParams(areaNo = areaNo, time = previousTime)
                when (val retryResult = ApiClientUtility.makeApiRequest(senTaIndexClient, retryParams) { retryResponse ->
                    val senTaIndexInfos = retryResponse.response?.body?.items?.mapNotNull { item ->
                        item?.let { parseSenTaIndexItem(it) }
                    } ?: emptyList()
                    Either.Right(senTaIndexInfos)
                }) {
                    is ApiResult.Success -> Either.Right(retryResult.data)
                    is ApiResult.Error -> Either.Right(emptyList())
                }
            } else {
                val senTaIndexInfos = items.mapNotNull { item -> item?.let { parseSenTaIndexItem(it) } }
                Either.Right(senTaIndexInfos)
            }
        }) {
            is ApiResult.Success -> result.data
            is ApiResult.Error -> {
                logger.error("여름철 체감온도 API 요청 실패: ${result.message}")
                emptyList()
            }
        }
    }

    // 여름철 체감온도 항목 파싱
    private fun parseSenTaIndexItem(item: SenTaIndexItem): SenTaIndexInfo? =
        runCatching {
            val values = mutableMapOf<String, String>()
            with(item) {
                listOf(
                    "h1" to h1, "h2" to h2, "h3" to h3, "h4" to h4,
                    "h5" to h5, "h6" to h6, "h7" to h7, "h8" to h8,
                    "h9" to h9, "h10" to h10, "h11" to h11, "h12" to h12,
                    "h13" to h13, "h14" to h14, "h15" to h15, "h16" to h16,
                    "h17" to h17, "h18" to h18, "h19" to h19, "h20" to h20,
                    "h21" to h21, "h22" to h22, "h23" to h23, "h24" to h24,
                    "h25" to h25, "h26" to h26, "h27" to h27, "h28" to h28,
                    "h29" to h29, "h30" to h30, "h31" to h31, "h32" to h32
                ).forEach { (key, value) -> value?.takeIf { it.isNotEmpty() }?.let { values[key] = it } }
            }
            if (values.isEmpty()) return null
            SenTaIndexInfo(
                date = item.date ?: return null,
                values = values
            )
        }.onFailure { e ->
            logger.warn("체감온도 항목 파싱 실패: ${e.message}")
        }.getOrNull()

    init {
        validateConfiguration()
    }

    // 설정값 검증
    private fun validateConfiguration() {
        if (serviceKey.isBlank() || senTaIndexBaseUrl.isBlank()) {
            logger.error("여름철 체감온도 서비스: 필수 설정값이 누락되었습니다")
        }
    }
}
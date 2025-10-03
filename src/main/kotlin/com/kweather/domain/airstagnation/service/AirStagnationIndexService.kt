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
            return url
        }

        override fun parseResponse(response: String): Either<String, AirStagnationIndexResponse> =
            runCatching {
                if (response.trim().equals("Error", ignoreCase = true)) {
                    return Either.Left("API에서 Error 응답을 반환했습니다")
                }
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
                if (response == null) {
                    Either.Left("API 응답이 null입니다")
                } else if (response.response?.header?.resultCode == "03" && attempt < 2) {
                    Either.Left("NO_DATA")
                } else {
                    val items = response.response?.body?.items?.item
                    if (items.isNullOrEmpty()) {
                        Either.Right(emptyList())
                    } else {
                        val infos = items.mapNotNull { parseAirStagnationIndexItem(it) }
                        Either.Right(infos)
                    }
                }
            }) {
                is ApiResult.Success -> return result.data
                is ApiResult.Error -> {
                    if (result.message.contains("NO_DATA")) {
                        currentTime = LocalDateTime.parse(currentTime, DateTimeFormatter.ofPattern("yyyyMMddHH"))
                            .minusHours(1)
                            .format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
                    } else if (result.message.contains("Error 응답을 반환했습니다") ||
                        result.message.contains("응답 파싱 실패")) {
                        return emptyList()
                    } else {
                        return emptyList()
                    }
                }
            }
        }
        return emptyList()
    }

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
            if (values.isEmpty()) {
                return null
            }
            AirStagnationIndexInfo(
                date = item.date ?: return null,
                values = values
            )
        }.onFailure { e ->
        }.getOrNull()

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serviceKey.isBlank() || airStagnationIndexBaseUrl.isBlank()) {
        }
    }
}
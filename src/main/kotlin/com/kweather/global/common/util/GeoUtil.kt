package com.kweather.global.common.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.flatMap
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class GeoUtil(
    @Value("\${kakao.api.key}") private val kakaoApiKey: String,
    @Value("\${kakao.api.base-url}") private val baseUrl: String,
    @Value("\${kakao.api.endpoint}") private val endpoint: String
) {

    private val logger = LoggerFactory.getLogger(GeoUtil::class.java)
    private val objectMapper = jacksonObjectMapper()

    interface ApiKeyProvider {
        fun getApiKey(): String
    }

    private val apiKeyProvider = object : ApiKeyProvider {
        override fun getApiKey(): String = kakaoApiKey
    }

    fun fetchGeoCoordinates(address: String): Either<String, Pair<String, String>> {
        val result = createHttpRequest(address)
            .flatMap { request -> executeRequest(request) }
            .flatMap { jsonResponse -> parseCoordinates(jsonResponse) }

        return result
    }

    private fun createHttpRequest(address: String): Either<String, HttpGet> = try {
        val encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString())
        val apiKey = "KakaoAK ${apiKeyProvider.getApiKey()}"
        val url = "$baseUrl$endpoint?query=$encodedAddress"

        val request = HttpGet(url).apply {
            addHeader("Authorization", apiKey)
        }

        request.right()
    } catch (e: Exception) {
        "HTTP 요청 생성 실패: ${e.message}".left()
    }

    private fun executeRequest(request: HttpGet): Either<String, String> = try {
        HttpClients.createDefault().use { httpClient ->
            httpClient.execute(request).use { response ->
                val statusCode = response.code
                if (statusCode != 200) {
                    "HTTP 응답 오류: $statusCode".left()
                } else {
                    val json = EntityUtils.toString(response.entity)
                    json.right()
                }
            }
        }
    } catch (e: Exception) {
        "HTTP 요청 실행 실패: ${e.message}".left()
    }

    private fun parseCoordinates(jsonResponse: String): Either<String, Pair<String, String>> = try {
        val result = objectMapper.readTree(jsonResponse)
        val documents = result["documents"]

        when {
            documents == null -> "응답 데이터 형식 오류: documents 필드 없음".left()
            documents.isEmpty -> "검색 결과 없음: 해당 주소를 찾을 수 없습니다".left()
            else -> extractCoordinatesFromDocument(documents[0])
        }
    } catch (e: Exception) {
        "JSON 파싱 실패: ${e.message}".left()
    }

    private fun extractCoordinatesFromDocument(document: JsonNode): Either<String, Pair<String, String>> = try {
        val x = document["x"]?.asText()
        val y = document["y"]?.asText()

        when {
            x.isNullOrBlank() || y.isNullOrBlank() -> "좌표 데이터 누락".left()
            else -> Pair(y, x).right()
        }
    } catch (e: Exception) {
        "좌표 추출 실패: ${e.message}".left()
    }
}
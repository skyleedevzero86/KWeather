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

    // Strategy Pattern을 위한 인터페이스
    interface ApiKeyProvider {
        fun getApiKey(): String
    }

    // 설정에서 API 키를 가져오는 전략
    private val apiKeyProvider = object : ApiKeyProvider {
        override fun getApiKey(): String = kakaoApiKey
    }

    fun fetchGeoCoordinates(address: String): Either<String, Pair<String, String>> {
        logger.info("주소 좌표 조회 시작: $address")

        val result = createHttpRequest(address)
            .flatMap { request -> executeRequest(request) }
            .flatMap { jsonResponse -> parseCoordinates(jsonResponse) }

        result.fold(
            { error -> logger.error("좌표 조회 실패: $error") },
            { (lat, lng) -> logger.info("좌표 조회 성공 - 위도: $lat, 경도: $lng") }
        )

        return result
    }

    private fun createHttpRequest(address: String): Either<String, HttpGet> = try {
        val encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString())
        val apiKey = "KakaoAK ${apiKeyProvider.getApiKey()}"
        val url = "$baseUrl$endpoint?query=$encodedAddress"

        val request = HttpGet(url).apply {
            addHeader("Authorization", apiKey)
        }

        logger.info("HTTP 요청 생성 완료: $url")
        request.right()
    } catch (e: Exception) {
        logger.error("HTTP 요청 생성 실패: ${e.message}", e)
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
                    logger.info("API 응답 수신 완료")
                    json.right()
                }
            }
        }
    } catch (e: Exception) {
        logger.error("HTTP 요청 실행 실패: ${e.message}", e)
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
        logger.error("JSON 파싱 실패: ${e.message}", e)
        "JSON 파싱 실패: ${e.message}".left()
    }

    private fun extractCoordinatesFromDocument(document: JsonNode): Either<String, Pair<String, String>> = try {
        val x = document["x"]?.asText()
        val y = document["y"]?.asText()

        when {
            x.isNullOrBlank() || y.isNullOrBlank() -> "좌표 데이터 누락".left()
            else -> Pair(y, x).right() // Kakao API에서 x는 경도, y는 위도
        }
    } catch (e: Exception) {
        logger.error("좌표 추출 실패: ${e.message}", e)
        "좌표 추출 실패: ${e.message}".left()
    }
}
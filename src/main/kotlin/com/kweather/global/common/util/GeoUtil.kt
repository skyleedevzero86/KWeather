package com.kweather.global.common.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils

@Component
class GeoUtil {

    fun fetchGeoCoordinates(address: String): Either<String, Pair<String, String>> {
        return try {
            val apiKey = "KakaoAK ${System.getenv("KAKAO_API_KEY") ?: "06b18721985d194ccecb60a6fd6ab132"}"
            val url = "https://dapi.kakao.com/v2/local/search/address.json?query=${address}"
            val httpClient = HttpClients.createDefault()
            val request = HttpGet(url)
            request.addHeader("Authorization", apiKey)

            // HttpClient 5.x의 execute 메서드를 람다로 처리
            val coordinates = httpClient.execute(request) { response ->
                val entity = response.entity
                val json = EntityUtils.toString(entity)
                val mapper = jacksonObjectMapper()
                val result = mapper.readTree(json)

                // documents가 비어 있는 경우 예외 처리
                if (result["documents"].isEmpty) {
                    throw Exception("주소에 해당하는 좌표를 찾을 수 없습니다.")
                }

                val documents = result["documents"].get(0)
                val x = documents["x"].asText()
                val y = documents["y"].asText()
                Pair(x, y)
            }

            coordinates.right() // Pair<String, String>을 Either의 right로 감쌈
        } catch (e: Exception) {
            "위치 조회 실패: ${e.message}".left() // String을 Either의 left로 감쌈
        }
    }
}
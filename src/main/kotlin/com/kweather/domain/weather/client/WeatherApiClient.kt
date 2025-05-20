package com.kweather.domain.weather.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class WeatherApiClient(
    private val restTemplate: RestTemplate
) {
    @Value("\${api.weather.service-key}")
    private lateinit var serviceKey: String // 인코딩되지 않은 값 주입 권장

    @Value("\${api.weather.base-url}")
    private lateinit var baseUrl: String

    data class WeatherResponse(
        val response: Response
    )

    data class Response(
        val header: Header,
        val body: Body
    )

    data class Header(
        val resultCode: String,
        val resultMsg: String
    )

    data class Body(
        val dataType: String,
        val items: Items,
        val pageNo: Int,
        val numOfRows: Int,
        val totalCount: Int
    )

    data class Items(
        val item: List<Item>
    )

    data class Item(
        val baseDate: String,
        val baseTime: String,
        val category: String,
        val nx: Int,
        val ny: Int,
        val obsrValue: String
    )

    fun fetchUltraSrtNcst(nx: Int, ny: Int, baseDate: String, baseTime: String): WeatherResponse? {
        val url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("serviceKey", serviceKey, true) // raw=true로 추가 인코딩 방지
            .queryParam("numOfRows", 10)
            .queryParam("pageNo", 1)
            .queryParam("base_date", baseDate)
            .queryParam("base_time", baseTime)
            .queryParam("nx", nx)
            .queryParam("ny", ny)
            .queryParam("dataType", "json")
            .toUriString()

        return try {
            restTemplate.getForObject(url, WeatherResponse::class.java)
        } catch (e: Exception) {
            println("API 호출 오류: ${e.message}")
            null
        }
    }
}
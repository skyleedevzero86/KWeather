package com.kweather.domain.weather.service

import com.kweather.domain.weather.client.WeatherApiClient
import com.kweather.domain.weather.mapper.WeatherImageMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class WeatherService(
    private val weatherApiClient: WeatherApiClient,
    private val weatherImageMapper: WeatherImageMapper
) {
    @Value("\${api.region.service-key}")
    private lateinit var serviceKey: String

    @Value("\${api.region.base-url}")
    private lateinit var baseUrl: String

    fun fetchAndDisplayWeather(nx: Int, ny: Int, baseDate: String, baseTime: String) {
        val weatherData = weatherApiClient.fetchUltraSrtNcst(serviceKey, baseUrl, nx, ny, baseDate, baseTime)
        if (weatherData?.response?.body?.items?.item == null) {
            println("날씨 데이터를 가져오지 못했습니다.")
            return
        }
        weatherData.response.body.items.item.forEach { item ->
            val imageText = weatherImageMapper.mapToImageText(item.category, item.obsrValue)
            println(imageText)
        }
    }
}
package com.kweather.domain.weather.dto


import com.kweather.domain.weather.model.AirQuality
import com.kweather.domain.weather.model.Body
import com.kweather.domain.weather.model.Header
import com.kweather.domain.weather.model.UVIndex

data class WeatherResponseData(
    val header: Header? = null,
    val body: Body? = null,
    val airQuality: AirQuality? = null, // 미세먼지 정보 추가
    val uvIndex: UVIndex? = null       // 초미세먼지 정보 추가
)
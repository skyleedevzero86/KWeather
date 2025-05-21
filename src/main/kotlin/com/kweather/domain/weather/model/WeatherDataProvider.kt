package com.kweather.domain.weather.model

import com.kweather.domain.weather.entity.Weather


/**
 * 날씨 정보 데이터 제공자 인터페이스
 */
interface WeatherDataProvider {
    fun getWeatherData(date: String, time: String): Weather
    fun getDustForecastData(date: String, informCode: String): List<com.kweather.domain.forecast.dto.ForecastInfo>
}
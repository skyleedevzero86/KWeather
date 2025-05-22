package com.kweather.domain.weather.model

import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.weather.dto.RealTimeDustInfo

interface WeatherDataProvider {
    fun getWeatherData(date: String, time: String): Weather
    fun getDustForecastData(date: String, informCode: String): List<com.kweather.domain.forecast.dto.ForecastInfo>
    fun getRealTimeDustData(sidoName: String): List<RealTimeDustInfo>
}
package com.kweather.domain.weather.model

import com.kweather.domain.airstagnation.dto.AirStagnationIndexInfo
import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.realtime.dto.RealTimeDustInfo
import com.kweather.domain.senta.dto.SenTaIndexInfo
import com.kweather.domain.uvi.dto.UVIndexInfo

interface WeatherDataProvider {
    fun getWeatherData(date: String, time: String): Weather
    fun getDustForecastData(date: String, informCode: String): List<com.kweather.domain.forecast.dto.ForecastInfo>
    fun getRealTimeDustData(sidoName: String): List<RealTimeDustInfo>
    fun getUVIndexData(areaNo: String, time: String): List<UVIndexInfo>
    fun getSenTaIndexData(areaNo: String, time: String): List<SenTaIndexInfo>
    fun getAirStagnationIndexData(areaNo: String, time: String): List<AirStagnationIndexInfo>
}
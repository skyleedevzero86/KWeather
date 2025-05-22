package com.kweather.domain.weather.model

import com.kweather.domain.weather.dto.WeatherItem

data class Items(
    val item: List<WeatherItem>? = null
)
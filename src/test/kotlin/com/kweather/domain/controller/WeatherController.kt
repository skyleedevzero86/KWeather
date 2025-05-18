package com.kweather.domain.controller

import com.kweather.global.common.util.DateTimeUtils.getCurrentDateTimeFormatted
import com.kweather.domain.entity.Weather
import com.kweather.domain.model.*


class WeatherController {
    @GetMapping("/")
    fun getWeather(model: Model): String {

        val (date, time) = getCurrentDateTimeFormatted()


        val weatherData = Weather(
            date = date,
            time = time,
            location = "한남동 (용산구)",
            currentTemperature = "-1.8°C",
            highLowTemperature = "-5°C / -1°C",
            weatherCondition = "맑음",
            windSpeed = "1km/초(남서) m/s 0",
            airQuality = AirQuality("미세먼지", "yellow-smiley", "좋음", "20 ㎍/㎥"),
            uvIndex = UVIndex("초미세먼지", "yellow-smiley", "좋음", "8 ㎍/㎥"),
            hourlyForecast = listOf(
                HourlyForecast("지금", "moon", "-1.8°C", "34%"),
                HourlyForecast("0시", "moon", "-6°C", "55%"),
                HourlyForecast("3시", "moon", "-6°C", "60%"),
                HourlyForecast("6시", "moon", "-7°C", "67%"),
                HourlyForecast("9시", "sun", "-6°C", "55%")
            )
        )

        return null
    }
}
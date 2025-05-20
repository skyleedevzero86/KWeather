package com.kweather.domain.weather.controller

import com.kweather.global.common.util.DateTimeUtils.getCurrentDateTimeFormatted
import com.kweather.domain.weather.entity.Weather
import com.kweather.domain.weather.model.AirQuality
import com.kweather.domain.weather.model.HourlyForecast
import com.kweather.domain.weather.model.UVIndex
import com.kweather.domain.weather.service.WeatherService
import com.kweather.global.common.util.DateTimeUtils
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * 날씨 정보를 처리하는 컨트롤러입니다.
 * 메인 페이지에 날씨 정보를 전달합니다.
 */
@Controller
class WeatherController (
    private val weatherService: WeatherService
){

    /**
     * 루트 경로("/") 요청 시 호출되며,
     * 현재 날씨 정보를 모델에 담아 View에 전달합니다.
     *
     * @param model Spring MVC의 Model 객체로, View에 데이터 전달에 사용됩니다.
     * @return 날씨 정보를 표시할 뷰 이름
     */
    @GetMapping("/")
    fun getWeather(model: Model): String {
        val (date, time) = getCurrentDateTimeFormatted()
        val hour = DateTimeUtils.getCurrentHour()

        val weatherData = Weather(
            date = date,
            time = time,
            location = "한남동 (용산구)",
            currentTemperature = "-1.8°C",
            highLowTemperature = "-5°C / -1°C",
            weatherCondition = "맑음",
            windSpeed = "1km/초(남서) m/s 0",
            airQuality = AirQuality("미세먼지", "yellow-smiley", "좋음", "20 ㎍/㎥", "㎍/㎥"),
            uvIndex = UVIndex("초미세먼지", "yellow-smiley", "좋음", "8 ㎍/㎥", "㎍/㎥"),
            hourlyForecast = listOf(
                HourlyForecast("지금", "moon", "-1.8°C", "34%"),
                HourlyForecast("0시", "moon", "-6°C", "55%"),
                HourlyForecast("3시", "moon", "-6°C", "60%"),
                HourlyForecast("6시", "moon", "-7°C", "67%"),
                HourlyForecast("9시", "sun", "-6°C", "55%")
            )
        )

        val timeOfDay = when {
            hour in 6..11 -> " ( 아침 )"
            hour in 12..17 -> " ( 낮 )"
            hour in 18..23 -> " ( 저녁 )"
            else -> "새벽 (밤)"
        }

        model.addAttribute("timeOfDay", timeOfDay)
        model.addAttribute("weather", weatherData)
        return "domain/weather/weather"
    }

    @GetMapping("/weather")
    fun getWeather(
        @RequestParam("nx") nx: Int,
        @RequestParam("ny") ny: Int
    ) {
        val baseDate = DateTimeUtils.getBaseDate()
        val baseTime = DateTimeUtils.getBaseTime()

        val result = weatherService.fetchAndDisplayWeather(nx, ny, baseDate, baseTime)

        println("결과: $result")
    }


}

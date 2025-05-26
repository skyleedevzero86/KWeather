package com.kweather.domain.locations.controller

import com.kweather.domain.locations.service.GeoService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import arrow.core.fold

@RestController
class GeoController(private val geoService: GeoService) {

    @GetMapping("/geo")
    fun getGeoCoordinates(@RequestParam address: String): Map<String, String> {
        return geoService.getCoordinates(address).fold(
            { errorMessage ->
                println("오류 발생: $errorMessage")
                mapOf(
                    "latitude" to "0.0",
                    "longitude" to "0.0",
                    "error" to errorMessage
                )
            },
            { (latitude, longitude) ->
                println("위도: ${latitude.toDouble().toInt()}")
                println("경도: ${longitude.toDouble().toInt()}")
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude
                )
            }
        )
    }
}
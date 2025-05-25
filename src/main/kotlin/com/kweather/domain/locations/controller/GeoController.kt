package com.kweather.domain.locations.controller

import com.kweather.domain.locations.service.GeoService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class GeoController(private val geoService: GeoService) {

    @GetMapping("/geo")
    fun getGeoCoordinates(@RequestParam address: String): Map<String, String> {
        val (x, y) = geoService.getCoordinates(address)
        println("위도 (x): $x")
        println("경도 (y): $y")
        return mapOf("latitude" to x, "longitude" to y)
    }
}
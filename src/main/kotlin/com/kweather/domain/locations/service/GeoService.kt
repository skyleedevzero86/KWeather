package com.kweather.domain.locations.service

import org.springframework.stereotype.Service
import arrow.core.getOrElse
import com.kweather.global.common.util.GeoUtil

@Service
class GeoService(private val geoUtil: GeoUtil) {

    fun getCoordinates(address: String): Pair<String, String> =
        geoUtil.fetchGeoCoordinates(address)
            .getOrElse { Pair("0.0", "0.0") }
}
package com.kweather.domain.weather.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Header(
    val resultCode: String?,
    val resultMsg: String?
)
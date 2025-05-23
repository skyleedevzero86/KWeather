package com.kweather.domain.weather.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Header(
    @JsonProperty("resultCode")
    val resultCode: String?,
    @JsonProperty("resultMsg")
    val resultMsg: String?
)
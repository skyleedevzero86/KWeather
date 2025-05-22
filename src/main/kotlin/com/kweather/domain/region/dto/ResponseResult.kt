package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResponseResult(
    @JsonProperty("resultCode")
    val resultCode: String? = null,

    @JsonProperty("resultMsg")
    val resultMsg: String? = null
)
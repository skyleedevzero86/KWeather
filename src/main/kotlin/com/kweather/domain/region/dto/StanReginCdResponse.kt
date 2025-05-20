package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class StanReginCdResponse(
    @JsonProperty("StanReginCd") val StanReginCd: List<StanReginCd>? = null
)
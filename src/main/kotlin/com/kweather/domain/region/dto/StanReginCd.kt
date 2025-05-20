package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class StanReginCd(
    @JsonProperty("row") val row: List<RegionDto>? = null
)
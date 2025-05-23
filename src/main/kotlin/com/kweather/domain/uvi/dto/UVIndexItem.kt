package com.kweather.domain.uvi.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UVIndexItem(
    val code: String?,
    @JsonProperty("areaNo") val areaNo: String?,
    @JsonProperty("date") val date: String?,
    @JsonProperty("h0") val h0: String?,
    @JsonProperty("h3") val h3: String?,
    @JsonProperty("h6") val h6: String?,
    @JsonProperty("h9") val h9: String?,
    @JsonProperty("h12") val h12: String?,
    @JsonProperty("h15") val h15: String?,
    @JsonProperty("h18") val h18: String?,
    @JsonProperty("h21") val h21: String?,
    @JsonProperty("h24") val h24: String?,
    @JsonProperty("h27") val h27: String?,
    @JsonProperty("h30") val h30: String?,
    @JsonProperty("h33") val h33: String?,
    @JsonProperty("h36") val h36: String?,
    @JsonProperty("h39") val h39: String?,
    @JsonProperty("h42") val h42: String?,
    @JsonProperty("h45") val h45: String?,
    @JsonProperty("h48") val h48: String?,
    @JsonProperty("h51") val h51: String?,
    @JsonProperty("h54") val h54: String?,
    @JsonProperty("h57") val h57: String?,
    @JsonProperty("h60") val h60: String?,
    @JsonProperty("h63") val h63: String?,
    @JsonProperty("h66") val h66: String?,
    @JsonProperty("h69") val h69: String?,
    @JsonProperty("h72") val h72: String?,
    @JsonProperty("h75") val h75: String?
)
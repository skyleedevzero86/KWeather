package com.kweather.domain.airstagnation.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AirStagnationIndexItem(
    val code: String? = null,
    val areaNo: String? = null,
    val date: String? = null,
    val h3: String? = null,
    val h6: String? = null,
    val h9: String? = null,
    val h12: String? = null,
    val h15: String? = null,
    val h18: String? = null,
    val h21: String? = null,
    val h24: String? = null,
    val h27: String? = null,
    val h30: String? = null,
    val h33: String? = null,
    val h36: String? = null,
    val h39: String? = null,
    val h42: String? = null,
    val h45: String? = null,
    val h48: String? = null,
    val h51: String? = null,
    val h54: String? = null,
    val h57: String? = null,
    val h60: String? = null,
    val h63: String? = null,
    val h66: String? = null,
    val h69: String? = null,
    val h72: String? = null,
    val h75: String? = null,
    val h78: String? = null
)
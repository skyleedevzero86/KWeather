package com.kweather.domain.airstagnation.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

@JsonIgnoreProperties(ignoreUnknown = true)
data class AirStagnationIndexBody(
    @JsonDeserialize(using = SingleObjectToListDeserializer::class)
    val items: List<AirStagnationIndexItem>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)
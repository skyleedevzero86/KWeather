package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * API 응답의 주요 내용을 나타내는 클래스입니다.
 * 이 클래스는 `head`와 `row` 필드를 포함하며, 각각 응답의 헤더 정보와 실제 행 데이터를 나타냅니다.
 *
 * @param head API 응답의 헤더 정보
 * @param rows API 응답의 데이터 행 정보
 */
data class ApiResponseContent(
    @JsonProperty("head") val head: List<Map<String, Any>>? = null,
    @JsonProperty("row") val rows: List<ApiResponseRow>? = null
)
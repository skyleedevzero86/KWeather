
package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * API 응답의 헤더 정보를 나타내는 클래스입니다.
 * 이 클래스는 응답의 페이지 정보와 결과 상태를 포함합니다.
 *
 * @param totalCount 전체 데이터 수
 * @param numOfRows 한 페이지에 포함된 데이터 수
 * @param pageNo 현재 페이지 번호
 * @param type 응답 타입 (예: "json")
 * @param result 응답 상태 정보
 */
data class ApiResponseHead(
    @JsonProperty("totalCount") val totalCount: Int? = null,
    @JsonProperty("numOfRows") val numOfRows: Int? = null,
    @JsonProperty("pageNo") val pageNo: Int? = null,
    @JsonProperty("type") val type: String? = null,
    @JsonProperty("RESULT") val result: ApiResponseResult? = null
)
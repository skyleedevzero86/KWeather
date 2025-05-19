package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * API 응답을 나타내는 클래스입니다.
 * 이 클래스는 외부 API의 응답에서 `StanReginCd` 필드를 매핑하며, 이 필드는 `ApiResponseContent` 리스트로 구성됩니다.
 *
 * @param stanReginCd 행정구역 데이터를 포함하는 리스트
 */
data class ApiResponse(
    @JsonProperty("StanReginCd") val stanReginCd: List<ApiResponseContent>
)
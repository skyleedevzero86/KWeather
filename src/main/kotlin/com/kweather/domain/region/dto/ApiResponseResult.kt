package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * API 응답의 결과 상태를 나타내는 클래스입니다.
 * 이 클래스는 API 요청 처리 결과의 상태 코드와 메시지를 포함합니다.
 *
 * @param resultCode 결과 코드 (예: "00"은 성공)
 * @param resultMsg 결과 메시지 (예: "정상 처리")
 */
data class ApiResponseResult(
    @JsonProperty("resultCode") val resultCode: String,
    @JsonProperty("resultMsg") val resultMsg: String
)
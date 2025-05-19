package com.kweather.domain.region.dto

/**
 * 행정구역을 나타내는 DTO 클래스입니다.
 * 이 클래스는 특정 행정구역 코드, 이름, 레벨, 자식 행정구역들을 포함합니다.
 *
 * @param code 행정구역 코드
 * @param name 행정구역 이름
 * @param level 행정구역 레벨 (시/도, 시/군/구, 읍/면/동)
 * @param children 하위 행정구역 (재귀적으로 자식 행정구역을 포함하는 리스트)
 */
data class RegionResponseDto(
    val code: String,
    val name: String,
    val level: Int,
    val children: List<RegionResponseDto> = emptyList()
)
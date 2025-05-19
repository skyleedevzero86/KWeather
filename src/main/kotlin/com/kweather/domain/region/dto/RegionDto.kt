package com.kweather.domain.region.dto

/**
 * 행정구역의 데이터를 나타내는 DTO 클래스입니다.
 * 각 행정구역의 고유한 코드와 이름을 포함합니다.
 *
 * @param regionCd 행정구역 코드
 * @param sidoCd 시/도 코드
 * @param sggCd 시/군/구 코드
 * @param umdCd 읍/면/동 코드
 * @param riCd 리 코드
 * @param locatjuminCd 주소 주민 코드
 * @param locatjijukCd 주소 지적 코드
 * @param locataddNm 주소 이름
 * @param locatOrder 주소 순서
 * @param locatRm 주소 비고
 * @param locathighCd 상위 행정구역 코드
 * @param locallowNm 하위 행정구역 이름
 * @param adptDe 적용일
 */
data class RegionDto(
    val regionCd: String,
    val sidoCd: String,
    val sggCd: String? = null,  // nullable
    val umdCd: String? = null,
    val riCd: String,
    val locatjuminCd: String,
    val locatjijukCd: String,
    val locataddNm: String,
    val locatOrder: Int,
    val locatRm: String?,
    val locathighCd: String,
    val locallowNm: String,
    val adptDe: String?
)

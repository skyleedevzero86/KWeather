package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 행정구역 정보를 포함하는 API 응답의 행(row)을 나타내는 클래스입니다.
 * 이 클래스는 각 행정구역의 세부 정보를 담고 있으며, API 응답의 각 행을 나타냅니다.
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
data class ApiResponseRow(
    @JsonProperty("region_cd") val regionCd: String,
    @JsonProperty("sido_cd") val sidoCd: String,
    @JsonProperty("sgg_cd") val sggCd: String,
    @JsonProperty("umd_cd") val umdCd: String,
    @JsonProperty("ri_cd") val riCd: String,
    @JsonProperty("locatjumin_cd") val locatjuminCd: String,
    @JsonProperty("locatjijuk_cd") val locatjijukCd: String,
    @JsonProperty("locatadd_nm") val locataddNm: String,
    @JsonProperty("locat_order") val locatOrder: Int,
    @JsonProperty("locat_rm") val locatRm: String?,
    @JsonProperty("locathigh_cd") val locathighCd: String,
    @JsonProperty("locallow_nm") val locallowNm: String,
    @JsonProperty("adpt_de") val adptDe: String?
) {
    /**
     * `ApiResponseRow` 객체를 `RegionDto` 객체로 변환합니다.
     *
     * @return 변환된 `RegionDto` 객체
     */
    fun toRegionDto(): RegionDto = RegionDto(
        regionCd = regionCd,
        sidoCd = sidoCd,
        sggCd = sggCd,
        umdCd = umdCd,
        riCd = riCd,
        locatjuminCd = locatjuminCd,
        locatjijukCd = locatjijukCd,
        locataddNm = locataddNm,
        locatOrder = locatOrder,
        locatRm = locatRm,
        locathighCd = locathighCd,
        locallowNm = locallowNm,
        adptDe = adptDe
    )
}
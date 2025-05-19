package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonProperty

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
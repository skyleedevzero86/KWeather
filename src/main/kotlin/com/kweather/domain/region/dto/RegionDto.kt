package com.kweather.domain.region.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class RegionDto(
    @JsonProperty("region_cd") val regionCd: String? = null,
    @JsonProperty("sido_cd") val sidoCd: String? = null,
    @JsonProperty("sgg_cd") val sggCd: String? = null,
    @JsonProperty("umd_cd") val umdCd: String? = null,
    @JsonProperty("ri_cd") val riCd: String? = null,
    @JsonProperty("locatjumin_cd") val locatjuminCd: String? = null,
    @JsonProperty("locatjijuk_cd") val locatjijukCd: String? = null,
    @JsonProperty("locatadd_nm") val locataddNm: String? = null,
    @JsonProperty("locat_order") val locatOrder: Int? = null,
    @JsonProperty("locat_rm") val locatRm: String? = null,
    @JsonProperty("locathigh_cd") val locathighCd: String? = null,
    @JsonProperty("locallow_nm") val locallowNm: String? = null,
    @JsonProperty("adpt_de") val adptDe: String? = null
)




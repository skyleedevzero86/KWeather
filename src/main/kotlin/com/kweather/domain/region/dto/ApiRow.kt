package com.kweather.domain.region.dto

data class ApiRow(
    val regionCd: String = "",
    val sidoCd: String = "",
    val sggCd: String = "",
    val umdCd: String = "",
    val riCd: String = "",
    val locatjuminCd: String = "",
    val locatjijukCd: String = "",
    val locataddNm: String = "",
    val locatOrder: Int = 0,
    val locatRm: String? = null,
    val locathighCd: String = "",
    val locallowNm: String = "",
    val adptDe: String? = null
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
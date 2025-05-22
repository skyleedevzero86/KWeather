package com.kweather.domain.region.entity

import jakarta.persistence.*

@Entity
@Table(name = "regions")
data class Region(
    @Id
    @Column(name = "region_cd", length = 10)
    val regionCd: String,

    @Column(name = "sido_cd", length = 2, nullable = false)
    val sidoCd: String,

    @Column(name = "sgg_cd", length = 3, nullable = false)
    val sggCd: String,

    @Column(name = "umd_cd", length = 3, nullable = false)
    val umdCd: String,

    @Column(name = "ri_cd", length = 2, nullable = false)
    val riCd: String,

    @Column(name = "locatjumin_cd", length = 10)
    val locatjuminCd: String? = null,

    @Column(name = "locatjijuk_cd", length = 10)
    val locatjijukCd: String? = null,

    @Column(name = "locatadd_nm", length = 50)
    val locataddNm: String? = null,

    @Column(name = "locat_order")
    val locatOrder: Int? = null,

    @Column(name = "locat_rm", length = 200)
    val locatRm: String? = null,

    @Column(name = "locathigh_cd", length = 10)
    val locathighCd: String? = null,

    @Column(name = "locallow_nm", length = 20)
    val locallowNm: String? = null,

    @Column(name = "adpt_de", length = 8)
    val adptDe: String? = null
)
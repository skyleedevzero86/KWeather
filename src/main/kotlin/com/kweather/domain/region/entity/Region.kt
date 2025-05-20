package com.kweather.domain.region.entity

import jakarta.persistence.*


@Entity
data class Region(
    @Id
    @Column(name = "region_cd", length = 10)
    val regionCd: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sido_cd", referencedColumnName = "sido_cd")
    val sido: Sido? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sgg_cd", referencedColumnName = "sgg_cd")
    val sgg: Sgg? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "umd_cd", referencedColumnName = "umd_cd")
    val umd: Umd? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ri_cd", referencedColumnName = "ri_cd")
    val ri: Ri? = null,

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

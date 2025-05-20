package com.kweather.domain.region.entity

import jakarta.persistence.*


@Entity
data class Umd(
    @Id
    @Column(name = "umd_cd", length = 3)
    val umdCd: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sgg_cd", referencedColumnName = "sgg_cd")
    val sgg: Sgg? = null
)
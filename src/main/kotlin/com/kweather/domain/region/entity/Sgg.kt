package com.kweather.domain.region.entity

import jakarta.persistence.*

@Entity
data class Sgg(
    @Id
    @Column(name = "sgg_cd", length = 3)
    val sggCd: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sido_cd", referencedColumnName = "sido_cd")
    val sido: Sido? = null
)
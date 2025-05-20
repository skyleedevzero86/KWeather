package com.kweather.domain.region.entity

import jakarta.persistence.*

@Entity
data class Ri(
    @Id
    @Column(name = "ri_cd", length = 2)
    val riCd: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "umd_cd", referencedColumnName = "umd_cd")
    val umd: Umd? = null
)
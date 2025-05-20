package com.kweather.domain.region.entity

import jakarta.persistence.*


@Entity
data class Sido(
    @Id
    @Column(name = "sido_cd", length = 2)
    val sidoCd: String
)
package com.kweather.domain.region.entity

import jakarta.persistence.*

@Entity
@Table(name = "sido")
data class Sido(
    @Id
    @Column(name = "sido_cd", length = 2)
    val sidoCd: String,

    @Column(name = "sido_nm", length = 20)
    val sidoNm: String? = null
) {
    override fun toString(): String = "Sido(sidoCd='$sidoCd')"
    override fun hashCode(): Int = sidoCd.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sido) return false
        return sidoCd == other.sidoCd
    }
}
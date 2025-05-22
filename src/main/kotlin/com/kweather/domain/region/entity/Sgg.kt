package com.kweather.domain.region.entity

import jakarta.persistence.*

@Entity
@Table(name = "sgg")
data class Sgg(
    @Id
    @Column(name = "sgg_cd", length = 3)
    val sggCd: String,

    @Column(name = "sido_cd", length = 2, nullable = false)
    val sidoCd: String,

    @Column(name = "sgg_nm", length = 30)
    val sggNm: String? = null
) {
    override fun toString(): String = "Sgg(sggCd='$sggCd', sidoCd='$sidoCd')"
    override fun hashCode(): Int = sggCd.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Sgg) return false
        return sggCd == other.sggCd
    }
}
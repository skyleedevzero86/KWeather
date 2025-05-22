package com.kweather.domain.region.entity

import jakarta.persistence.*

@Entity
@Table(name = "ri")
data class Ri(
    @Id
    @Column(name = "ri_cd", length = 2)
    val riCd: String,

    @Column(name = "umd_cd", length = 3, nullable = false)
    val umdCd: String,

    @Column(name = "ri_nm", length = 30)
    val riNm: String? = null
) {
    override fun toString(): String = "Ri(riCd='$riCd', umdCd='$umdCd')"
    override fun hashCode(): Int = riCd.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ri) return false
        return riCd == other.riCd
    }
}
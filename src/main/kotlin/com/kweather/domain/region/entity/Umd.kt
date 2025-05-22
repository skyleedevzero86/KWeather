package com.kweather.domain.region.entity

import jakarta.persistence.*

@Entity
@Table(name = "umd")
data class Umd(
    @Id
    @Column(name = "umd_cd", length = 3)
    val umdCd: String,

    @Column(name = "sgg_cd", length = 3, nullable = false)
    val sggCd: String,

    @Column(name = "umd_nm", length = 30)
    val umdNm: String? = null
) {
    override fun toString(): String = "Umd(umdCd='$umdCd', sggCd='$sggCd')"
    override fun hashCode(): Int = umdCd.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Umd) return false
        return umdCd == other.umdCd
    }
}
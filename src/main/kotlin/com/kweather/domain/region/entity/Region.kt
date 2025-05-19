package com.kweather.domain.region.entity

import jakarta.persistence.*

@Entity
@Table(name = "regions")
data class Region(
    @Id
    @Column(name = "region_cd", length = 10)
    val regionCd: String,

    @Column(name = "sido_cd", length = 2, nullable = true)
    val sidoCd: String?,

    @Column(name = "sgg_cd", length = 3, nullable = true)
    val sggCd: String?,

    @Column(name = "umd_cd", length = 3, nullable = true)
    val umdCd: String?,

    @Column(name = "ri_cd", length = 2, nullable = true)
    val riCd: String?,

    @Column(name = "locatjumin_cd", length = 10, nullable = true)
    val locatjuminCd: String?,

    @Column(name = "locatjijuk_cd", length = 10, nullable = true)
    val locatjijukCd: String?,

    @Column(name = "locatadd_nm", length = 50, nullable = true)
    val locataddNm: String?,

    @Column(name = "locat_order")
    val locatOrder: Int,

    @Column(name = "locat_rm", length = 200, nullable = true)
    val locatRm: String?,

    @Column(name = "locathigh_cd", length = 10, nullable = true)
    val locathighCd: String?,

    @Column(name = "locallow_nm", length = 20, nullable = true)
    val locallowNm: String?,

    @Column(name = "adpt_de", length = 8, nullable = true)
    val adptDe: String?,

    // 행정구역 레벨: 1=시/도, 2=시/군/구, 3=읍/면/동
    @Column(name = "level")
    val level: Int
) {
    // 생성자에서 부모-자식 관계 설정을 제거하고 단순화합니다
    // 필요시 별도의 후처리 작업을 통해 관계를 설정할 수 있습니다

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Region
        return regionCd == other.regionCd
    }

    override fun hashCode(): Int {
        return regionCd.hashCode()
    }

    override fun toString(): String {
        return "Region(regionCd='$regionCd', locataddNm='$locataddNm', level=$level)"
    }
}
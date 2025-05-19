package com.kweather.domain.region.entity

import jakarta.persistence.*

@Entity
@Table(name = "regions")
class Region(
    @Id
    @Column(name = "region_cd", length = 10)
    val regionCd: String,

    @Column(name = "sido_cd", length = 2)
    val sidoCd: String,

    @Column(name = "sgg_cd", length = 3)
    val sggCd: String,

    @Column(name = "umd_cd", length = 3)
    val umdCd: String,

    @Column(name = "ri_cd", length = 2)
    val riCd: String,

    @Column(name = "locatjumin_cd", length = 10)
    val locatjuminCd: String,

    @Column(name = "locatjijuk_cd", length = 10)
    val locatjijukCd: String,

    @Column(name = "locatadd_nm", length = 50)
    val locataddNm: String,

    @Column(name = "locat_order")
    val locatOrder: Int,

    @Column(name = "locat_rm", length = 200)
    val locatRm: String?,

    @Column(name = "locathigh_cd", length = 10)
    val locathighCd: String,

    @Column(name = "locallow_nm", length = 20)
    val locallowNm: String,

    @Column(name = "adpt_de", length = 8)
    val adptDe: String?
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locathigh_cd", referencedColumnName = "region_cd", insertable = false, updatable = false)
    var parent: Region? = null

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val children: MutableList<Region> = mutableListOf()

    fun addChild(child: Region) {
        children.add(child)
        child.parent = this
    }

    val level: Int
        get() = when {
            umdCd != "000" -> 3
            sggCd != "000" -> 2
            else -> 1
        }
}
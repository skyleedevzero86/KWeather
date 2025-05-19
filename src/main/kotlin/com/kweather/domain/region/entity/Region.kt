package com.kweather.domain.region.entity

import jakarta.persistence.*

/**
 * 지역 정보를 나타내는 엔티티 클래스입니다.
 * 각 지역은 계층 구조를 가지며, 상위 지역을 참조하거나 하위 지역을 가질 수 있습니다.
 */
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
    /**
     * 상위 지역을 참조하는 Many-to-One 관계입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_cd", referencedColumnName = "region_cd")
    var parent: Region? = null

    /**
     * 하위 지역들을 저장하는 One-to-Many 관계입니다.
     */
    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val children: MutableList<Region> = mutableListOf()

    /**
     * 자식 노드를 추가하는 메서드입니다.
     *
     * @param child 추가할 자식 지역
     */
    fun addChild(child: Region) {
        children.add(child)
        child.parent = this
    }

    /**
     * 지역의 계층 레벨을 계산하는 프로퍼티입니다.
     *
     * @return 계층 레벨 (1: 시/도, 2: 시/군/구, 3: 읍/면/동)
     */
    val level: Int
        get() = when {
            umdCd != "000" -> 3  // 읍/면/동 레벨
            sggCd != "000" -> 2  // 시/군/구 레벨
            else -> 1            // 시/도 레벨
        }
}

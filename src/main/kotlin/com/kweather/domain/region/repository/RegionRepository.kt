package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 지역 정보를 처리하는 데이터베이스 리포지토리 인터페이스입니다.
 * JpaRepository를 확장하여 기본적인 CRUD 기능을 제공합니다.
 */
@Repository
interface RegionRepository : JpaRepository<Region, String> {

    /**
     * 시/도 목록을 조회합니다.
     *
     * @return 시/도 목록
     */
    @Query("SELECT r FROM Region r WHERE r.locathighCd = '0000000000' OR r.locathighCd IS NULL ORDER BY r.locatOrder")
    fun findAllSido(): List<Region>

    /**
     * 특정 시/도에 속한 시/군/구 목록을 조회합니다.
     *
     * @param sidoCode 시/도의 코드
     * @return 시/군/구 목록
     */
    @Query("SELECT r FROM Region r WHERE r.locathighCd = :sidoCode ORDER BY r.locatOrder")
    fun findSggBySido(@Param("sidoCode") sidoCode: String): List<Region>

    /**
     * 특정 시/군/구에 속한 읍/면/동 목록을 조회합니다.
     *
     * @param sggCode 시/군/구의 코드
     * @return 읍/면/동 목록
     */
    @Query("SELECT r FROM Region r WHERE r.locathighCd = :sggCode ORDER BY r.locatOrder")
    fun findDongBySgg(@Param("sggCode") sggCode: String): List<Region>

    /**
     * 코드로 지역을 조회합니다.
     *
     * @param regionCd 지역 코드
     * @return 해당 코드에 맞는 지역
     */
    fun findByRegionCd(regionCd: String): Region?

    /**
     * 저장된 모든 지역 코드를 조회합니다.
     *
     * @return 지역 코드 목록
     */
    @Query("SELECT r.regionCd FROM Region r")
    fun findAllRegionCodes(): List<String>
}
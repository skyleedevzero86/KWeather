package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RegionRepository : JpaRepository<Region, String> {

    @Query("SELECT DISTINCT r.locataddNm FROM Region r WHERE r.locataddNm IS NOT NULL")
    fun findDistinctSidoNames(): List<String?>

    @Query("SELECT DISTINCT r.locataddNm FROM Region r WHERE r.locataddNm LIKE :sido% AND r.locataddNm IS NOT NULL")
    fun findSggsBySido(sido: String): List<String?>

    @Query("SELECT r FROM Region r WHERE r.locataddNm LIKE :fullName% AND r.locataddNm IS NOT NULL AND r.locallowNm IS NOT NULL")
    fun findUmdsBySidoAndSgg(fullName: String): List<Region>

    fun findAllByRegionCdIn(regionCds: List<String>): List<Region>
}
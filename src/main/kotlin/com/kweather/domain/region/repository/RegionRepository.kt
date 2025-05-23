package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RegionRepository : JpaRepository<Region, String> {

    @Query("SELECT r FROM Region r WHERE r.regionCd IN (:regionCodes)")
    fun findAllByRegionCdIn(regionCodes: Set<String>): List<Region>

    fun existsByRegionCd(regionCd: String): Boolean
}
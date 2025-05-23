package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SggRepository : JpaRepository<Sgg, String> {
    @Query("SELECT s FROM Sgg s WHERE s.sggCd IN :sggCodes")
    fun findAllBySggCdIn(@Param("sggCodes") sggCodes: Set<String>): List<Sgg>
}
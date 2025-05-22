package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.Ri
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RiRepository : JpaRepository<Ri, String> {
    @Query("SELECT r FROM Ri r WHERE r.riCd IN :riCodes")
    fun findAllByRiCdIn(@Param("riCodes") riCodes: Set<String>): List<Ri>
}

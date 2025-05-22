package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SidoRepository : JpaRepository<Sido, String> {
    @Query("SELECT s FROM Sido s WHERE s.sidoCd IN :sidoCodes")
    fun findAllBySidoCdIn(@Param("sidoCodes") sidoCodes: Set<String>): List<Sido>
}

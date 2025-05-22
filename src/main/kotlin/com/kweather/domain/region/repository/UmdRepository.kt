package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UmdRepository : JpaRepository<Umd, String> {
    @Query("SELECT u FROM Umd u WHERE u.umdCd IN :umdCodes")
    fun findAllByUmdCdIn(@Param("umdCodes") umdCodes: Set<String>): List<Umd>
}
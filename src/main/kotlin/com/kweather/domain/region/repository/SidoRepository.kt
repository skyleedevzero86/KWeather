package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.Sido
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SidoRepository : JpaRepository<Sido, String>

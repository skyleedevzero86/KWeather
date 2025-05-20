package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.Umd
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UmdRepository : JpaRepository<Umd, String>

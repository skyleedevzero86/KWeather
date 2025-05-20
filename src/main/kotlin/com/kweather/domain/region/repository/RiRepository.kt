package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.Ri
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RiRepository : JpaRepository<Ri, String>
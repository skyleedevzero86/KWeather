package com.kweather.domain.region.repository

import com.kweather.domain.region.entity.Sgg
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface SggRepository : JpaRepository<Sgg, String>
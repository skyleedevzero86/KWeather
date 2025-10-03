package com.kweather.global.common.batch

import com.kweather.domain.region.entity.Region
import com.kweather.domain.region.repository.RegionRepository
import com.kweather.global.common.constants.BatchConstants
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegionWriter(
    private val regionRepository: RegionRepository
) : ItemWriter<Region> {

    private val logger = LoggerFactory.getLogger(RegionWriter::class.java)

    @Transactional
    override fun write(chunk: Chunk<out Region>) {
        try {
            val items = chunk.items.filterNotNull()

            if (items.isNotEmpty()) {
                val existingRegionCodes = regionRepository.findAllByRegionCdIn(
                    items.map { it.regionCd }
                ).map { it.regionCd }.toSet()

                val newRegions = items.filter { it.regionCd !in existingRegionCodes }

                if (newRegions.isNotEmpty()) {
                    regionRepository.saveAll(newRegions)
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
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
            logger.info("저장할 지역 데이터 {} 건", items.size)

            if (items.isNotEmpty()) {
                // 중복 체크 후 저장
                val existingRegionCodes = regionRepository.findAllByRegionCdIn(
                    items.map { it.regionCd }.toSet()
                ).map { it.regionCd }.toSet()

                val newRegions = items.filter { it.regionCd !in existingRegionCodes }

                if (newRegions.isNotEmpty()) {
                    regionRepository.saveAll(newRegions)
                    logger.info(BatchConstants.LogMessages.DATA_SAVE_SUCCESS, newRegions.size)
                } else {
                    logger.info("저장할 신규 데이터가 없습니다")
                }
            }
        } catch (e: Exception) {
            logger.error("${BatchConstants.LogMessages.DATA_SAVE_FAILED}, error=${e.message}", e)
            throw e
        }
    }
}
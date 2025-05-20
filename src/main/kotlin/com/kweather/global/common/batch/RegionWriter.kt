package com.kweather.global.common.batch

import com.kweather.domain.region.entity.Region
import org.springframework.stereotype.Component
import com.kweather.domain.region.repository.*
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.slf4j.LoggerFactory

@Component
class RegionWriter(
    private val regionRepository: RegionRepository
) : ItemWriter<Region> {

    private val logger = LoggerFactory.getLogger(RegionWriter::class.java)

    override fun write(chunk: Chunk<out Region>) {
        try {
            val items = chunk.items
            logger.info("저장할 지역 데이터 ${items.size}개")

            if (items.isNotEmpty()) {
                regionRepository.saveAll(items)
                logger.info("지역 데이터 ${items.size}개 저장 완료")
            }
        } catch (e: Exception) {
            logger.error("지역 데이터 저장 중 오류 발생", e)
            throw e
        }
    }
}
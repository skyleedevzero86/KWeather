package com.kweather.global.common.batch

import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.entity.Region
import com.kweather.domain.region.service.HierarchyService
import com.kweather.global.common.constants.BatchConstants
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class RegionProcessor(
    private val hierarchyService: HierarchyService
) : ItemProcessor<RegionDto, Region?> {

    private val logger = LoggerFactory.getLogger(RegionProcessor::class.java)

    override fun process(item: RegionDto): Region? {
        return try {
            if (!item.isValid()) {
                logger.warn(BatchConstants.LogMessages.INVALID_DATA_SKIP, item)
                return null
            }

            // HierarchyService에서 참조 데이터 확인
            val sido = item.sidoCd?.let { hierarchyService.getSido(it) }
            val sgg = item.sggCd?.let { hierarchyService.getSgg(it) }
            val umd = item.umdCd?.let { hierarchyService.getUmd(it) }
            val ri = item.riCd?.let { hierarchyService.getRi(it) }

            if (sido == null || sgg == null || umd == null || ri == null) {
                logger.warn("참조 데이터 누락, 스킵: regionCd=${item.regionCd}, sido=${item.sidoCd}, sgg=${item.sggCd}, umd=${item.umdCd}, ri=${item.riCd}")
                return null
            }

            Region(
                regionCd = item.regionCd!!,
                sidoCd = item.sidoCd!!,
                sggCd = item.sggCd!!,
                umdCd = item.umdCd!!,
                riCd = item.riCd!!,
                locatjuminCd = item.locatjuminCd,
                locatjijukCd = item.locatjijukCd,
                locataddNm = item.locataddNm,
                locatOrder = item.locatOrder,
                locatRm = item.locatRm,
                locathighCd = item.locathighCd,
                locallowNm = item.locallowNm,
                adptDe = item.adptDe
            )
        } catch (e: Exception) {
            logger.error("${BatchConstants.LogMessages.PROCESSING_ERROR}, regionCd=${item.regionCd}, error=${e.message}", e)
            null
        }
    }
}
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
            logger.error(BatchConstants.LogMessages.PROCESSING_ERROR, item.regionCd, e)
            null
        }
    }
}

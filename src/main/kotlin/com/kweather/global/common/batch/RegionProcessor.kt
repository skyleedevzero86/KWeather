package com.kweather.global.common.batch

import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.entity.Region
import com.kweather.domain.region.entity.Sido
import com.kweather.domain.region.entity.Sgg
import com.kweather.domain.region.entity.Umd
import com.kweather.domain.region.entity.Ri
import com.kweather.domain.region.repository.SidoRepository
import com.kweather.domain.region.repository.SggRepository
import com.kweather.domain.region.repository.UmdRepository
import com.kweather.domain.region.repository.RiRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class RegionProcessor(
    private val sidoRepository: SidoRepository,
    private val sggRepository: SggRepository,
    private val umdRepository: UmdRepository,
    private val riRepository: RiRepository
) : ItemProcessor<RegionDto, Region?> {

    private val logger = LoggerFactory.getLogger(RegionProcessor::class.java)

    override fun process(item: RegionDto): Region? {
        try {
            // Validate required fields
            if (item.sidoCd == null || item.sggCd == null || item.umdCd == null || item.riCd == null || item.regionCd == null) {
                logger.warn("Skipping region item due to missing required fields: {}", item)
                return null
            }

            // 계층 구조 생성
            val sido = sidoRepository.findById(item.sidoCd)
                .orElseGet { sidoRepository.save(Sido(item.sidoCd)) }

            val sgg = sggRepository.findById(item.sggCd).orElseGet {
                val newSgg = Sgg(item.sggCd, sido)
                sggRepository.save(newSgg)
            }

            val umd = umdRepository.findById(item.umdCd).orElseGet {
                val newUmd = Umd(item.umdCd, sgg)
                umdRepository.save(newUmd)
            }

            val ri = riRepository.findById(item.riCd).orElseGet {
                val newRi = Ri(item.riCd, umd)
                riRepository.save(newRi)
            }

            return Region(
                regionCd = item.regionCd,
                sido = sido,
                sgg = sgg,
                umd = umd,
                ri = ri,
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
            logger.error("Error processing region item: regionCd=${item.regionCd}", e)
            return null
        }
    }
}
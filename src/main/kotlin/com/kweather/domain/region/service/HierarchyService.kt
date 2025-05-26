package com.kweather.domain.region.service

import com.kweather.domain.region.entity.*
import com.kweather.domain.region.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class HierarchyService(
    private val sidoRepository: SidoRepository,
    private val sggRepository: SggRepository,
    private val umdRepository: UmdRepository,
    private val riRepository: RiRepository
) {
    private val logger = LoggerFactory.getLogger(HierarchyService::class.java)
    private val sidoCache = mutableMapOf<String, Sido>()
    private val sggCache = mutableMapOf<String, Sgg>()
    private val umdCache = mutableMapOf<String, Umd>()
    private val riCache = mutableMapOf<String, Ri>()

    fun loadHierarchyData(regions: List<com.kweather.domain.region.dto.RegionDto>) {
        logger.info("계층 데이터 로드 시작, 지역 수: ${regions.size}")
        val validRegions = regions.filter { it.isValid() }
        if (validRegions.isEmpty()) {
            logger.warn("유효한 지역 데이터 없음")
            return
        }

        val sidoCodes = validRegions.mapNotNull { it.sidoCd }.toSet()
        val sggCodes = validRegions.mapNotNull { it.sggCd }.toSet()
        val umdCodes = validRegions.mapNotNull { it.umdCd }.toSet()
        val riCodes = validRegions.mapNotNull { it.riCd }.toSet()

        logger.debug("캐시 로드: 시도 {}, 시군구 {}, 읍면동 {}, 리 {}", sidoCodes.size, sggCodes.size, umdCodes.size, riCodes.size)

        // 기존 데이터 로드
        try {
            val sidos = sidoRepository.findAllBySidoCdIn(sidoCodes)
            sidos.forEach { sidoCache[it.sidoCd] = it }
            logger.info("로드된 시도 데이터: ${sidos.map { it.sidoCd }}")

            val sggs = sggRepository.findAllBySggCdIn(sggCodes)
            sggs.forEach { sggCache[it.sggCd] = it }
            logger.info("로드된 시군구 데이터: ${sggs.map { it.sggCd }}")

            val umds = umdRepository.findAllByUmdCdIn(umdCodes)
            umds.forEach { umdCache[it.umdCd] = it }
            logger.info("로드된 읍면동 데이터: ${umds.map { it.umdCd }}")

            val ris = riRepository.findAllByRiCdIn(riCodes)
            ris.forEach { riCache[it.riCd] = it }
            logger.info("로드된 리 데이터: ${ris.map { it.riCd }}")
        } catch (e: Exception) {
            logger.error("데이터베이스에서 기존 데이터 로드 중 오류: ${e.message}", e)
            throw e
        }

        // 누락된 데이터 생성 및 저장
        createMissingHierarchyData(validRegions)
    }

    private fun createMissingHierarchyData(regions: List<com.kweather.domain.region.dto.RegionDto>) {
        val newSidos = mutableListOf<Sido>()
        val newSggs = mutableListOf<Sgg>()
        val newUmds = mutableListOf<Umd>()
        val newRis = mutableListOf<Ri>()

        regions.forEach { region ->
            region.sidoCd?.let { sidoCd ->
                if (!sidoCache.containsKey(sidoCd)) {
                    val sido = Sido(sidoCd)
                    sidoCache[sidoCd] = sido
                    newSidos.add(sido)
                }
            }

            region.sggCd?.let { sggCd ->
                if (!sggCache.containsKey(sggCd) && region.sidoCd != null) {
                    val sgg = Sgg(sggCd, region.sidoCd)
                    sggCache[sggCd] = sgg
                    newSggs.add(sgg)
                }
            }

            region.umdCd?.let { umdCd ->
                if (!umdCache.containsKey(umdCd) && region.sggCd != null) {
                    val umd = Umd(umdCd, region.sggCd)
                    umdCache[umdCd] = umd
                    newUmds.add(umd)
                }
            }

            region.riCd?.let { riCd ->
                if (!riCache.containsKey(riCd) && region.umdCd != null) {
                    val ri = Ri(riCd, region.umdCd)
                    riCache[riCd] = ri
                    newRis.add(ri)
                }
            }
        }

        // 배치로 저장
        try {
            if (newSidos.isNotEmpty()) {
                logger.info("신규 시도 저장: ${newSidos.size}")
                sidoRepository.saveAll(newSidos)
            }
            if (newSggs.isNotEmpty()) {
                logger.info("신규 시군구 저장: ${newSggs.size}")
                sggRepository.saveAll(newSggs)
            }
            if (newUmds.isNotEmpty()) {
                logger.info("신규 읍면동 저장: ${newUmds.size}")
                umdRepository.saveAll(newUmds)
            }
            if (newRis.isNotEmpty()) {
                logger.info("신규 리 저장: ${newRis.size}")
                riRepository.saveAll(newRis)
            }
        } catch (e: Exception) {
            logger.error("데이터베이스 저장 중 오류: ${e.message}", e)
            throw e
        }
    }

    fun getSido(sidoCd: String): Sido? = sidoCache[sidoCd]
    fun getSgg(sggCd: String): Sgg? = sggCache[sggCd]
    fun getUmd(umdCd: String): Umd? = umdCache[umdCd]
    fun getRi(riCd: String): Ri? = riCache[riCd]

    fun clearCache() {
        sidoCache.clear()
        sggCache.clear()
        umdCache.clear()
        riCache.clear()
        logger.info("캐시 정리 완료")
    }
}
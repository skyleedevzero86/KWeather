package com.kweather.domain.region.service

import com.kweather.domain.region.entity.*
import com.kweather.domain.region.repository.*
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
    private val sidoCache = mutableMapOf<String, Sido>()
    private val sggCache = mutableMapOf<String, Sgg>()
    private val umdCache = mutableMapOf<String, Umd>()
    private val riCache = mutableMapOf<String, Ri>()

    fun loadHierarchyData(regions: List<com.kweather.domain.region.dto.RegionDto>) {
        val sidoCodes = regions.mapNotNull { it.sidoCd }.toSet()
        val sggCodes = regions.mapNotNull { it.sggCd }.toSet()
        val umdCodes = regions.mapNotNull { it.umdCd }.toSet()
        val riCodes = regions.mapNotNull { it.riCd }.toSet()

        // 기존 데이터 로드
        sidoRepository.findAllBySidoCdIn(sidoCodes).forEach { sidoCache[it.sidoCd] = it }
        sggRepository.findAllBySggCdIn(sggCodes).forEach { sggCache[it.sggCd] = it }
        umdRepository.findAllByUmdCdIn(umdCodes).forEach { umdCache[it.umdCd] = it }
        riRepository.findAllByRiCdIn(riCodes).forEach { riCache[it.riCd] = it }

        // 누락된 데이터 생성 및 저장
        createMissingHierarchyData(regions)
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
        if (newSidos.isNotEmpty()) sidoRepository.saveAll(newSidos)
        if (newSggs.isNotEmpty()) sggRepository.saveAll(newSggs)
        if (newUmds.isNotEmpty()) umdRepository.saveAll(newUmds)
        if (newRis.isNotEmpty()) riRepository.saveAll(newRis)
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
    }
}

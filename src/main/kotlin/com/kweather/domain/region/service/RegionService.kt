package com.kweather.domain.region.service

import com.kweather.domain.region.repository.RegionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RegionService(private val regionRepository: RegionRepository) {
    private val logger = LoggerFactory.getLogger(RegionService::class.java)

    fun getAllSidos(): List<String> {
        logger.info("모든 시도 조회 시작")
        val sidos = regionRepository.findDistinctSidoNames()
            .mapNotNull { it?.split(" ")?.getOrNull(0) }
            .distinct()
            .sorted()
        logger.info("조회된 시도 목록: $sidos")
        return sidos
    }

    fun getSggsBySido(sidoName: String): List<String> {
        logger.info("시군구 조회 시작 - 시도: $sidoName")
        val normalizedSido = normalizeSido(sidoName)
        val sggs = regionRepository.findSggsBySido(normalizedSido)
            .mapNotNull { it?.split(" ")?.getOrNull(1) }
            .distinct()
            .sorted()
        logger.info("조회된 시군구 목록: $sggs")
        return sggs
    }

    fun getUmdsBySidoAndSgg(sidoName: String, sggName: String): List<String> {
        logger.info("읍면동 조회 시작 - 시도: $sidoName, 시군구: $sggName")
        val normalizedSido = normalizeSido(sidoName)
        val umds = regionRepository.findUmdsBySidoAndSgg("$normalizedSido $sggName")
            .mapNotNull { it.locallowNm }
            .distinct()
            .sorted()
        logger.info("조회된 읍면동 목록: $umds")
        return umds
    }

    private fun normalizeSido(sido: String) = when (sido) {
        "서울" -> "서울특별시"
        "경기" -> "경기도"
        "인천" -> "인천광역시"
        "강원" -> "강원특별자치도"
        "충북" -> "충청북도"
        "충남" -> "충청남도"
        "대전" -> "대전광역시"
        "세종" -> "세종특별자치시"
        "전북" -> "전북특별자치도"
        "전남" -> "전라남도"
        "광주" -> "광주광역시"
        "경북" -> "경상북도"
        "경남" -> "경상남도"
        "대구" -> "대구광역시"
        "부산" -> "부산광역시"
        "울산" -> "울산광역시"
        "제주" -> "제주특별자치도"
        else -> sido
    }
}
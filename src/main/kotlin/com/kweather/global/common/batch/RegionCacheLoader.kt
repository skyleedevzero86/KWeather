package com.kweather.global.common.batch

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.kweather.domain.region.dto.RegionDto
import com.kweather.global.common.constants.BatchConstants
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class RegionCacheLoader(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(RegionCacheLoader::class.java)

    fun loadRegionsFromCache(): List<RegionDto> {
        return try {
            val resource = ClassPathResource(BatchConstants.CACHE_FILE_PATH)
            logger.info("캐시 파일 경로: ${resource.file.absolutePath}")
            if (!resource.exists()) {
                logger.warn("지역 캐시 파일이 존재하지 않습니다: {}", BatchConstants.CACHE_FILE_PATH)
                return emptyList()
            }

            resource.inputStream.use { inputStream ->
                val rawData = inputStream.bufferedReader().use { it.readText() }
                logger.debug("원본 캐시 데이터: $rawData")
                val typeReference = object : TypeReference<List<RegionDto>>() {}
                val regions: List<RegionDto> = objectMapper.readValue(rawData, typeReference)
                val validRegions = regions.filter { it.isValid() }
                if (validRegions.size < regions.size) {
                    logger.warn("유효하지 않은 캐시 데이터: ${regions.filterNot { it.isValid() }}")
                }
                logger.info(BatchConstants.LogMessages.CACHE_LOAD_SUCCESS, validRegions.size)
                logger.debug("캐시 데이터 내용: {}", validRegions)
                validRegions
            }
        } catch (e: IOException) {
            logger.error(BatchConstants.LogMessages.CACHE_LOAD_FAILED, e)
            logger.warn("캐시 로드 실패, 빈 리스트 반환")
            emptyList()
        } catch (e: Exception) {
            logger.error("캐시 데이터 파싱 실패: ${e.message}", e)
            emptyList()
        }
    }
}
package com.kweather.global.common.batch

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.kweather.domain.region.dto.RegionDto
import com.kweather.global.common.constants.BatchConstants
import com.kweather.global.common.exception.CacheLoadException
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
            if (!resource.exists()) {
                logger.warn("지역 캐시 파일이 존재하지 않습니다: {}", BatchConstants.CACHE_FILE_PATH)
                return emptyList()
            }

            resource.inputStream.use { inputStream ->
                val typeReference = object : TypeReference<List<RegionDto>>() {}
                val regions: List<RegionDto> = objectMapper.readValue(inputStream, typeReference)
                logger.info(BatchConstants.LogMessages.CACHE_LOAD_SUCCESS, regions.size)
                regions
            }
        } catch (e: IOException) {
            logger.error(BatchConstants.LogMessages.CACHE_LOAD_FAILED, e)
            throw CacheLoadException("캐시 파일 로드 실패", e)
        }
    }
}

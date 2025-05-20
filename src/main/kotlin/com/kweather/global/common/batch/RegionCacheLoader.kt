package com.kweather.global.common.batch

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.kweather.domain.region.dto.RegionDto
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class RegionCacheLoader(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(RegionCacheLoader::class.java)
    private val cacheFilePath = "region_cache.json"

    fun loadRegionsFromCache(): List<RegionDto> {
        try {
            val resource = ClassPathResource(cacheFilePath)
            if (!resource.exists()) {
                logger.warn("Region cache file does not exist: {}", cacheFilePath)
                return emptyList()
            }

            val inputStream = resource.inputStream

            // TypeReference를 사용하여 명시적으로 타입 지정
            val typeReference = object : TypeReference<List<RegionDto>>() {}
            val regions: List<RegionDto> = objectMapper.readValue(inputStream, typeReference)

            logger.info("Loaded {} regions from cache file", regions.size)
            return regions
        } catch (e: IOException) {
            logger.error("Failed to load regions from cache file", e)
            return emptyList()
        }
    }
}
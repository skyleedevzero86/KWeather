package com.kweather.domain.region.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kweather.domain.region.dto.ApiResponse
import com.kweather.domain.region.dto.RegionDto
import io.github.resilience4j.retry.annotation.Retry

/**
 * 행정구역 관련 데이터를 외부 API에서 가져오는 서비스를 제공하는 클래스입니다.
 * 이 클래스는 외부 API를 호출하여 행정구역 정보를 페이지별로 가져오고,
 * 전체 데이터와 특정 시/도의 데이터를 처리하는 기능을 제공합니다.
 *
 * @constructor [restTemplate] HTTP 요청을 수행하기 위한 RestTemplate 객체
 * @constructor [objectMapper] JSON 응답을 파싱하기 위한 ObjectMapper 객체
 */
@Service
class RegionApiService(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(RegionApiService::class.java)

    @Value("\${api.region.service-key}")
    private lateinit var serviceKey: String

    @Value("\${api.region.base-url}")
    private lateinit var baseUrl: String

    /**
     * 특정 페이지에 해당하는 행정구역 데이터를 가져옵니다.
     * 이 메서드는 API에서 데이터를 가져오고, JSON 응답을 파싱하여
     * [RegionDto] 객체의 리스트로 반환합니다.
     *
     * @param pageNo 데이터를 가져올 페이지 번호
     * @param numOfRows 한 페이지당 가져올 데이터의 수 (기본값은 1000)
     * @return 페이지에 해당하는 행정구역 데이터 리스트
     * @throws Exception API 호출 중 오류가 발생한 경우 예외가 발생할 수 있습니다.
     */
    @Retry(name = "regionApi")
    fun fetchRegionDataByPage(pageNo: Int, numOfRows: Int = 1000): List<RegionDto> {
        try {
            val uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/getStanReginCdList")
                .queryParam("ServiceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("type", "json")
                .queryParam("flag", "Y")
                .build()
                .toUri()

            logger.info("Fetching region data from API: $uri")

            val response = restTemplate.getForObject(uri, String::class.java)
            response?.let {
                val apiResponse = objectMapper.readValue<ApiResponse>(it)
                return apiResponse.stanReginCd
                    .flatMap { content -> content.rows ?: emptyList() }
                    .map { row -> row.toRegionDto() }
            }

            return emptyList()
        } catch (e: Exception) {
            logger.error("Error fetching region data from API: ${e.message}", e)
            throw e
        }
    }

    /**
     * 전체 행정구역 데이터를 가져옵니다.
     * 페이지별로 데이터를 호출하여 전체 데이터를 가져오는 방식입니다.
     *
     * @return 전체 행정구역 데이터를 담은 [RegionDto] 객체 리스트
     * @throws Exception API 호출 중 오류가 발생한 경우 예외가 발생할 수 있습니다.
     */
    fun fetchAllRegionData(): List<RegionDto> {
        val allRegions = mutableListOf<RegionDto>()
        var pageNo = 1
        val numOfRows = 1000 // 한 번에 가져올 데이터 수

        while (true) {
            val pageData = fetchRegionDataByPage(pageNo, numOfRows)
            if (pageData.isEmpty()) {
                break
            }

            allRegions.addAll(pageData)

            // 만약 받은 데이터가 numOfRows보다 적으면 마지막 페이지
            if (pageData.size < numOfRows) {
                break
            }

            pageNo++
        }

        logger.info("Total fetched regions: ${allRegions.size}")
        return allRegions
    }

    /**
     * 특정 시/도에 속한 시/군/구 데이터를 가져옵니다.
     * 주어진 시/도 이름을 사용하여 API에서 해당 시/도의 시/군/구 데이터를 조회합니다.
     *
     * @param sidoName 시/도의 이름
     * @return 해당 시/도에 속한 시/군/구 데이터를 담은 [RegionDto] 객체 리스트
     * @throws Exception API 호출 중 오류가 발생한 경우 예외가 발생할 수 있습니다.
     */
    fun fetchSggBySido(sidoName: String): List<RegionDto> {
        try {
            // 시/도 이름을 URL 인코딩하여 안전하게 전달합니다.
            val encodedSidoName = URLEncoder.encode(sidoName, StandardCharsets.UTF_8.toString())
            val uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/getStanReginCdList")
                .queryParam("ServiceKey", serviceKey)
                .queryParam("type", "json")
                .queryParam("locatadd_nm", encodedSidoName)
                .build()
                .toUri()

            logger.info("Fetching SGG data for sido: $sidoName")

            val response = restTemplate.getForObject(uri, String::class.java)
            response?.let {
                val apiResponse = objectMapper.readValue<ApiResponse>(it)
                return apiResponse.stanReginCd
                    .flatMap { content -> content.rows ?: emptyList() }
                    .map { row -> row.toRegionDto() }
            }

            return emptyList()
        } catch (e: Exception) {
            logger.error("Error fetching SGG data for sido $sidoName: ${e.message}", e)
            throw e
        }
    }
}
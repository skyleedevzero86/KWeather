package com.kweather.domain.region.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import io.github.resilience4j.retry.annotation.Retry
import com.kweather.domain.region.dto.ApiResponse
import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.dto.XmlApiResponse
import java.net.URI

@Service
class RegionApiService(
    private val restTemplate: RestTemplate,
    private val jsonMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(RegionApiService::class.java)
    private val xmlMapper = XmlMapper()

    @Value("\${api.region.service-key}")
    private lateinit var serviceKey: String

    @Value("\${api.region.base-url}")
    private lateinit var baseUrl: String

    @Retry(name = "regionApi")
    fun fetchRegionDataByPage(pageNo: Int, numOfRows: Int = 1000): List<RegionDto> {
        try {
            val uriString = "$baseUrl/getStanReginCdList?ServiceKey=$serviceKey&pageNo=$pageNo" +
                    "&numOfRows=$numOfRows&type=json&flag=Y"
            val uri = URI.create(uriString)
            logger.info("Fetching region data from API: $uri")

            val headers = HttpHeaders().apply {
                accept = listOf(MediaType.APPLICATION_JSON)
                set("Content-Type", "application/json")
            }
            val request = RequestEntity.get(uri)
                .headers(headers)
                .build()

            val response = restTemplate.exchange(request, String::class.java)
            logger.info("API response status: ${response.statusCode}, content-type: ${response.headers.contentType}")

            if (response.statusCodeValue != 200) {
                logger.error("API call failed with status: ${response.statusCode}, body: ${response.body}")
                return emptyList()
            }

            val responseBody = response.body
            if (responseBody.isNullOrEmpty()) {
                logger.warn("Received empty response body from API")
                return emptyList()
            }

            val contentType = response.headers.contentType?.toString() ?: ""
            if (contentType.contains("xml") || responseBody.trim().startsWith("<")) {
                logger.warn("Received XML response, checking for errors")
                try {
                    val xmlResponse = xmlMapper.readValue(responseBody, XmlApiResponse::class.java)
                    val errorMsg = xmlResponse.cmmMsgHeader?.errMsg
                    val returnAuthMsg = xmlResponse.cmmMsgHeader?.returnAuthMsg
                    val returnReasonCode = xmlResponse.cmmMsgHeader?.returnReasonCode

                    logger.error("XML error response: errMsg=$errorMsg, returnAuthMsg=$returnAuthMsg, returnReasonCode=$returnReasonCode")
                    return emptyList()
                } catch (e: Exception) {
                    logger.error("Failed to parse XML error response: ${e.message}")
                    return emptyList()
                }
            }

            try {
                val apiResponse = jsonMapper.readValue(responseBody, ApiResponse::class.java)
                val result = apiResponse.stanReginCd?.flatMap { content ->
                    content.rows?.mapNotNull { row -> row.toRegionDto() } ?: emptyList()
                } ?: emptyList()

                logger.info("Successfully parsed JSON response, found ${result.size} regions")
                return result
            } catch (e: Exception) {
                logger.error("Failed to parse JSON response: ${e.message}", e)
                return emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error fetching region data from API: ${e.message}", e)
            return emptyList()
        }
    }

    fun fetchAllRegionData(): List<RegionDto> {
        val allRegions = mutableListOf<RegionDto>()
        var pageNo = 1
        val numOfRows = 1000

        while (true) {
            try {
                val pageData = fetchRegionDataByPage(pageNo, numOfRows)
                if (pageData.isEmpty()) {
                    logger.warn("No data fetched for page $pageNo, stopping pagination")
                    break
                }
                allRegions.addAll(pageData)
                logger.info("Fetched ${pageData.size} regions for page $pageNo, total: ${allRegions.size}")
                if (pageData.size < numOfRows) {
                    break
                }
                pageNo++
            } catch (e: Exception) {
                logger.warn("Temporary error on page $pageNo, skipping: ${e.message}")
                pageNo++
            }
        }
        logger.info("Total fetched regions: ${allRegions.size}")
        return allRegions
    }
}
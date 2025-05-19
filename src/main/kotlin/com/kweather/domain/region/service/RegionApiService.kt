package com.kweather.domain.region.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.kweather.domain.region.dto.RegionDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.File
import java.nio.file.Files
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class RegionApiService(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${api.region.service-key}")
    private val serviceKey: String,
    @Value("\${api.region.base-url}")
    private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(RegionApiService::class.java)
    private val cacheFile = File("region_cache.json")

    /**
     * 모든 지역 정보를 API에서 가져옵니다.
     */
    fun fetchAllRegionData(): List<RegionDto> {
        // 캐시된 파일이 있으면 먼저 시도
        if (cacheFile.exists()) {
            try {
                val json = Files.readString(cacheFile.toPath())
                val cachedData = objectMapper.readValue(json, Array<RegionDto>::class.java).toList()
                logger.info("Loaded ${cachedData.size} regions from cache file")
                return cachedData
            } catch (e: Exception) {
                logger.warn("Failed to load from cache file: ${e.message}", e)
            }
        }

        // API에서 데이터 조회 시도
        try {
            val allRegions = mutableListOf<RegionDto>()
            var pageNo = 1
            var hasMoreData = true
            val numOfRows = 1000 // 한 번에 더 많은 데이터를 가져오도록 증가

            while (hasMoreData && pageNo <= 30) { // 최대 30페이지로 제한하여 무한 루프 방지
                val pageData = fetchRegionDataPage(pageNo, numOfRows)
                if (pageData.isEmpty()) {
                    hasMoreData = false
                } else {
                    allRegions.addAll(pageData)
                    logger.info("Fetched page $pageNo with ${pageData.size} regions, total so far: ${allRegions.size}")
                    pageNo++
                }
            }

            if (allRegions.isNotEmpty()) {
                saveToCacheFile(allRegions)
                return allRegions
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch data from API: ${e.message}", e)
        }

        // 시도별 더미 데이터 생성 로직 (API 연결 실패 시)
        val dummyData = createDummyRegionData()
        if (dummyData.isNotEmpty()) {
            saveToCacheFile(dummyData)
            return dummyData
        }

        return emptyList()
    }

    /**
     * 특정 페이지의 지역 정보를 API에서 가져옵니다.
     */
    private fun fetchRegionDataPage(pageNo: Int, numOfRows: Int = 100): List<RegionDto> {
        // URL 구성 코드...

        val encodedServiceKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8.toString())

        val url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("ServiceKey", encodedServiceKey)
            .queryParam("pageNo", pageNo)
            .queryParam("numOfRows", numOfRows)
            .queryParam("type", "json")
            .queryParam("flag", "Y")
            .build()
            .toUriString()

        logger.info("Fetching region data from API: $url")

        try {
            logger.info("Sending request to: $url")
            val response: ResponseEntity<String> = restTemplate.getForEntity(url, String::class.java)

            // 응답 헤더, 상태 코드, 본문 로깅
            logger.info("Response status: ${response.statusCode}")
            logger.info("Response headers: ${response.headers}")
            logger.info("Response body length: ${response.body?.length ?: 0}")
            logger.debug("Response body: ${response.body}")

            // 응답 본문이 없으면 빈 리스트 반환
            if (response.body.isNullOrBlank()) {
                logger.warn("Empty response body received for page $pageNo")
                return emptyList()
            }

            return parseJsonResponse(response.body!!)
        } catch (e: Exception) {
            logger.error("Error fetching region data: ${e.message}", e)
            // 스택 트레이스 출력으로 상세 오류 확인
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * JSON 응답을 파싱합니다.
     * 실제 API 응답 형식인 StanReginCd > head/row 구조에 맞게 수정
     */
    private fun parseJsonResponse(jsonString: String): List<RegionDto> {
        try {
            // 원본 응답 JSON 로깅
            logger.debug("Raw JSON response: $jsonString")

            val jsonNode = objectMapper.readTree(jsonString)
            logger.debug("JSON node structure: ${jsonNode.toPrettyString()}")

            // StanReginCd 배열이 있는지 확인
            val stanReginCdNode = jsonNode.path("StanReginCd")
            if (stanReginCdNode.isMissingNode() || !stanReginCdNode.isArray()) {
                logger.warn("StanReginCd node is missing or not an array")
                return emptyList()
            }

            // 배열의 두 번째 요소에서 row 값을 찾는다
            val rowsNode = if (stanReginCdNode.size() > 1) {
                stanReginCdNode.get(1).path("row")
            } else {
                logger.warn("StanReginCd array does not have second element")
                return emptyList()
            }

            if (rowsNode.isMissingNode() || !rowsNode.isArray()) {
                logger.warn("row node is missing or not an array")
                return emptyList()
            }

            // row 배열의 각 항목을 RegionDto로 변환
            val regions = mutableListOf<RegionDto>()
            rowsNode.forEach { rowNode ->
                try {
                    val regionCd = rowNode.path("region_cd").asText("")
                    if (regionCd.isNotEmpty()) {
                        val regionDto = RegionDto(
                            regionCd = regionCd,
                            sidoCd = rowNode.path("sido_cd").asText(""),
                            sggCd = rowNode.path("sgg_cd").asText(""),
                            umdCd = rowNode.path("umd_cd").asText(""),
                            riCd = rowNode.path("ri_cd").asText(""),
                            locatjuminCd = rowNode.path("locatjumin_cd").asText(""),
                            locatjijukCd = rowNode.path("locatjijuk_cd").asText(""),
                            locataddNm = rowNode.path("locatadd_nm").asText(""),
                            locatOrder = rowNode.path("locat_order").asInt(0),
                            locatRm = rowNode.path("locat_rm").asText(""),
                            locathighCd = rowNode.path("locathigh_cd").asText(""),
                            locallowNm = rowNode.path("locallow_nm").asText(""),
                            adptDe = rowNode.path("adpt_de").asText(""),
                            level = determineLevel(regionCd)
                        )
                        regions.add(regionDto)
                    }
                } catch (e: Exception) {
                    logger.error("Error parsing row node: ${e.message}", e)
                }
            }

            logger.info("Successfully parsed ${regions.size} regions from JSON")
            return regions
        } catch (e: Exception) {
            logger.error("Error parsing JSON response: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * 지역 코드로 레벨을 결정합니다.
     */
    private fun determineLevel(regionCd: String): Int {
        return when {
            // 시/도 레벨 (XX00000000)
            regionCd.endsWith("00000000") -> 1
            // 시/군/구 레벨 (XXYYY00000)
            regionCd.endsWith("00000") -> 2
            // 읍/면/동 레벨 (XXYYYZZ000)
            else -> 3
        }
    }

    /**
     * API 연결 실패 시 기본적인 시/도 정보를 제공하기 위한 더미 데이터 생성
     */
    private fun createDummyRegionData(): List<RegionDto> {
        logger.info("Creating dummy region data as API connection failed")
        val sidoCodes = listOf(
            "1100000000" to "서울특별시",
            "2600000000" to "부산광역시",
            "2700000000" to "대구광역시",
            "2800000000" to "인천광역시",
            "2900000000" to "광주광역시",
            "3000000000" to "대전광역시",
            "3100000000" to "울산광역시",
            "3600000000" to "세종특별자치시",
            "4100000000" to "경기도",
            "4200000000" to "강원특별자치도",
            "4300000000" to "충청북도",
            "4400000000" to "충청남도",
            "4500000000" to "전라북도",
            "4600000000" to "전라남도",
            "4700000000" to "경상북도",
            "4800000000" to "경상남도",
            "5000000000" to "제주특별자치도"
        )

        return sidoCodes.mapIndexed { index, (code, name) ->
            RegionDto(
                regionCd = code,
                sidoCd = code.substring(0, 2),
                sggCd = null,
                umdCd = null,
                riCd = null,
                locatjuminCd = code,
                locatjijukCd = code,
                locataddNm = name,
                locatOrder = index + 1,
                locatRm = "기본 제공 시/도 데이터",
                locathighCd = null,
                locallowNm = name,
                adptDe = "20250519",
                level = 1
            )
        }
    }

    /**
     * 지역 데이터를 캐시 파일에 저장합니다.
     */
    private fun saveToCacheFile(regions: List<RegionDto>) {
        try {
            val json = objectMapper.writeValueAsString(regions)
            Files.writeString(cacheFile.toPath(), json)
            logger.info("Saved ${regions.size} regions to cache file")
        } catch (e: Exception) {
            logger.error("Failed to save regions to cache file: ${e.message}", e)
        }
    }
}
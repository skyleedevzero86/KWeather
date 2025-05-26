package com.kweather.domain.region.controller

import com.kweather.domain.region.service.RegionService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RegionRestController(private val regionService: RegionService) {
    private val logger = LoggerFactory.getLogger(RegionRestController::class.java)

    @GetMapping("/api/regions/sidos")
    fun getSidos(): List<String> {
        logger.info("모든 시/도 목록 조회 요청")
        return regionService.getAllSidos().also { logger.info("조회된 시/도 목록: $it") }
    }

    @GetMapping("/api/regions/sggs")
    fun getSggs(@RequestParam("sido") sido: String): List<String> {
        logger.info("시/군/구 목록 조회 요청 - 시도: $sido")
        return regionService.getSggsBySido(sido).also { logger.info("조회된 시/군/구 목록: $it") }
    }

    @GetMapping("/api/regions/umds")
    fun getUmds(@RequestParam("sido") sido: String, @RequestParam("sgg") sgg: String): List<String> {
        logger.info("읍/면/동 목록 조회 요청 - 시도: $sido, 시/군/구: $sgg")
        return regionService.getUmdsBySidoAndSgg(sido, sgg).also { logger.info("조회된 읍/면/동 목록: $it") }
    }
}
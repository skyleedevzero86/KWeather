package com.kweather.domain.region.controller

import com.kweather.domain.region.dto.RegionResponseDto
import com.kweather.domain.region.service.RegionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.function.Supplier

/**
 * 지역 정보를 처리하는 REST API 엔드포인트를 제공합니다.
 * 이 클래스는 행정구역 데이터를 조회하는 여러 메서드를 제공합니다.
 */
@RestController
@RequestMapping("/api/regions")
class RegionController(private val regionService: RegionService) {

    /**
     * 지역 데이터를 실행할 수 있도록 돕는 유틸리티 메서드입니다.
     * 이 메서드는 예외가 발생하지 않으면 정상적으로 결과를 반환하고,
     * 예외가 발생하면 500 내부 서버 오류를 반환합니다.
     *
     * @param supplier 실행할 쿼리를 제공하는 Supplier 객체
     * @return 쿼리 실행 결과를 담은 ResponseEntity 객체
     */
    private fun <T> executeQuery(supplier: Supplier<T>): ResponseEntity<T> {
        return try {
            ResponseEntity.ok(supplier.get())
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * 시/도 목록을 조회합니다.
     *
     * @return 시/도 목록을 담은 ResponseEntity 객체
     */
    @GetMapping("/sido")
    fun getSidoList(): ResponseEntity<List<RegionResponseDto>> {
        return executeQuery(Supplier { regionService.getSidoList() })
    }

    /**
     * 특정 시/도에 속한 시/군/구 목록을 조회합니다.
     *
     * @param sidoCode 시/도의 코드
     * @return 해당 시/도에 속한 시/군/구 목록을 담은 ResponseEntity 객체
     */
    @GetMapping("/sido/{sidoCode}/sgg")
    fun getSggList(@PathVariable sidoCode: String): ResponseEntity<List<RegionResponseDto>> {
        return executeQuery(Supplier { regionService.getSggBySido(sidoCode) })
    }

    /**
     * 특정 시/군/구에 속한 읍/면/동 목록을 조회합니다.
     *
     * @param sidoCode 시/도의 코드
     * @param sggCode 시/군/구의 코드
     * @return 해당 시/군/구에 속한 읍/면/동 목록을 담은 ResponseEntity 객체
     */
    @GetMapping("/sido/{sidoCode}/sgg/{sggCode}/dong")
    fun getDongList(
        @PathVariable sidoCode: String,
        @PathVariable sggCode: String
    ): ResponseEntity<List<RegionResponseDto>> {
        return executeQuery(Supplier { regionService.getDongBySgg(sggCode) })
    }

    /**
     * 전체 행정구역 계층 구조를 조회합니다.
     *
     * @return 전체 행정구역 계층을 담은 ResponseEntity 객체
     */
    @GetMapping("/hierarchy")
    fun getRegionHierarchy(): ResponseEntity<List<RegionResponseDto>> {
        return executeQuery(Supplier { regionService.getRegionHierarchy() })
    }
}

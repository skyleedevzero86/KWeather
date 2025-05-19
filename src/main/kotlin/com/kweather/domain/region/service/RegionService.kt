package com.kweather.domain.region.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.kweather.domain.region.dto.*
import com.kweather.domain.region.repository.RegionRepository
import com.kweather.domain.region.entity.Region

/**
 * 지역 정보를 제공하는 서비스 클래스입니다.
 * 시/도, 시/군/구, 읍/면/동 목록을 제공하고, 지역 계층 구조를 처리합니다.
 */
@Service
class RegionService(private val regionRepository: RegionRepository) {

    /**
     * 시/도 목록을 조회합니다.
     *
     * @return 시/도 목록
     */
    @Transactional(readOnly = true)
    fun getSidoList(): List<RegionResponseDto> {
        val sidoList = regionRepository.findAllSido()
        return sidoList.map { toResponseDto(it) }
    }

    /**
     * 특정 시/도에 속한 시/군/구 목록을 조회합니다.
     *
     * @param sidoCode 시/도의 코드
     * @return 해당 시/도에 속한 시/군/구 목록
     */
    @Transactional(readOnly = true)
    fun getSggBySido(sidoCode: String): List<RegionResponseDto> {
        val sggList = regionRepository.findSggBySido(sidoCode)
        return sggList.map { toResponseDto(it) }
    }

    /**
     * 특정 시/군/구에 속한 읍/면/동 목록을 조회합니다.
     *
     * @param sggCode 시/군/구 코드
     * @return 해당 시/군/구에 속한 읍/면/동 목록
     */
    @Transactional(readOnly = true)
    fun getDongBySgg(sggCode: String): List<RegionResponseDto> {
        val dongList = regionRepository.findDongBySgg(sggCode)
        return dongList.map { toResponseDto(it) }
    }

    /**
     * 엔티티를 응답 DTO로 변환합니다.
     *
     * @param region 변환할 지역 엔티티
     * @return 변환된 RegionResponseDto 객체
     */
    private fun toResponseDto(region: Region): RegionResponseDto {
        return RegionResponseDto(
            code = region.regionCd,
            name = region.locallowNm,
            level = region.level,
            children = emptyList()
        )
    }

    /**
     * 전체 행정구역 계층 구조를 조회합니다.
     *
     * @return 전체 계층 구조를 담은 리스트
     */
    @Transactional(readOnly = true)
    fun getRegionHierarchy(): List<RegionResponseDto> {
        val sidoList = regionRepository.findAllSido()
        return sidoList.map { sido ->
            RegionResponseDto(
                code = sido.regionCd,
                name = sido.locallowNm,
                level = sido.level,
                children = getChildrenRecursive(sido.regionCd)
            )
        }
    }

    /**
     * 재귀적으로 자식 지역을 가져옵니다.
     *
     * @param parentCode 부모 지역 코드
     * @return 자식 지역들의 리스트
     */
    private fun getChildrenRecursive(parentCode: String): List<RegionResponseDto> {
        val children = when {
            parentCode.endsWith("000000") -> regionRepository.findSggBySido(parentCode)
            parentCode.endsWith("000") -> regionRepository.findDongBySgg(parentCode)
            else -> emptyList()
        }

        return children.map { child ->
            RegionResponseDto(
                code = child.regionCd,
                name = child.locallowNm,
                level = child.level,
                children = getChildrenRecursive(child.regionCd)
            )
        }
    }

    /**
     * 특정 레벨과 상위 코드에 따른 하위 지역 목록을 조회합니다.
     *
     * @param parentCode 상위 지역 코드 (null이면 최상위 시/도 조회)
     * @return 하위 지역 목록
     */
    @Transactional(readOnly = true)
    fun getRegionsByParent(parentCode: String?): List<RegionResponseDto> {
        val regions = if (parentCode.isNullOrEmpty()) {
            regionRepository.findAllSido()
        } else {
            regionRepository.findByRegionCd(parentCode)?.children ?: emptyList()
        }
        return regions.map { toResponseDto(it) }
    }


}

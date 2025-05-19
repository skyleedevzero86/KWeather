package com.kweather.batch.config

import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.entity.Region
import com.kweather.domain.region.repository.RegionRepository
import com.kweather.domain.region.service.RegionApiService
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.support.ListItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

/**
 * Batch 작업을 위한 설정 클래스입니다. 이 클래스는 지역 데이터를 처리하는 배치 작업을 정의합니다.
 * 지역 데이터를 외부 API로부터 읽어온 후, 이를 데이터베이스에 저장합니다.
 */
@Configuration
@EnableBatchProcessing
class RegionBatchConfig(
    private val regionApiService: RegionApiService,
    private val regionRepository: RegionRepository
) {

    /**
     * 배치 작업을 정의하는 메서드입니다. 이 메서드는 Job을 생성하고 반환합니다.
     *
     * @param jobRepository 배치 작업의 메타데이터를 저장하는 저장소
     * @param step 작업을 구성하는 단위 단계
     * @return Job 객체
     */
    @Bean
    fun regionJob(jobRepository: JobRepository, step: Step): Job {
        return JobBuilder("regionJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(step)
            .build()
    }

    /**
     * 배치 작업의 단계를 정의하는 메서드입니다. 이 메서드는 Step을 생성하고 반환합니다.
     *
     * @param jobRepository 배치 작업의 메타데이터를 저장하는 저장소
     * @param transactionManager 트랜잭션 관리
     * @return Step 객체
     */
    @Bean
    fun regionStep(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Step {
        return StepBuilder("regionStep", jobRepository)
            .chunk<RegionDto, Region>(100, transactionManager) // 입력: RegionDto, 출력: Region
            .reader(regionItemReader())
            .processor(regionItemProcessor())
            .writer(regionItemWriter())
            .build()
    }

    /**
     * 외부 API로부터 지역 데이터를 읽어오는 ItemReader를 정의하는 메서드입니다.
     *
     * @return ItemReader<RegionDto> 지역 데이터 읽기
     */
    @Bean
    fun regionItemReader(): ItemReader<RegionDto> {
        val allData = regionApiService.fetchAllRegionData()
        return ListItemReader(allData)
    }

    /**
     * 읽어온 지역 데이터를 처리하여 Region 엔티티로 변환하는 ItemProcessor를 정의하는 메서드입니다.
     *
     * @return ItemProcessor<RegionDto, Region> 데이터 처리
     */
    @Bean
    fun regionItemProcessor(): ItemProcessor<RegionDto, Region> {
        return ItemProcessor { dto ->
            // RegionDto에서 Region 엔티티로 변환
            Region(
                regionCd = dto.regionCd,
                sidoCd = dto.sidoCd,
                sggCd = dto.sggCd,
                umdCd = dto.umdCd,
                riCd = dto.riCd,
                locatjuminCd = dto.locatjuminCd,
                locatjijukCd = dto.locatjijukCd,
                locataddNm = dto.locataddNm,
                locatOrder = dto.locatOrder,
                locatRm = dto.locatRm,
                locathighCd = dto.locathighCd,
                locallowNm = dto.locallowNm,
                adptDe = dto.adptDe
            ).apply {
                // 상위 지역 설정
                val parentCode = dto.locathighCd
                if (parentCode != "0000000000" && parentCode.isNotEmpty()) {
                    val parent = findParentRegion(parentCode)
                    parent?.let { this.parent = it }
                }
            }
        }
    }

    /**
     * 변환된 지역 데이터를 데이터베이스에 저장하는 ItemWriter를 정의하는 메서드입니다.
     *
     * @return ItemWriter<Region> 데이터베이스 저장
     */
    @Bean
    fun regionItemWriter(): ItemWriter<Region> {
        return ItemWriter { regions ->
            regions.forEach { region ->
                // 데이터베이스에 저장 (JPA를 통해 자동 저장)
                regionRepository.save(region)
                // 상위-하위 관계 반영
                val parentCode = region.locathighCd
                if (parentCode != "0000000000" && parentCode.isNotEmpty()) {
                    val parent = regionRepository.findByRegionCd(parentCode)
                    parent?.addChild(region)
                    regionRepository.save(parent ?: region)
                }
            }
        }
    }

    /**
     * 주어진 부모 코드에 해당하는 상위 지역을 조회하는 메서드입니다.
     *
     * @param parentCode 상위 지역 코드
     * @return 해당하는 상위 지역 객체 또는 null
     */
    private fun findParentRegion(parentCode: String): Region? {
        return if (parentCode.isNotEmpty()) {
            regionRepository.findByRegionCd(parentCode)
        } else {
            null
        }
    }
}

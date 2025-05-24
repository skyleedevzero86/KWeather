package com.kweather.global.config

import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.entity.Region
import com.kweather.global.common.batch.RegionProcessor
import com.kweather.global.common.batch.RegionReader
import com.kweather.global.common.batch.RegionWriter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.client.RestTemplate

/**
 * KotlinWeather 애플리케이션의 Spring Batch 작업을 구성하는 설정 클래스
 *
 * 이 클래스는 지역 정보 가져오기(Import Region) 배치 작업을 정의합니다.
 * 외부 데이터 소스에서 지역 정보를 읽어와 시스템 데이터베이스에 저장하는
 * ETL(Extract, Transform, Load) 프로세스를 담당합니다.
 *
 * ## 배치 작업 플로우
 * 1. **Extract**: RegionReader를 통해 외부 소스에서 지역 데이터 읽기
 * 2. **Transform**: RegionProcessor를 통해 데이터 변환 및 검증
 * 3. **Load**: RegionWriter를 통해 데이터베이스에 저장
 *
 * ## 주요 특징
 * - 청크 기반 처리로 메모리 효율성 확보
 * - 장애 허용(Fault Tolerance) 기능으로 안정성 보장
 * - 트랜잭션 관리를 통한 데이터 무결성 유지
 *
 * @author kylee (궁금하면 500원)
 * @version 1.0
 * @since 2025-05-24
 *
 * @see RegionReader 지역 데이터 읽기 컴포넌트
 * @see RegionProcessor 지역 데이터 변환 컴포넌트
 * @see RegionWriter 지역 데이터 저장 컴포넌트
 */
@Configuration
@Tag(name = "Batch Configuration", description = "Spring Batch 작업 구성")
class BatchConfig(
    /**
     * Spring Batch 작업 실행 이력을 관리하는 JobRepository
     * 작업 상태, 실행 시간, 처리 건수 등의 메타데이터를 저장
     */
    private val jobRepository: JobRepository,

    /**
     * 배치 작업의 트랜잭션을 관리하는 트랜잭션 매니저
     * 청크 단위로 트랜잭션을 커밋/롤백하여 데이터 일관성 보장
     */
    private val transactionManager: PlatformTransactionManager,

    /**
     * 외부 소스에서 지역 데이터를 읽어오는 ItemReader 구현체
     * CSV, JSON, API 등 다양한 데이터 소스를 지원
     */
    private val regionReader: RegionReader,

    /**
     * 읽어온 지역 데이터를 비즈니스 로직에 맞게 변환하는 ItemProcessor 구현체
     * 데이터 검증, 형식 변환, 중복 체크 등의 처리를 담당
     */
    private val regionProcessor: RegionProcessor,

    /**
     * 변환된 지역 데이터를 데이터베이스에 저장하는 ItemWriter 구현체
     * 배치 삽입(Batch Insert)을 통해 성능 최적화
     */
    private val regionWriter: RegionWriter
) {

    /**
     * 지역 정보 가져오기 Job을 정의합니다.
     *
     * 이 Job은 외부 데이터 소스에서 지역 정보를 읽어와
     * 시스템 데이터베이스에 저장하는 전체 워크플로우를 관리합니다.
     *
     * ## Job 구성
     * - **Job명**: importRegionJob
     * - **Step 구성**: importRegionStep 단일 스텝으로 구성
     * - **실행 방식**: 단일 스레드 순차 실행
     *
     * ## 실행 시나리오
     * 1. 스케줄러 또는 수동 트리거에 의해 실행
     * 2. importRegionStep 스텝 실행
     * 3. 성공/실패 결과를 JobRepository에 기록
     *
     * @param importRegionStep 지역 데이터 처리를 담당하는 Step
     * @return 구성된 Job 인스턴스
     *
     * @see JobBuilder Spring Batch Job 빌더
     * @see Step 배치 작업의 단위 처리 단계
     */
    @Bean
    fun importRegionJob(importRegionStep: Step): Job {
        return JobBuilder("importRegionJob", jobRepository)
            .start(importRegionStep)
            .build()
    }

    /**
     * 지역 정보 가져오기 Step을 정의합니다.
     *
     * 이 Step은 청크 기반 처리 방식을 사용하여 대용량 데이터를
     * 효율적으로 처리합니다. 메모리 사용량을 최적화하고
     * 트랜잭션 범위를 제한하여 안정성을 보장합니다.
     *
     * ## 청크 처리 설정
     * - **청크 크기**: 100개 (한 번에 100개 레코드씩 처리)
     * - **입력 타입**: RegionDto (외부에서 읽어온 지역 데이터 DTO)
     * - **출력 타입**: Region? (변환된 지역 엔티티, nullable)
     *
     * ## 처리 플로우
     * 1. **Read**: regionReader에서 RegionDto 100개씩 읽기
     * 2. **Process**: regionProcessor에서 각 RegionDto를 Region으로 변환
     * 3. **Write**: regionWriter에서 변환된 Region 리스트를 데이터베이스에 저장
     * 4. **Commit**: 트랜잭션 커밋 (청크 단위)
     *
     * ## 장애 허용(Fault Tolerance) 설정
     * - **Skip 대상**: 모든 Exception 타입
     * - **Skip 한계**: 최대 100개 레코드까지 건너뛰기 허용
     * - **동작 방식**: 개별 레코드 처리 실패 시 해당 레코드만 건너뛰고 계속 진행
     *
     * ## 성능 고려사항
     * - 청크 크기 100은 메모리 사용량과 처리 성능의 균형점
     * - 너무 크면 메모리 부족, 너무 작으면 트랜잭션 오버헤드 증가
     * - 데이터 특성에 따라 조정 가능
     *
     * ## 에러 처리 전략
     * - **Retry**: 현재 미설정 (필요시 추가 가능)
     * - **Skip**: Exception 발생 시 해당 레코드 건너뛰기
     * - **Restart**: Job 재시작 시 실패 지점부터 재개 (JobRepository 관리)
     *
     * @return 구성된 Step 인스턴스
     *
     * @see StepBuilder Spring Batch Step 빌더
     * @see RegionDto 외부 데이터 소스의 지역 정보 DTO
     * @see Region 시스템 내부의 지역 정보 엔티티
     */
    @Bean
    fun importRegionStep(): Step {
        return StepBuilder("importRegionStep", jobRepository)
            .chunk<RegionDto, Region?>(100, transactionManager)
            .reader(regionReader.reader())
            .processor(regionProcessor)
            .writer(regionWriter)
            .faultTolerant()
            .skip(Exception::class.java)
            .skipLimit(100)
            .build()
    }
}
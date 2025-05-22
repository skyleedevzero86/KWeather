package com.kweather.global.common.batch

import com.kweather.domain.region.service.HierarchyService
import org.slf4j.LoggerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.util.Date

@Component
class RegionBatchRunner(
    private val jobLauncher: JobLauncher,
    @Qualifier("importRegionJob") private val importRegionJob: Job,
    @Value("\${region.batch.startup-execution:false}") private val startupExecution: Boolean,
    private val hierarchyService: HierarchyService
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(RegionBatchRunner::class.java)

    override fun run(args: ApplicationArguments) {
        if (startupExecution) {
            try {
                logger.info("애플리케이션 시작 시 지역 데이터 가져오기 작업 시작")

                val jobParameters = JobParametersBuilder()
                    .addDate("시작시간", Date())
                    .addString("실행모드", "시작시실행")
                    .toJobParameters()

                val jobExecution = jobLauncher.run(importRegionJob, jobParameters)

                logger.info("지역 데이터 가져오기 작업 완료, 상태: {}", jobExecution.status)

                if (jobExecution.status.isUnsuccessful) {
                    logger.error("지역 데이터 가져오기 작업이 실패했습니다")
                }
            } catch (e: Exception) {
                logger.error("지역 데이터 가져오기 작업 실행 중 오류 발생", e)
            } finally {
                // 캐시 정리
                hierarchyService.clearCache()
            }
        } else {
            logger.info("시작 시 지역 데이터 가져오기가 비활성화되어 있습니다")
        }
    }
}
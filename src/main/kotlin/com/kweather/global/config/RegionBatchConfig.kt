package com.kweather.batch.config

import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.entity.Region
import com.kweather.domain.region.repository.RegionRepository
import com.kweather.domain.region.service.RegionApiService
import org.springframework.batch.core.*
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.slf4j.LoggerFactory
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.boot.CommandLineRunner
import org.springframework.dao.DataIntegrityViolationException

@Configuration
@EnableBatchProcessing
class RegionBatchConfig(
    private val regionApiService: RegionApiService,
    private val regionRepository: RegionRepository,
    private val jobLauncher: JobLauncher
) {
    private val logger = LoggerFactory.getLogger(RegionBatchConfig::class.java)

    @Bean
    fun regionJob(jobRepository: JobRepository, step: Step): Job {
        return JobBuilder("regionJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(step)
            .listener(object : JobExecutionListener {
                override fun beforeJob(jobExecution: JobExecution) {
                    logger.info("Starting region import job")
                }

                override fun afterJob(jobExecution: JobExecution) {
                    logger.info("Job completed with status: ${jobExecution.status}")
                    if (jobExecution.status == BatchStatus.FAILED) {
                        jobExecution.allFailureExceptions.forEach { e ->
                            logger.error("Job failure", e)
                        }
                    }
                }
            })
            .build()
    }

    @Bean
    fun regionStep(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Step {
        return StepBuilder("regionStep", jobRepository)
            .chunk<RegionDto, Region>(50, transactionManager) // 청크 크기를 더 작게 설정
            .reader(regionItemReader())
            .processor(regionItemProcessor())
            .writer(regionItemWriter())
            .faultTolerant()
            .retryLimit(3)
            .retry(Exception::class.java)
            .skip(DataIntegrityViolationException::class.java)
            .skipLimit(100)
            .listener(object : SkipListener<RegionDto, Region> {
                override fun onSkipInWrite(item: Region, t: Throwable) {
                    logger.warn("Skipped region ${item.regionCd} due to: ${t.message}")
                }
                override fun onSkipInRead(t: Throwable) {
                    logger.warn("Skipped reading due to: ${t.message}")
                }
                override fun onSkipInProcess(item: RegionDto, t: Throwable) {
                    logger.warn("Skipped processing ${item.regionCd} due to: ${t.message}")
                }
            })
            .listener(object : StepExecutionListener {
                override fun beforeStep(stepExecution: StepExecution) {
                    logger.info("Clearing existing regions")
                    try {
                        // 기존 데이터 삭제 시도
                        val count = regionRepository.count()
                        if (count > 0) {
                            regionRepository.deleteAllInBatch()
                            logger.info("Successfully cleared $count existing regions")
                        } else {
                            logger.info("No existing regions to clear")
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to clear existing regions: ${e.message}", e)
                        // 기존 데이터 삭제 실패 시 처리할 내용을 추가
                        // 배치 작업을 계속 진행하므로 예외를 다시 던지지 않음
                    }
                }

                override fun afterStep(stepExecution: StepExecution): ExitStatus? {
                    logger.info("Step completed with status: ${stepExecution.status}")
                    logger.info("Read count: ${stepExecution.readCount}, Write count: ${stepExecution.writeCount}, Skip count: ${stepExecution.skipCount}")
                    return null
                }
            })
            .build()
    }

    @Bean
    fun regionItemReader(): ItemReader<RegionDto> {
        return object : ItemReader<RegionDto> {
            private var regions: List<RegionDto> = emptyList()
            private var index = 0

            init {
                try {
                    logger.info("Initializing region item reader")
                    val apiRegions = regionApiService.fetchAllRegionData()
                    regions = apiRegions.sortedBy { region -> region.locatOrder ?: Int.MAX_VALUE }
                    logger.info("Loaded ${regions.size} regions for reading")
                } catch (e: Exception) {
                    logger.error("Failed to load regions: ${e.message}", e)
                    regions = emptyList()
                }
            }

            override fun read(): RegionDto? {
                if (index < regions.size) {
                    if (index % 500 == 0) {
                        logger.info("Reading progress: $index/${regions.size}")
                    }
                    val region = regions[index]
                    index++
                    return region
                }
                return null
            }
        }
    }

    @Bean
    fun regionItemProcessor(): ItemProcessor<RegionDto, Region> {
        return ItemProcessor { dto ->
            // 일관된 레벨 결정 로직 적용
            val level = when {
                dto.regionCd.endsWith("00000000") -> 1 // 시/도
                dto.regionCd.endsWith("00000") -> 2 // 시/군/구
                else -> 3 // 읍/면/동 또는 기타
            }

            val region = Region(
                regionCd = dto.regionCd,
                sidoCd = dto.sidoCd ?: "",
                sggCd = dto.sggCd ?: "",
                umdCd = dto.umdCd ?: "",
                riCd = dto.riCd ?: "",
                locatjuminCd = dto.locatjuminCd ?: "",
                locatjijukCd = dto.locatjijukCd ?: "",
                locataddNm = dto.locataddNm ?: "",
                locatOrder = dto.locatOrder ?: 0,
                locatRm = dto.locatRm ?: "",
                locathighCd = dto.locathighCd.takeIf { it != "0000000000" && !it.isNullOrEmpty() },
                locallowNm = dto.locallowNm ?: "",
                adptDe = dto.adptDe ?: "",
                level = level
            )

            logger.debug("Processed region: ${region.regionCd} (${region.locataddNm}) at level $level")
            region
        }
    }

    @Bean
    fun regionItemWriter(): ItemWriter<Region> {
        return ItemWriter { regions ->
            try {
                val validRegions = regions.filterNotNull()
                logger.info("Attempting to save ${validRegions.size} regions")

                // 샘플 로그 출력
                if (validRegions.isNotEmpty()) {
                    val sample = validRegions.first()
                    logger.info("Sample region: ${sample.regionCd}, ${sample.locataddNm}, level=${sample.level}, parent=${sample.locathighCd}")
                }

                // 저장 시도
                regionRepository.saveAll(validRegions)
                logger.info("Successfully saved ${validRegions.size} regions to database")

            } catch (e: Exception) {
                logger.error("Failed to save regions: ${e.message}", e)
                // 스택 트레이스 출력
                e.printStackTrace()
                throw RuntimeException("Region save failed", e)
            }
        }
    }

    @Bean
    fun runBatchJob(regionJob: Job): CommandLineRunner {
        return CommandLineRunner {
            try {
                val jobParameters = JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters()
                logger.info("Running region batch job with parameters: $jobParameters")

                val jobExecution = jobLauncher.run(regionJob, jobParameters)
                logger.info("Job execution status: ${jobExecution.status}")
            } catch (e: Exception) {
                logger.error("Failed to run batch job: ${e.message}", e)
            }
        }
    }
}
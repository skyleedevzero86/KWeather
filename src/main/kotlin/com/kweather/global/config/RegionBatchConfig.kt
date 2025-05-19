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
import org.springframework.batch.item.support.ListItemReader
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.nio.file.Files
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.JobParametersBuilder

@Configuration
@EnableBatchProcessing
class RegionBatchConfig(
    private val regionApiService: RegionApiService,
    private val regionRepository: RegionRepository,
    private val objectMapper: ObjectMapper,
    private val jobLauncher: JobLauncher
) {
    private val logger = LoggerFactory.getLogger(RegionBatchConfig::class.java)
    private val regionCache = ConcurrentHashMap<String, Region>()
    private val cacheFile = File("region_cache.json")

    private fun loadCachedRegions(): List<RegionDto> {
        if (!cacheFile.exists()) {
            logger.warn("Cache file does not exist: ${cacheFile.absolutePath}")
            val defaultData = loadDefaultRegions()
            if (defaultData.isNotEmpty()) {
                saveToCache(defaultData)
                return defaultData
            }
            return emptyList()
        }
        val json = Files.readString(cacheFile.toPath())
        logger.debug("Loading cached JSON: $json")  // JSON 데이터 로깅 추가
        return try {
            objectMapper.readValue(json, Array<RegionDto>::class.java).toList()
        } catch (e: Exception) {
            logger.error("Failed to parse cached JSON: ${e.message}", e)
            emptyList()
        }
    }

    private fun loadDefaultRegions(): List<RegionDto> {
        val resource = javaClass.classLoader.getResourceAsStream("default_regions.json")
        return if (resource != null) {
            objectMapper.readValue(resource, Array<RegionDto>::class.java).toList()
        } else {
            logger.warn("No default regions found in resources")
            emptyList()
        }
    }

    private fun saveToCache(regions: List<RegionDto>) {
        try {
            val json = objectMapper.writeValueAsString(regions)
            Files.writeString(cacheFile.toPath(), json)
            logger.info("Saved ${regions.size} regions to cache")
        } catch (e: Exception) {
            logger.error("Failed to save regions to cache: ${e.message}", e)
        }
    }

    @Bean
    fun regionJob(jobRepository: JobRepository, step: Step): Job {
        return JobBuilder("regionJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(step)
            .listener(object : JobExecutionListener {
                override fun beforeJob(jobExecution: JobExecution) {
                    logger.info("Starting region import job")
                    regionCache.clear()
                }

                override fun afterJob(jobExecution: JobExecution) {
                    logger.info("Job completed with status: ${jobExecution.status}")
                    if (jobExecution.status == BatchStatus.FAILED) {
                        jobExecution.allFailureExceptions.forEach { e ->
                            logger.error("Job failure", e)
                        }
                    }
                    regionCache.clear()
                }
            })
            .build()
    }

    @Bean
    fun regionStep(jobRepository: JobRepository, transactionManager: PlatformTransactionManager): Step {
        return StepBuilder("regionStep", jobRepository)
            .chunk<RegionDto, Region>(1000, transactionManager)
            .reader(regionItemReader())
            .processor(regionItemProcessor())
            .writer { items -> items.forEach { println(it) } }
            .faultTolerant()
            .retryLimit(3)
            .retry(Exception::class.java)
            .listener(object : StepExecutionListener {
                override fun beforeStep(stepExecution: StepExecution) {
                    logger.info("Clearing existing regions")
                    regionRepository.deleteAllInBatch()
                }

                override fun afterStep(stepExecution: StepExecution): ExitStatus? {
                    logger.info("Step completed with status: ${stepExecution.status}")
                    return null
                }
            })
            .build()
    }

    @Bean
    fun regionItemReader(): ItemReader<RegionDto> {
        val allData = regionApiService.fetchAllRegionData()
        if (allData.isEmpty()) {
            println("API에서 데이터를 못 가져왔어요. 캐시를 확인합니다.")
            val cachedData = loadCachedRegions()
            if (cachedData.isEmpty()) {
                println("캐시도 없네요. 빈 데이터로 시작합니다.")
                return ListItemReader(emptyList())
            }
            return ListItemReader(cachedData)
        }
        return ListItemReader(allData)
    }

    @Bean
    fun regionItemProcessor(): ItemProcessor<RegionDto, Region> {
        return ItemProcessor { dto ->
            try {
                logger.debug("Processing region: ${dto.locataddNm} (${dto.regionCd}), parent: ${dto.locathighCd}")
                val region = Region(
                    regionCd = dto.regionCd,
                    sidoCd = dto.sidoCd ?: "",
                    sggCd = dto.sggCd ?: "",
                    umdCd = dto.umdCd ?: "",
                    riCd = dto.riCd,
                    locatjuminCd = dto.locatjuminCd ?: "",
                    locatjijukCd = dto.locatjijukCd ?: "",  // null일 경우 빈 문자열로 처리
                    locataddNm = dto.locataddNm,
                    locatOrder = dto.locatOrder,
                    locatRm = dto.locatRm,
                    locathighCd = dto.locathighCd,
                    locallowNm = dto.locallowNm,
                    adptDe = dto.adptDe
                )
                region
            } catch (e: Exception) {
                logger.error("Failed to process region ${dto.locataddNm} (${dto.regionCd}): ${e.message}", e)
                null
            }
        }
    }


    @Bean
    fun regionItemWriter(): ItemWriter<Region> {
        return ItemWriter { regions ->
            try {
                val validRegions = regions.filterNotNull()
                validRegions.forEach { region ->
                    val parentCode = region.locathighCd
                    if (parentCode != "0000000000" && parentCode.isNotEmpty()) {
                        val parent = regionRepository.findByRegionCd(parentCode)
                        if (parent != null) {
                            region.parent = parent
                            parent.addChild(region)
                        } else {
                            logger.warn("Parent region not found for code: $parentCode (child: ${region.regionCd})")
                        }
                    }
                }
                regionRepository.saveAll(validRegions)
                regionRepository.flush()
                logger.info("Saved ${validRegions.size} regions to database")
            } catch (e: Exception) {
                logger.error("Failed to save regions: ${e.message}", e)
                throw RuntimeException("Region save failed", e)
            }
        }
    }

    @Bean
    fun runBatchJob(regionJob: Job): CommandLineRunner {
        return CommandLineRunner {
            val jobParameters = JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters()
            jobLauncher.run(regionJob, jobParameters)
        }
    }
}
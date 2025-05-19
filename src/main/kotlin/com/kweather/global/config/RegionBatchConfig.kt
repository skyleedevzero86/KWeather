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
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.nio.file.Files
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.JobParametersBuilder
import java.util.concurrent.ConcurrentHashMap

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
        logger.debug("Loading cached JSON: $json")
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
            .writer(regionItemWriter())
            .faultTolerant()
            .retryLimit(3)
            .retry(Exception::class.java)
            .skip(org.springframework.dao.DataIntegrityViolationException::class.java)
            .skipLimit(10)
            .listener(object : SkipListener<RegionDto, Region> {
                override fun onSkipInWrite(item: Region, t: Throwable) {
                    logger.warn("Skipped region ${item.regionCd} due to: ${t.message}")
                }
                override fun onSkipInRead(t: Throwable) {}
                override fun onSkipInProcess(item: RegionDto, t: Throwable) {}
            })
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
        return object : ItemReader<RegionDto> {
            private var regions: List<RegionDto> = emptyList()
            private var index = 0

            init {
                try {
                    val apiRegions = regionApiService.fetchAllRegionData()
                    if (apiRegions.isEmpty()) {
                        logger.info("API에서 데이터를 못 가져왔어요. 캐시를 확인합니다.")
                        val cachedJson = File("region_cache.json").readText()
                        val typeRef = object : com.fasterxml.jackson.core.type.TypeReference<List<RegionDto>>() {}
                        regions = objectMapper.readValue(cachedJson, typeRef)
                            .sortedBy { region -> region.locatOrder ?: Int.MAX_VALUE }
                    } else {
                        regions = apiRegions.sortedBy { region -> region.locatOrder ?: Int.MAX_VALUE }
                    }
                    logger.info("Loaded ${regions.size} regions for reading")
                } catch (e: Exception) {
                    logger.error("Failed to load regions: ${e.message}", e)
                    regions = emptyList()
                }
            }

            override fun read(): RegionDto? {
                return if (index < regions.size) {
                    regions[index++]
                } else {
                    null
                }
            }
        }
    }

    @Bean
    fun regionItemProcessor(): ItemProcessor<RegionDto, Region> {
        return ItemProcessor { dto ->
            logger.debug("Processing RegionDto: ${dto.locataddNm} (${dto.regionCd})")

            // 지역 코드 구조에 따라 level을 결정
            val level = when {
                // 시/도 레벨 (XX00000000)
                dto.regionCd.endsWith("00000000") || dto.sidoCd != null && dto.sggCd == null -> 1
                // 시/군/구 레벨 (XXYYY00000)
                dto.regionCd.endsWith("00000") || dto.sggCd != null && dto.umdCd == null -> 2
                // 읍/면/동 레벨 (XXYYYZZ000)
                else -> 3
            }

            Region(
                regionCd = dto.regionCd,
                sidoCd = dto.sidoCd ?: "",
                sggCd = dto.sggCd ?: "",
                umdCd = dto.umdCd ?: "",
                riCd = dto.riCd ?: "",
                locatjuminCd = dto.locatjuminCd ?: "",
                locatjijukCd = dto.locatjijukCd ?: "",
                locataddNm = dto.locataddNm ?: "",
                locatOrder = dto.locatOrder ?: 0,
                locatRm = dto.locatRm,
                locathighCd = if (dto.locathighCd == "0000000000") null else dto.locathighCd,
                locallowNm = dto.locallowNm ?: "",
                adptDe = dto.adptDe,
                level = level  // level 파라미터 추가
            )
        }
    }

    @Bean
    fun regionItemWriter(): ItemWriter<Region> {
        return ItemWriter { regions ->
            try {
                val validRegions = regions.filterNotNull()
                logger.info("Attempting to save ${validRegions.size} regions")

                val regionsToInsert = mutableListOf<Region>()
                validRegions.forEach { region ->
                    logger.debug("Processing region: ${region.locataddNm} (${region.regionCd})")
                    var updatedRegion = region
                    if (region.locathighCd != null && region.locathighCd != "0000000000" && region.locathighCd.isNotEmpty()) {
                        val parent = regionRepository.findByRegionCd(region.locathighCd)
                        if (parent != null) {
                            updatedRegion = region.copy(parent = parent)
                            parent.addChild(updatedRegion)
                        } else {
                            logger.warn("Parent region not found for code: ${region.locathighCd} (child: ${region.regionCd})")
                            updatedRegion = region.copy(locathighCd = null)
                        }
                    } else if (region.locathighCd == "0000000000") {
                        logger.warn("Invalid parent code '0000000000' for region: ${region.regionCd}")
                        updatedRegion = region.copy(locathighCd = null)
                    }
                    regionsToInsert.add(updatedRegion)
                }

                regionRepository.saveAll(regionsToInsert)
                regionRepository.flush()
                logger.info("Successfully saved ${regionsToInsert.size} regions to database")
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
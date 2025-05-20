package com.kweather.global.config

import com.kweather.domain.region.dto.RegionDto
import com.kweather.domain.region.entity.Region
import com.kweather.global.common.batch.RegionProcessor
import com.kweather.global.common.batch.RegionReader
import com.kweather.global.common.batch.RegionWriter
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.client.RestTemplate

@Configuration
class BatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val regionReader: RegionReader,
    private val regionProcessor: RegionProcessor,
    private val regionWriter: RegionWriter,
    private val restTemplate: RestTemplate
) {

    @Bean
    fun importRegionJob(importRegionStep: Step): Job {
        return JobBuilder("importRegionJob", jobRepository)
            .start(importRegionStep)
            .build()
    }

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
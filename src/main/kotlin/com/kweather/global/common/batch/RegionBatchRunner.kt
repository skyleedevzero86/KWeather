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
                val startTime = System.currentTimeMillis()
                val startDate = Date(startTime)

                val jobParameters = JobParametersBuilder()
                    .addDate("startTime", startDate)
                    .addString("runMode", "startup")
                    .toJobParameters()

                val jobExecution = jobLauncher.run(importRegionJob, jobParameters)

                val endTime = System.currentTimeMillis()
                val endDate = Date(endTime)
                val durationMs = endTime - startTime
                val durationMinutes = durationMs / 1000 / 60
                val durationSeconds = (durationMs / 1000) % 60

                if (jobExecution.status.isUnsuccessful) {
                }
            } catch (e: Exception) {
            } finally {
                hierarchyService.clearCache()
            }
        }
    }
}
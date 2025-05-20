package com.kweather.global.common.batch

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
    @Value("\${region.batch.startup-execution:false}") private val startupExecution: Boolean
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(RegionBatchRunner::class.java)

    override fun run(args: ApplicationArguments) {
        if (startupExecution) {
            try {
                logger.info("Starting region data import job on application startup")

                val jobParameters = JobParametersBuilder()
                    .addDate("time", Date())
                    .toJobParameters()

                val jobExecution = jobLauncher.run(importRegionJob, jobParameters)

                logger.info("Region data import job completed with status: {}", jobExecution.status)
            } catch (e: Exception) {
                logger.error("Error executing region data import job", e)
            }
        } else {
            logger.info("Startup execution for region data import is disabled")
        }
    }
}
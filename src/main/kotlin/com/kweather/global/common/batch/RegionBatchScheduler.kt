package com.kweather.global.common.batch

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class RegionBatchScheduler(
    private val jobLauncher: JobLauncher,
    private val regionJob: Job
) {
    //@Scheduled(fixedDelay = 180000)
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정 실행
    fun runRegionJob() {
        try {
            val jobParameters = JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters()
            jobLauncher.run(regionJob, jobParameters)
            println("Region batch job executed successfully")
        } catch (e: Exception) {
            println("Error executing region batch job: ${e.message}")
        }
    }
}
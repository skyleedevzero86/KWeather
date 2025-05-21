package com.kweather.domain.forecast.dto

import java.util.*

data class ForecastInfo(
    val date: String,
    val type: String,
    val overall: String,
    val cause: String,
    val grade: String,
    val dataTime: String,
    val imageUrls: List<String>
) {
    companion object {
        fun parseGrade(gradeText: String, targetGrade: String): List<String> {
            val regions = mutableListOf<String>()
            val gradeEntries = gradeText.split(',').map { it.trim() }
            gradeEntries.forEach { entry ->
                val (region, grade) = entry.split(':').map { it.trim() }
                if (grade == targetGrade) {
                    regions.add(region)
                }
            }
            return regions
        }
    }
}
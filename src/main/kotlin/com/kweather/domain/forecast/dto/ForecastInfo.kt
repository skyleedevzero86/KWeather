package com.kweather.domain.forecast.dto

data class ForecastInfo(
    val date: String,           // 예보 날짜
    val type: String,           // 예보 종류 (PM10, PM25, O3)
    val overall: String,        // 전체 예보 요약
    val cause: String,          // 예보 원인
    val grade: String,          // 지역별 예보 등급
    val dataTime: String,       // 발표 시간
    val imageUrls: List<String> // 예보 이미지 URL 리스트
)
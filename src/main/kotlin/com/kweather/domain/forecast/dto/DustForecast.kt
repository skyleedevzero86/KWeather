package com.kweather.domain.forecast.dto

data class DustForecast(
    val date: String,           // 예: 2025-05-21
    val type: String,           // 예: O3
    val dataTime: String,       // 예: 2025-05-20 23시
    val overall: String,        // 예: 경기남부·강원권·충남·경북은 '나쁨', 그 밖의 권역은 '보통'으로 예상됩니다.
    val cause: String,          // 예: 대기오염물질의 광화학 반응에 의한 오존 생성과 이동으로...
    val grade: String,          // 예: 경기남부:나쁨,강원권:나쁨,충남:나쁨,경북:나쁨,그밖의권역:보통
    val imageUrls: List<String> // 예: ["url1", "url2", ...]
)
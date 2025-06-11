package com.kweather.global.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * KotlinWeather 애플리케이션의 핵심 설정을 담당하는 구성 클래스
 *
 * 이 클래스는 다음과 같은 주요 기능을 제공합니다:
 * - 설정 클래스들을 통합 관리
 * - OpenAPI 문서화 설정
 * - 모듈별 설정 Import
 *
 * @author kylee (궁금하면 500원)
 * @version 1.0
 * @since 2025-05-24
 */
@Configuration
@Import(
    JacksonConfig::class,
    HttpClientConfig::class,
    WebMvcConfig::class,
    FilterConfig::class
)
@OpenAPIDefinition(
    info = Info(
        title = "KotlinWeather API",
        version = "1.0.0",
        description = """
            KotlinWeather 애플리케이션의 RESTful API 문서입니다.
            
            이 API는 날씨 정보 조회 및 관련 서비스를 제공합니다.
            모든 응답은 UTF-8 인코딩된 JSON 형식으로 제공됩니다.
            
            ## 주요 기능
            - 실시간 날씨 정보 조회
            - 날씨 예보 데이터 제공
            - 지역별 날씨 정보 검색
            
            ## 응답 형식
            - Content-Type: application/json;charset=UTF-8
            - 모든 날짜는 ISO-8601 형식으로 제공
        """,
        contact = Contact(
            name = "KotlinWeather Support",
            email = "support@kotlinweather.com",
            url = "https://kotlinweather.com"
        ),
        license = License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = [
        Server(
            url = "http://localhost:8080",
            description = "개발 서버"
        ),
        Server(
            url = "https://api.kotlinweather.com",
            description = "프로덕션 서버"
        )
    ]
)
class AppConfig
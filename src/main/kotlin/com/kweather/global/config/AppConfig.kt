package com.kweather.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.servers.Server
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.TimeValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * KotlinWeather 애플리케이션의 핵심 설정을 담당하는 구성 클래스
 *
 * 이 클래스는 다음과 같은 주요 기능을 제공합니다:
 * - Jackson ObjectMapper 설정 및 Bean 등록
 * - HTTP 클라이언트 연결 풀링이 적용된 RestTemplate 구성
 * - 응답 인코딩 필터 설정
 * - 정적 리소스 핸들링 구성
 *
 * @author kylee (궁금하면 500원)
 * @version 1.0
 * @since 2025-05-24
 */
@Configuration
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
class AppConfig : WebMvcConfigurer {

    @Value("\${custom.site.name:KotlinWeather}")
    private lateinit var siteName: String

    @Value("\${queue.api.base-url:http://localhost:8080}")
    private lateinit var baseUrl: String

    @Bean
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Bean
    fun restTemplate(): RestTemplate {
        val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnPerRoute(20)
            .setMaxConnTotal(50)
            .setValidateAfterInactivity(TimeValue.ofSeconds(10))
            .build()

        val httpClient: CloseableHttpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .evictIdleConnections(TimeValue.ofSeconds(30))
            .build()

        val factory = HttpComponentsClientHttpRequestFactory(httpClient).apply {
            setConnectTimeout(15000)
            setReadTimeout(15000)
        }

        val restTemplate = RestTemplate(factory)
        restTemplate.messageConverters.removeIf { it is MappingJackson2XmlHttpMessageConverter }
        restTemplate.messageConverters.add(0, MappingJackson2HttpMessageConverter(objectMapper()))
        restTemplate.messageConverters.add(MappingJackson2XmlHttpMessageConverter(XmlMapper()))
        return restTemplate
    }

    @Bean
    fun encodingFilter(): OncePerRequestFilter {
        return object : OncePerRequestFilter() {
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain
            ) {
                val contentType = response.contentType
                when {
                    contentType == null -> response.contentType = "application/json;charset=UTF-8"
                    contentType.contains("application/json") && !contentType.contains("charset") ->
                        response.contentType = "application/json;charset=UTF-8"
                    contentType.contains("text/html") && !contentType.contains("charset") ->
                        response.contentType = "text/html;charset=UTF-8"
                }
                filterChain.doFilter(request, response)
            }
        }
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(0)

        registry.addResourceHandler("/gen/images/**")
            .addResourceLocations("classpath:/static/gen/images/")
            .setCachePeriod(0)

        registry.addResourceHandler("/static/global/**")
            .addResourceLocations("classpath:/static/global/")
            .setCachePeriod(0)

        registry.addResourceHandler("/favicon.ico")
            .addResourceLocations("classpath:/static/favicon.ico")
            .setCachePeriod(0)
    }
}
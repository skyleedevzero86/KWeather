package com.kweather.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.IOException

/**
 * 애플리케이션 공통 설정 클래스
 * - 정적 리소스 매핑
 * - ObjectMapper (Jackson)
 * - RestTemplate
 * - 인코딩 필터
 */
@Configuration
class AppConfig : WebMvcConfigurer {

    /**
     * 사이트 이름을 application.yml 또는 properties에서 주입받습니다.
     */
    @Value("\${custom.site.name}")
    lateinit var siteName: String

    /**
     * AppConfig 클래스의 싱글턴 인스턴스를 설정합니다.
     * 클래스 내부 companion object에서 접근하기 위해 사용됩니다.
     */
    @Value("\${queue.api.base-url:http://localhost:8080}")
    private lateinit var baseUrl: String

    @PostConstruct
    fun init() {
        instance = this
    }

    companion object {
        lateinit var instance: AppConfig
            private set
    }

    /**
     * JSON 직렬화를 위한 공통 ObjectMapper Bean 등록
     * - Kotlin 모듈 지원
     * - JavaTime 모듈 지원
     * - Pretty Print 비활성화
     * - 타임스탬프 직렬화 옵션 설정
     */
    @Bean
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * 외부 API 통신용 RestTemplate Bean
     * - 기본 timeout 설정 포함
     */
    @Bean
    fun restTemplate(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5000)
            setReadTimeout(5000)
        }
        return RestTemplate(factory)
    }

    /**
     * 응답 Content-Type에 UTF-8 charset 설정하는 필터
     */
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
                    contentType == null -> {
                        response.contentType = "application/json;charset=UTF-8"
                    }
                    contentType.contains("application/json") && !contentType.contains("charset") -> {
                        response.contentType = "application/json;charset=UTF-8"
                    }
                    contentType.contains("text/html") && !contentType.contains("charset") -> {
                        response.contentType = "text/html;charset=UTF-8"
                    }
                }
                filterChain.doFilter(request, response)
            }
        }
    }

    /**
     * 정적 리소스 경로 설정
     */
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

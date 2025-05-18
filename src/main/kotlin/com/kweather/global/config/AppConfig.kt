package com.kweather.global.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException

/**
 * 웹 애플리케이션의 공통 설정을 담당하는 구성 클래스입니다.
 * - 정적 리소스 매핑
 * - 공통 필터 등록
 * - 외부 API 통신용 RestTemplate 등록
 */
@Configuration
class AppConfig : WebMvcConfigurer {

    /**
     * 사이트 이름을 application.yml 또는 properties에서 주입받습니다.
     */
    @Value("\${custom.site.name}")
    lateinit var siteName: String

    /**
     * 외부 큐 API의 기본 URL을 설정합니다.
     * 기본값은 `http://localhost:8080` 입니다.
     */
    @Value("\${queue.api.base-url:http://localhost:8080}")
    private lateinit var baseUrl: String

    /**
     * AppConfig 클래스의 싱글턴 인스턴스를 설정합니다.
     * 클래스 내부 companion object에서 접근하기 위해 사용됩니다.
     */
    @PostConstruct
    fun init() {
        instance = this
    }

    /**
     * RestTemplate 빈을 등록합니다.
     *
     * @return RestTemplate 인스턴스
     */
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

    /**
     * 요청/응답의 Content-Type에 charset=UTF-8을 설정하는 필터입니다.
     * JSON 또는 HTML 응답의 인코딩 문제를 방지합니다.
     *
     * @return OncePerRequestFilter 인스턴스
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
     * 정적 리소스 경로를 설정합니다.
     *
     * @param registry 정적 자원 등록기
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

    companion object {
        private lateinit var instance: AppConfig

        /**
         * 정적 리소스(static/)의 절대 경로를 반환합니다.
         * 서버 내 실제 경로 접근을 위해 사용됩니다.
         *
         * @return static 디렉터리의 절대 경로
         * @throws RuntimeException 디렉터리 접근 실패 시 예외 발생
         */
        fun getResourcesStaticDirPath(): String {
            val resource = ClassPathResource("static/")
            return try {
                resource.file.absolutePath
            } catch (e: IOException) {
                throw RuntimeException("정적 리소스 디렉터리에 접근하지 못했습니다.", e)
            }
        }
    }
}
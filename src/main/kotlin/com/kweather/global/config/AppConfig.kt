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

@Configuration
class AppConfig : WebMvcConfigurer {
    @Value("\${custom.site.name}")
    lateinit var siteName: String

    @Value("\${queue.api.base-url:http://localhost:8080}")
    private lateinit var baseUrl: String

    @PostConstruct
    fun init() {
        instance = this
    }

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

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
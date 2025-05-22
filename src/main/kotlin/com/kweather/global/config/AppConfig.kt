package com.kweather.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
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

@Configuration
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
            setConnectTimeout(15000) // 타임아웃 15초로 증가
            setReadTimeout(15000)   // 타임아웃 15초로 증가
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
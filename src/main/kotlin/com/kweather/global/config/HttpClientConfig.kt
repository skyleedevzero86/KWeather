package com.kweather.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.kweather.global.properties.HttpClientProperties
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.TimeValue
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.web.client.RestTemplate

/**
 * HTTP 클라이언트 및 RestTemplate 설정을 전담하는 구성 클래스
 *
 * HTTP 연결 관련 설정을 담당합니다:
 * - 연결 풀링 설정
 * - 타임아웃 설정
 * - 메시지 컨버터 설정
 */
@Configuration
class HttpClientConfig(
    private val objectMapper: ObjectMapper,
    private val httpClientProperties: HttpClientProperties
) {

    @Bean
    fun restTemplate(): RestTemplate {
        val connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnPerRoute(httpClientProperties.maxConnPerRoute)
            .setMaxConnTotal(httpClientProperties.maxConnTotal)
            .setValidateAfterInactivity(TimeValue.ofSeconds(httpClientProperties.validateAfterInactivitySeconds))
            .build()

        val httpClient: CloseableHttpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .evictIdleConnections(TimeValue.ofSeconds(httpClientProperties.evictIdleConnectionsSeconds))
            .build()

        val factory = HttpComponentsClientHttpRequestFactory(httpClient).apply {
            setConnectTimeout(httpClientProperties.connectTimeoutMs)
            setReadTimeout(httpClientProperties.readTimeoutMs)
        }

        val restTemplate = RestTemplate(factory)

        // XML 컨버터 제거 후 JSON, XML 컨버터 재설정
        restTemplate.messageConverters.removeIf { it is MappingJackson2XmlHttpMessageConverter }
        restTemplate.messageConverters.add(0, MappingJackson2HttpMessageConverter(objectMapper))
        restTemplate.messageConverters.add(MappingJackson2XmlHttpMessageConverter(XmlMapper()))

        return restTemplate
    }
}
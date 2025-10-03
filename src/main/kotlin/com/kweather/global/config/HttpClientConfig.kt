package com.kweather.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.kweather.global.properties.HttpClientProperties
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.util.TimeValue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter
import org.springframework.web.client.RestTemplate

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

        restTemplate.messageConverters.removeIf { it is MappingJackson2XmlHttpMessageConverter }
        restTemplate.messageConverters.add(0, MappingJackson2HttpMessageConverter(objectMapper))
        restTemplate.messageConverters.add(MappingJackson2XmlHttpMessageConverter(XmlMapper()))

        return restTemplate
    }
}
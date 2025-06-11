package com.kweather.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.http-client")
data class HttpClientProperties(
    var maxConnPerRoute: Int = 20,
    var maxConnTotal: Int = 50,
    var validateAfterInactivitySeconds: Long = 10,
    var evictIdleConnectionsSeconds: Long = 30,
    var connectTimeoutMs: Int = 15000,
    var readTimeoutMs: Int = 15000
)
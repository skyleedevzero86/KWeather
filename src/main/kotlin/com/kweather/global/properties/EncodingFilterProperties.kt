package com.kweather.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.encoding-filter")
data class EncodingFilterProperties(
    var enabled: Boolean = true,
    var charset: String = "UTF-8",
    var defaultContentType: String = "application/json;charset=UTF-8",
    var setDefaultContentType: Boolean = true,
    var supportedContentTypes: List<String> = listOf("application/json", "text/html", "text/plain")
)
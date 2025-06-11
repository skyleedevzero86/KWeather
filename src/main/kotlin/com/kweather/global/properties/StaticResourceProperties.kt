package com.kweather.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.static-resources")
data class StaticResourceProperties(
    var mappings: List<ResourceMapping> = defaultMappings()
) {
    companion object {
        fun defaultMappings() = listOf(
            ResourceMapping("/static/**", listOf("classpath:/static/"), 0),
            ResourceMapping("/gen/images/**", listOf("classpath:/static/gen/images/"), 0),
            ResourceMapping("/static/global/**", listOf("classpath:/static/global/"), 0),
            ResourceMapping("/favicon.ico", listOf("classpath:/static/favicon.ico"), 0)
        )
    }
}

data class ResourceMapping(
    val urlPattern: String,
    val locations: List<String>,
    val cachePeriodSeconds: Int
)
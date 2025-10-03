package com.kweather.global.config

import com.kweather.global.properties.ResourceMapping
import com.kweather.global.properties.StaticResourceProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val staticResourceProperties: StaticResourceProperties
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        staticResourceProperties.mappings.forEach { mapping: ResourceMapping ->
            registry.addResourceHandler(mapping.urlPattern)
                .addResourceLocations(*mapping.locations.toTypedArray())
                .setCachePeriod(mapping.cachePeriodSeconds)
        }
    }
}
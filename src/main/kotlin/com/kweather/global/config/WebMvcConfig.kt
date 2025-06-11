package com.kweather.global.config

import com.kweather.global.properties.ResourceMapping
import com.kweather.global.properties.StaticResourceProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Spring WebMVC 설정을 전담하는 구성 클래스
 *
 * 웹 관련 설정을 담당합니다:
 * - 정적 리소스 핸들링
 * - 캐시 정책 설정
 */
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
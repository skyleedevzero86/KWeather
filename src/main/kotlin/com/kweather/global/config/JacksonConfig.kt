package com.kweather.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Jackson 설정 클래스입니다.
 * KotlinModule을 등록하여 Kotlin 지원을 활성화합니다.
 */
@Configuration
class JacksonConfig {

    /**
     * Jackson ObjectMapper Bean을 생성합니다.
     *
     * @return KotlinModule이 등록된 ObjectMapper
     */
    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().registerModule(KotlinModule.Builder().build())
    }
}

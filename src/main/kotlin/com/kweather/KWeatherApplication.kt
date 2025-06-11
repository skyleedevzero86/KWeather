package com.kweather

import com.kweather.global.properties.EncodingFilterProperties
import com.kweather.global.properties.HttpClientProperties
import com.kweather.global.properties.StaticResourceProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    StaticResourceProperties::class,
    HttpClientProperties::class,
    EncodingFilterProperties::class
)
class KWeatherApplication

fun main(args: Array<String>) {
    runApplication<KWeatherApplication>(*args)
}
package com.kweather

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.kweather"])
class KWeatherApplication

fun main(args: Array<String>) {
    runApplication<KWeatherApplication>(*args)
}

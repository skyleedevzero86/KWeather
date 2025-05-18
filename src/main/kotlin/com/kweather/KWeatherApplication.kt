package com.kweather

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KWeatherApplication

fun main(args: Array<String>) {
    runApplication<KWeatherApplication>(*args)
}

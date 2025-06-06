package com.kweather.domain.weather.controller

import com.kweather.domain.weather.entity.Weather;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
class HourlyRestController {

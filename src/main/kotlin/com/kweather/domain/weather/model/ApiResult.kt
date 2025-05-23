package com.kweather.domain.weather.model

// 응답 결과 래핑을 위한 시일드 클래스 정의
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : ApiResult<Nothing>()
}
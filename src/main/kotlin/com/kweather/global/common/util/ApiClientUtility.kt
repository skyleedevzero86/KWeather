package com.kweather.global.common.util

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import com.kweather.domain.weather.model.ApiResult

object ApiClientUtility {
    private val logger = LoggerFactory.getLogger(ApiClientUtility::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    }

    interface ApiClient<P, T> {
        fun buildUrl(params: P): String
        fun parseResponse(response: String): Either<String, T>
    }

    @Retryable(
        value = [SocketTimeoutException::class, IOException::class],
        maxAttempts = 5,
        backoff = Backoff(delay = 2000, multiplier = 2.0)
    )
    fun <P, T, R> makeApiRequest(
        client: ApiClient<P, T>,
        params: P,
        transform: (T) -> Either<String, R>
    ): ApiResult<R> {
        logger.info("API 요청 시작: $params")
        return try {
            val urlString = client.buildUrl(params)
            val response = fetchDataFromApi(urlString).fold(
                { error -> return ApiResult.Error("데이터 가져오기 실패: $error") },
                { it }
            )
            if (response.trim().startsWith("<")) {
                logger.error("XML 오류 응답 수신: $response")
                return ApiResult.Error("API에서 XML 오류 응답을 반환했습니다")
            }
            when (val parseResult = client.parseResponse(response)) {
                is Either.Left -> ApiResult.Error(parseResult.value)
                is Either.Right -> when (val transformResult = transform(parseResult.value)) {
                    is Either.Left -> ApiResult.Error(transformResult.value)
                    is Either.Right -> ApiResult.Success(transformResult.value)
                }
            }
        } catch (e: Exception) {
            logger.error("API 요청 실패: ${e.message}", e)
            ApiResult.Error("API 요청 실패: ${e.message}", e)
        }
    }

    @Retryable(
        value = [SocketTimeoutException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, multiplier = 2.0)
    )
    private fun fetchDataFromApi(urlString: String): Either<String, String> =
        Either.catch {
            URL(urlString).openConnection().let { conn ->
                (conn as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 30000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "KWeather/1.0")
                }
                val responseCode = conn.responseCode
                val reader = if (responseCode in 200..299) {
                    BufferedReader(InputStreamReader(conn.inputStream))
                } else {
                    BufferedReader(InputStreamReader(conn.errorStream))
                }
                use(reader) { r -> r.lines().collect(java.util.stream.Collectors.joining()) }
            }
        }.mapLeft { e ->
            logger.error("HTTP 요청 실패: ${e.message}", e)
            "HTTP 요청 실패: ${e.message}"
        }

    private inline fun <T : AutoCloseable, R> use(resource: T, block: (T) -> R): R {
        try {
            return block(resource)
        } finally {
            try {
                resource.close()
            } catch (e: IOException) {
                logger.error("리소스 닫기 실패", e)
            }
        }
    }

    fun getObjectMapper(): ObjectMapper = objectMapper
}
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
import java.util.stream.Collectors

/**
 * API 클라이언트 유틸리티
 *
 * 외부 API 호출을 위한 공통 유틸리티 클래스입니다.
 * 재시도 메커니즘과 에러 핸들링을 포함합니다.
 *
 * @author kylee (궁금하면 500원)
 * @version 1.0
 * @since 2025-05-24
 */
object ApiClientUtility {
    private val logger = LoggerFactory.getLogger(ApiClientUtility::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    }

    /**
     * API 클라이언트 인터페이스
     *
     * @param P 요청 파라미터 타입
     * @param T 응답 데이터 타입
     */
    interface ApiClient<P, T> {
        /**
         * 요청 파라미터를 사용하여 API URL을 생성합니다.
         *
         * @param params 요청 파라미터
         * @return 생성된 URL 문자열
         */
        fun buildUrl(params: P): String

        /**
         * API 응답을 파싱합니다.
         *
         * @param response 원시 응답 문자열
         * @return 파싱된 결과 또는 에러 메시지
         */
        fun parseResponse(response: String): Either<String, T>
    }

    /**
     * API 요청을 실행합니다.
     *
     * 재시도 메커니즘을 포함하여 안정적인 API 호출을 보장합니다.
     * SocketTimeoutException과 IOException에 대해 최대 5회 재시도합니다.
     *
     * @param client API 클라이언트 구현체
     * @param params 요청 파라미터
     * @param transform 응답 변환 함수
     * @return API 결과 (성공 또는 에러)
     */
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

    /**
     * HTTP GET 요청을 통해 API에서 데이터를 가져옵니다.
     *
     * @param urlString 요청할 URL
     * @return 응답 문자열 또는 에러 메시지
     */
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
                val response = use(reader) { r -> r.lines().collect(Collectors.joining()) }
                logger.info("API 응답: 코드=$responseCode, 본문=$response")
                response
            }
        }.mapLeft { e ->
            logger.error("HTTP 요청 실패: ${e.message}", e)
            "HTTP 요청 실패: ${e.message}"
        }

    /**
     * 리소스를 안전하게 사용하고 해제합니다.
     */
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

    /**
     * ObjectMapper 인스턴스를 반환합니다.
     *
     * @return 설정된 Jackson ObjectMapper
     */
    fun getObjectMapper(): ObjectMapper = objectMapper
}
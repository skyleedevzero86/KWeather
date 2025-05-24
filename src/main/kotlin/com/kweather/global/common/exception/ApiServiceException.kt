package com.kweather.global.common.exception

/**
 * API 서비스 호출 중 발생하는 예외를 처리하기 위한 사용자 정의 예외 클래스입니다.
 * 이 예외는 API 요청 실패, 응답 오류, 또는 기타 서비스 관련 문제를 처리할 때 사용됩니다.
 *
 * @author kylee (궁금하면 500원)
 * @version 1.0
 * @since 2025-05-24
 */
class ApiServiceException : RuntimeException {
    /**
     * 지정된 메시지로 `ApiServiceException`을 생성합니다.
     *
     * @param message 예외 메시지. 발생한 오류의 상세 내용을 설명합니다.
     */
    constructor(message: String?) : super(message)

    /**
     * 지정된 메시지와 원인으로 `ApiServiceException`을 생성합니다.
     *
     * @param message 예외 메시지. 발생한 오류의 상세 내용을 설명합니다.
     * @param cause 예외의 원인. 이 예외를 발생시킨 원인 예외를 지정합니다.
     */
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
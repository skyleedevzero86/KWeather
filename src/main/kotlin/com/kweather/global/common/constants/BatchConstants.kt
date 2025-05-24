package com.kweather.global.common.constants

/**
 * 배치 처리 및 API 호출 관련 상수를 정의하는 클래스입니다.
 * 이 클래스는 배치 작업의 설정값, 로그 메시지, 에러 메시지, HTTP 상태 코드 등을 상수로 제공합니다.
 * 상수는 API 호출, 캐시 처리, 데이터 처리 등의 작업에서 일관된 값을 사용하기 위해 정의되었습니다.
 *
 * @author kylee (궁금하면 500원)
 * @version 1.0
 * @since 2025-05-24
 */
object BatchConstants {
    /**
     * 기본 청크 크기 (한 번에 처리할 데이터 단위).
     */
    const val DEFAULT_CHUNK_SIZE: Int = 1000

    /**
     * 기본 페이지 크기 (한 페이지당 데이터 수).
     */
    const val DEFAULT_PAGE_SIZE: Int = 1000

    /**
     * 기본 최대 페이지 수.
     */
    const val DEFAULT_MAX_PAGE: Int = 21

    /**
     * 기본 스킵 제한 (데이터 스킵 처리 시 사용).
     */
    const val DEFAULT_SKIP_LIMIT: Int = 100

    /**
     * API 호출 타임아웃 시간 (밀리초 단위).
     */
    const val API_TIMEOUT_MS: Int = 15000

    /**
     * API 호출 실패 시 최대 재시도 횟수.
     */
    const val MAX_RETRY_ATTEMPTS: Int = 3

    /**
     * 재시도 간 지연 시간 (밀리초 단위).
     */
    const val RETRY_DELAY_MS: Long = 2000L

    /**
     * 재시도 지연 시간 배수 (지연 시간 증가율).
     */
    const val RETRY_MULTIPLIER: Double = 1.5

    /**
     * 캐시 파일 경로.
     */
    const val CACHE_FILE_PATH: String = "region_cache.json"

    /**
     * 로그 메시지를 정의하는 내부 클래스.
     * API 호출, 데이터 처리, 캐시 로드 등에 사용되는 로그 메시지를 상수로 제공합니다.
     */
    object LogMessages {
        /**
         * API 요청 시작 로그 메시지. 페이지 번호를 포함.
         */
        const val API_REQUEST_START: String = "API 요청 시작: 페이지 {}"

        /**
         * API 응답 성공 로그 메시지. 처리된 데이터 건수를 포함.
         */
        const val API_REQUEST_SUCCESS: String = "API 응답 수신 성공: {} 건"

        /**
         * 페이지별 데이터 로드 완료 로그 메시지.
         */
        const val API_RESPONSE_SUCCESS: String = "페이지 {}에서 {} 건의 지역 데이터 로드 완료"

        /**
         * API 요청 실패 시 캐시 로드 시도 로그 메시지.
         */
        const val API_REQUEST_FAILED: String = "API 요청 실패, 캐시에서 로드 시도"

        /**
         * 캐시에서 데이터 로드 성공 로그 메시지.
         */
        const val CACHE_LOAD_SUCCESS: String = "캐시에서 {} 건의 지역 데이터 로드 완료"

        /**
         * 캐시 파일 로드 실패 로그 메시지.
         */
        const val CACHE_LOAD_FAILED: String = "캐시 파일 로드 실패"

        /**
         * 데이터 저장 성공 로그 메시지.
         */
        const val DATA_SAVE_SUCCESS: String = "지역 데이터 {} 건 저장 완료"

        /**
         * 데이터 저장 실패 로그 메시지.
         */
        const val DATA_SAVE_FAILED: String = "지역 데이터 저장 중 오류 발생"

        /**
         * 필수 필드 누락으로 데이터 스킵 로그 메시지.
         */
        const val INVALID_DATA_SKIP: String = "필수 필드 누락으로 데이터 스킵: {}"

        /**
         * 데이터 처리 중 오류 로그 메시지. 지역 코드 포함.
         */
        const val PROCESSING_ERROR: String = "데이터 처리 중 오류: regionCd={}"

        /**
         * 더 이상 데이터가 없는 경우 로그 메시지.
         */
        const val NO_MORE_DATA: String = "페이지 {}에서 더 이상 데이터가 없습니다"

        /**
         * 총 데이터 로드 완료 로그 메시지.
         */
        const val TOTAL_DATA_LOADED: String = "총 {} 건의 지역 데이터 로드 완료"

        /**
         * API 응답 코드 로그 메시지.
         */
        const val API_RESPONSE_CODE: String = "응답 코드: {}"

        /**
         * API 키 오류 로그 메시지.
         */
        const val API_KEY_ERROR: String = "API 키 오류: {}"

        /**
         * XML 오류 응답 로그 메시지.
         */
        const val XML_ERROR_RESPONSE: String = "XML 오류 응답 수신: {}"

        /**
         * API 데이터 가져오기 실패 로그 메시지.
         */
        const val API_DATA_FETCH_FAILED: String = "API 데이터 가져오기 실패: {}"

        /**
         * 계층 구조 데이터 미리 로드 시작 로그 메시지.
         */
        const val HIERARCHY_DATA_PRELOAD: String = "계층 구조 데이터 미리 로드 시작"

        /**
         * 계층 구조 데이터 미리 로드 완료 로그 메시지.
         */
        const val HIERARCHY_DATA_PRELOAD_COMPLETE: String = "계층 구조 데이터 미리 로드 완료"
    }

    /**
     * 에러 메시지를 정의하는 내부 클래스.
     * API 호출 및 데이터 처리 중 발생하는 에러 메시지를 상수로 제공합니다.
     */
    object ErrorMessages {
        /**
         * 서비스 키가 비어있을 때의 에러 메시지.
         */
        const val SERVICE_KEY_EMPTY: String = "서비스 키는 비어있을 수 없습니다"

        /**
         * 기본 URL이 비어있을 때의 에러 메시지.
         */
        const val BASE_URL_EMPTY: String = "기본 URL은 비어있을 수 없습니다"

        /**
         * API 서비스 키 오류 메시지.
         */
        const val API_SERVICE_KEY_ERROR: String = "API 서비스 키 오류: 키가 유효한지 확인해주세요"

        /**
         * API 오류 응답 메시지.
         */
        const val API_ERROR_RESPONSE: String = "API에서 오류 응답을 반환했습니다"

        /**
         * 캐시 파일 로드 실패 에러 메시지.
         */
        const val CACHE_LOAD_ERROR: String = "캐시 파일 로드 실패"
    }

    /**
     * HTTP 상태 코드를 정의하는 내부 클래스.
     * 성공적인 응답 범위를 정의합니다.
     */
    object HttpStatus {
        /**
         * 성공 응답의 최소 HTTP 상태 코드.
         */
        const val OK_MIN: Int = 200

        /**
         * 성공 응답의 최대 HTTP 상태 코드.
         */
        const val OK_MAX: Int = 299
    }
}
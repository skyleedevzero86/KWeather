package com.kweather.global.common.constants

object BatchConstants {
    const val DEFAULT_CHUNK_SIZE: Int = 1000
    const val DEFAULT_PAGE_SIZE: Int = 1000
    const val DEFAULT_MAX_PAGE: Int = 21
    const val DEFAULT_SKIP_LIMIT: Int = 100
    const val API_TIMEOUT_MS: Int = 15000
    const val MAX_RETRY_ATTEMPTS: Int = 3
    const val RETRY_DELAY_MS: Long = 2000L
    const val RETRY_MULTIPLIER: Double = 1.5
    const val CACHE_FILE_PATH: String = "region_cache.json"

    object LogMessages {
        const val API_REQUEST_START: String = "API 요청 시작: 페이지 {}"
        const val API_REQUEST_SUCCESS: String = "API 응답 수신 성공: {} 건"
        const val API_RESPONSE_SUCCESS: String = "페이지 {}에서 {} 건의 지역 데이터 로드 완료"
        const val API_REQUEST_FAILED: String = "API 요청 실패, 캐시에서 로드 시도"
        const val CACHE_LOAD_SUCCESS: String = "캐시에서 {} 건의 지역 데이터 로드 완료"
        const val CACHE_LOAD_FAILED: String = "캐시 파일 로드 실패"
        const val DATA_SAVE_SUCCESS: String = "지역 데이터 {} 건 저장 완료"
        const val DATA_SAVE_FAILED: String = "지역 데이터 저장 중 오류 발생"
        const val INVALID_DATA_SKIP: String = "필수 필드 누락으로 데이터 스킵: {}"
        const val PROCESSING_ERROR: String = "데이터 처리 중 오류: regionCd={}"
        const val NO_MORE_DATA: String = "페이지 {}에서 더 이상 데이터가 없습니다"
        const val TOTAL_DATA_LOADED: String = "총 {} 건의 지역 데이터 로드 완료"
        const val API_RESPONSE_CODE: String = "응답 코드: {}"
        const val API_KEY_ERROR: String = "API 키 오류: {}"
        const val XML_ERROR_RESPONSE: String = "XML 오류 응답 수신: {}"
        const val API_DATA_FETCH_FAILED: String = "API 데이터 가져오기 실패: {}"
        const val HIERARCHY_DATA_PRELOAD: String = "계층 구조 데이터 미리 로드 시작"
        const val HIERARCHY_DATA_PRELOAD_COMPLETE: String = "계층 구조 데이터 미리 로드 완료"
    }

    object ErrorMessages {
        const val SERVICE_KEY_EMPTY: String = "서비스 키는 비어있을 수 없습니다"
        const val BASE_URL_EMPTY: String = "기본 URL은 비어있을 수 없습니다"
        const val API_SERVICE_KEY_ERROR: String = "API 서비스 키 오류: 키가 유효한지 확인해주세요"
        const val API_ERROR_RESPONSE: String = "API에서 오류 응답을 반환했습니다"
        const val CACHE_LOAD_ERROR: String = "캐시 파일 로드 실패"
    }

    object HttpStatus {
        const val OK_MIN: Int = 200
        const val OK_MAX: Int = 299
    }
}
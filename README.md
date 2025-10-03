# 메인페이지<br/>
![image](https://github.com/user-attachments/assets/b3eec3c2-bafa-460c-b45b-9a00c105d553)

# 미세먼지 예보보기<br/>
![image](https://github.com/user-attachments/assets/2e56df20-4de6-4310-9c37-16acba0617ca)

# 실시간 미세먼지 <br/>
![image](https://github.com/user-attachments/assets/2a155e0a-accc-42d3-b83d-4b649be1723f)


# 여름철 체감온도 예보 (5월~9월) <br/>
![image](https://github.com/user-attachments/assets/071bb7f3-f97c-41cb-99cf-8c2fd7b0b00c)


# 대기정체지수 예보 <br/>
![image](https://github.com/user-attachments/assets/246d77ed-6cf8-417f-b2ec-a7efd7e41376)

# 강수량 예보 <br/>
![image](https://github.com/user-attachments/assets/896a9a8e-727c-423d-a74e-9687538fb652)

# 시간별 온도 예보  <br/>
![image](https://github.com/user-attachments/assets/306fa7d4-37e1-4d26-b623-7b4155246888)
![image](https://github.com/user-attachments/assets/d2bb0f1b-1088-49f2-9668-5b3ad09aaff8)

# 🌤️ 실시간 날씨 정보<br/>
![image](https://github.com/user-attachments/assets/a5bf9d19-2074-4cc1-bb95-8a8d68625cb3)

# 날씨 기상 대시보드<br/>
![image](https://github.com/user-attachments/assets/70771482-8ffe-4587-8e86-442aec4b53cf)
![image](https://github.com/user-attachments/assets/3e4df6d2-e7ad-4394-ac08-7efed006cb90)
![image](https://github.com/user-attachments/assets/389ee550-d461-44b3-a2b8-c32853e59c9d)
![image](https://github.com/user-attachments/assets/066fb41c-4abd-4e7a-b7db-906acd0bc0b4)




***

# KWeather

KWeather는 대한민국 지역별 실시간/예보 날씨, 미세먼지, 체감온도, 대기정체지수, 자외선지수 등 다양한 기상 정보를 제공하는 Spring Boot 기반 웹 애플리케이션입니다.

---

## 주요 기능

- **실시간 날씨 및 미세먼지 정보**  
  기상청, 환경부 등 공공 API 연동  
- **시간별/일별 예보**  
  온도, 강수량, 미세먼지, 체감온도, 대기정체지수 등  
- **지역 선택 및 관리**  
  시/도, 시/군/구, 읍/면/동 계층적 선택  
- **차트 및 대시보드**  
  Chart.js 기반 데이터 시각화  
- **반응형 UI**  
  모바일/PC 모두 지원  

---

## 주요 기술 스택

- Kotlin 1.9  
- Spring Boot 3.4  
- Spring Data JPA (PostgreSQL)  
- Thymeleaf  
- Chart.js  
- Arrow Core (Functional Error Handling)  
- Resilience4j (API 안정성)  
- QueryDSL  
- JUnit5, MockK, Kotest (테스트)  

---

## 폴더 구조
```plaintext
src/
└─ main/
   ├─ kotlin/com/kweather/
   │   ├─ domain/
   │   │   ├─ region/          # 지역(행정구역) 관리
   │   │   ├─ weather/         # 날씨/미세먼지/강수/예보
   │   │   ├─ senta/           # 체감온도
   │   │   ├─ airstagnation/   # 대기정체지수
   │   │   ├─ uvi/             # 자외선지수
   │   │   ├─ realtime/        # 실시간 미세먼지
   │   │   └─ forecast/        # 미세먼지 예보
   │   └─ global/              # 공통 유틸리티, 설정
   ├─ resources/
   │   ├─ templates/           # Thymeleaf 템플릿
   │   └─ static/              # 정적 리소스(JS, CSS)
   └─ ...
```

---

## 빌드 및 실행

### 환경 변수/설정  
`application.yml` 또는 환경변수에 공공 API 키, DB 정보 등 필수 설정이 필요합니다.

### 빌드  
```bash
./gradlew build
```

실행  
bash ./gradlew bootRun  
접속: http://localhost:8080/  

주요 도메인 설명  
1. 지역(Region) 관리  
- Entity/Repository/Service/Controller 구조로 시/도, 시/군/구, 읍/면/동, 리 등 계층적 지역정보 관리  
- RegionRestController: 시/도, 시/군/구, 읍/면/동 API 제공  
- /api/regions/sidos  
- /api/regions/sggs  
- /api/regions/umds  
- HierarchyService: 지역 계층 데이터 일괄 로드 및 캐싱  

2. 날씨/미세먼지/예보  
- GeneralWeatherService: 기상청 초단기예보, 미세먼지 예보, 실시간 미세먼지 API 연동 및 파싱  
- WeatherController: 메인 날씨 페이지 렌더링 및 모델 데이터 구성  
- HourlyRestController, PrecipitationController 등: 시간별 온도, 강수량 REST API 제공  

3. 체감온도/대기정체지수/자외선지수  
- SenTaIndexService, AirStagnationIndexService, UVIndexService에서 외부 API 연동 및 파싱 담당  
- Chart.js 기반 대시보드/차트 데이터 REST API 제공  

4. 프론트엔드 (템플릿/JS)  
- weather.html: 메인 날씨/예보/차트/팝업 UI (Thymeleaf)  
- global.js: 위치 선택, 차트, 팝업, AJAX 등 동적 기능  
- style.css: 반응형/모던 UI 스타일  

API / 비즈니스 로직 예시  
- 시간별 온도: /api/hourly-temperature  
- 강수량 차트: /api/precipitation  
- 체감온도 차트: /api/chart/temperature  
- 대기정체지수 차트: /api/airchart/air-stagnation  
- 지역 선택: /api/regions/sidos, /api/regions/sggs, /api/regions/umds  

DTO / Entity / Service / Controller 구조  
- DTO: AirStagnationIndexInfo, SenTaIndexInfo, UVIndexInfo, ForecastInfo, WeatherInfo 등  
- Entity: Region, Sido, Sgg, Umd, Ri, Weather  
- Service: GeneralWeatherService, SenTaIndexService, AirStagnationIndexService, UVIndexService  
- Controller: WeatherController, RegionRestController, HourlyRestController, PrecipitationController 등  

커스텀 / 공통 유틸리티  
- ApiClientUtility: 외부 API 요청/파싱 공통화  
- DateTimeUtils: 날짜/시간 변환  
- GeoUtil: 주소-좌표 변환  

기타 특징  
- 테스트: JUnit5, MockK, Kotest 기반 단위/통합 테스트  
- QueryDSL: 동적 쿼리 지원  
- Resilience4j: 외부 API 장애 대응  

참고사항  
- build.gradle.kts에 모든 의존성 명시  
- 환경별 DB/포트/API 키 등 별도 설정 필요  
- 공공 API 사용량 제한 및 정책 유의  

문의 / 기여  
- Pull Request, Issue 언제든 환영  
- 코드 및 문서 개선 제안 부담 없이 남길 것  


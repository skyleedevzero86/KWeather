package com.kweather.domain.region.service

import com.kweather.domain.region.repository.RegionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class RegionService(private val regionRepository: RegionRepository) {
    private val logger = LoggerFactory.getLogger(RegionService::class.java)

    fun getAllSidos(): List<String> {
        logger.info("모든 시도 조회 시작")
        return try {
            val sidos = regionRepository.findDistinctSidoNames()
                .mapNotNull { it?.split(" ")?.getOrNull(0) }
                .distinct()
                .sorted()

            if (sidos.isEmpty()) {
                logger.warn("데이터베이스에서 시/도 데이터를 찾을 수 없습니다. 기본 데이터를 반환합니다.")
                getDefaultSidos()
            } else {
                logger.info("조회된 시도 목록: $sidos")
                sidos
            }
        } catch (e: Exception) {
            logger.error("시/도 데이터 조회 중 오류 발생: ${e.message}", e)
            getDefaultSidos()
        }
    }

    fun getSggsBySido(sidoName: String): List<String> {
        logger.info("시군구 조회 시작 - 시도: $sidoName")
        return try {
            val normalizedSido = normalizeSido(sidoName)
            val sggs = regionRepository.findSggsBySido(normalizedSido)
                .mapNotNull { it?.split(" ")?.getOrNull(1) }
                .distinct()
                .sorted()

            if (sggs.isEmpty()) {
                logger.warn("데이터베이스에서 시/군/구 데이터를 찾을 수 없습니다. 기본 데이터를 반환합니다.")
                getDefaultSggs(normalizedSido)
            } else {
                logger.info("조회된 시군구 목록: $sggs")
                sggs
            }
        } catch (e: Exception) {
            logger.error("시/군/구 데이터 조회 중 오류 발생: ${e.message}", e)
            getDefaultSggs(normalizeSido(sidoName))
        }
    }

    fun getUmdsBySidoAndSgg(sidoName: String, sggName: String): List<String> {
        logger.info("읍면동 조회 시작 - 시도: $sidoName, 시군구: $sggName")
        return try {
            val normalizedSido = normalizeSido(sidoName)
            val umds = regionRepository.findUmdsBySidoAndSgg("$normalizedSido $sggName")
                .mapNotNull { it.locallowNm }
                .distinct()
                .sorted()

            if (umds.isEmpty()) {
                logger.warn("데이터베이스에서 읍/면/동 데이터를 찾을 수 없습니다. 기본 데이터를 반환합니다.")
                getDefaultUmds(normalizedSido, sggName)
            } else {
                logger.info("조회된 읍면동 목록: $umds")
                umds
            }
        } catch (e: Exception) {
            logger.error("읍/면/동 데이터 조회 중 오류 발생: ${e.message}", e)
            getDefaultUmds(normalizeSido(sidoName), sggName)
        }
    }

    private fun getDefaultSidos(): List<String> {
        return listOf(
            "서울특별시", "경기도", "인천광역시", "강원특별자치도", "충청북도", "충청남도",
            "대전광역시", "세종특별자치시", "전북특별자치도", "전라남도", "광주광역시",
            "경상북도", "경상남도", "대구광역시", "부산광역시", "울산광역시", "제주특별자치도"
        )
    }

    private fun getDefaultSggs(sido: String): List<String> {
        return when (sido) {
            "서울특별시" -> listOf("종로구", "중구", "용산구", "성동구", "광진구", "동대문구", "중랑구", "성북구", "강북구", "도봉구", "노원구", "은평구", "서대문구", "마포구", "양천구", "강서구", "구로구", "금천구", "영등포구", "동작구", "관악구", "서초구", "강남구", "송파구", "강동구")
            "경기도" -> listOf("수원시", "성남시", "의정부시", "안양시", "부천시", "광명시", "평택시", "과천시", "오산시", "시흥시", "군포시", "의왕시", "하남시", "용인시", "파주시", "이천시", "안성시", "김포시", "화성시", "광주시", "여주시", "양평군", "고양시", "의정부시", "동두천시", "가평군", "연천군")
            "인천광역시" -> listOf("중구", "동구", "미추홀구", "연수구", "남동구", "부평구", "계양구", "서구", "강화군", "옹진군")
            "강원특별자치도" -> listOf("춘천시", "원주시", "강릉시", "동해시", "태백시", "속초시", "삼척시", "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군", "양구군", "인제군", "고성군", "양양군")
            "충청북도" -> listOf("청주시", "충주시", "제천시", "보은군", "옥천군", "영동군", "증평군", "진천군", "괴산군", "음성군", "단양군")
            "충청남도" -> listOf("천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시", "당진시", "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군", "태안군")
            "대전광역시" -> listOf("동구", "중구", "서구", "유성구", "대덕구")
            "세종특별자치시" -> listOf("세종특별자치시")
            "전북특별자치도" -> listOf("전주시", "군산시", "익산시", "정읍시", "남원시", "김제시", "완주군", "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군")
            "전라남도" -> listOf("목포시", "여수시", "순천시", "나주시", "광양시", "담양군", "곡성군", "구례군", "고흥군", "보성군", "화순군", "장흥군", "강진군", "해남군", "영암군", "무안군", "함평군", "영광군", "장성군", "완도군", "진도군", "신안군")
            "광주광역시" -> listOf("동구", "서구", "남구", "북구", "광산구")
            "경상북도" -> listOf("포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시", "상주시", "문경시", "경산시", "군위군", "의성군", "청송군", "영양군", "영덕군", "청도군", "고령군", "성주군", "칠곡군", "예천군", "봉화군", "울진군", "울릉군")
            "경상남도" -> listOf("창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시", "양산시", "의령군", "함안군", "창녕군", "고성군", "남해군", "하동군", "산청군", "함양군", "거창군", "합천군")
            "대구광역시" -> listOf("중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군")
            "부산광역시" -> listOf("중구", "서구", "동구", "영도구", "부산진구", "동래구", "남구", "북구", "해운대구", "사하구", "금정구", "강서구", "연제구", "수영구", "사상구", "기장군")
            "울산광역시" -> listOf("중구", "남구", "동구", "북구", "울주군")
            "제주특별자치도" -> listOf("제주시", "서귀포시")
            else -> emptyList()
        }
    }

    private fun getDefaultUmds(sido: String, sgg: String): List<String> {
        return when (sido) {
            "서울특별시" -> when (sgg) {
                "종로구" -> listOf("청진동", "신문로1가", "신문로2가", "궁정동", "효자동", "창신동", "숭인동", "이화동", "혜화동", "명륜3가", "명륜4가", "완전동", "명륜1가", "명륜2가", "와룡동", "무악동", "교남동", "평창동", "부암동", "삼청동", "가회동", "종로1가", "종로2가", "종로3가", "종로4가", "종로5가", "종로6가", "수송동", "내수동", "사직동", "세종로")
                "중구" -> listOf("소공동", "회현동", "명동", "필동", "장충동", "광희동", "을지로1가", "을지로2가", "을지로3가", "을지로4가", "을지로5가", "을지로6가", "을지로7가", "신당동", "다산동", "약수동", "청구동", "신당5동", "동화동", "황학동", "중림동", "의주로1가", "의주로2가", "만리동1가", "만리동2가", "순화동", "봉래동1가", "봉래동2가", "남대문로1가", "남대문로2가", "남대문로3가", "남대문로4가", "남대문로5가", "남산동1가", "남산동2가", "남산동3가", "태평로1가", "태평로2가", "태평로3가", "태평로4가", "태평로5가", "태평로6가", "태평로7가", "태평로8가", "태평로9가", "태평로10가", "태평로11가", "태평로12가", "태평로13가", "태평로14가", "태평로15가", "태평로16가", "태평로17가", "태평로18가", "태평로19가", "태평로20가", "태평로21가", "태평로22가", "태평로23가", "태평로24가", "태평로25가", "태평로26가", "태평로27가", "태평로28가", "태평로29가", "태평로30가", "태평로31가", "태평로32가", "태평로33가", "태평로34가", "태평로35가", "태평로36가", "태평로37가", "태평로38가", "태평로39가", "태평로40가", "태평로41가", "태평로42가", "태평로43가", "태평로44가", "태평로45가", "태평로46가", "태평로47가", "태평로48가", "태평로49가", "태평로50가", "태평로51가", "태평로52가", "태평로53가", "태평로54가", "태평로55가", "태평로56가", "태평로57가", "태평로58가", "태평로59가", "태평로60가", "태평로61가", "태평로62가", "태평로63가", "태평로64가", "태평로65가", "태평로66가", "태평로67가", "태평로68가", "태평로69가", "태평로70가", "태평로71가", "태평로72가", "태평로73가", "태평로74가", "태평로75가", "태평로76가", "태평로77가", "태평로78가", "태평로79가", "태평로80가", "태평로81가", "태평로82가", "태평로83가", "태평로84가", "태평로85가", "태평로86가", "태평로87가", "태평로88가", "태평로89가", "태평로90가", "태평로91가", "태평로92가", "태평로93가", "태평로94가", "태평로95가", "태평로96가", "태평로97가", "태평로98가", "태평로99가", "태평로100가")
                else -> listOf("기본동")
            }
            else -> listOf("기본동")
        }
    }

    private fun normalizeSido(sido: String) = when (sido) {
        "서울" -> "서울특별시"
        "경기" -> "경기도"
        "인천" -> "인천광역시"
        "강원" -> "강원특별자치도"
        "충북" -> "충청북도"
        "충남" -> "충청남도"
        "대전" -> "대전광역시"
        "세종" -> "세종특별자치시"
        "전북" -> "전북특별자치도"
        "전남" -> "전라남도"
        "광주" -> "광주광역시"
        "경북" -> "경상북도"
        "경남" -> "경상남도"
        "대구" -> "대구광역시"
        "부산" -> "부산광역시"
        "울산" -> "울산광역시"
        "제주" -> "제주특별자치도"
        else -> sido
    }
}
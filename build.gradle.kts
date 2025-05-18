plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    //kotlin("plugin.jpa") version "1.9.25"
    ///kotlin("kapt") version "1.9.25"
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(19)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 웹 MVC
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // WebFlux
    //implementation("org.springframework.boot:spring-boot-starter-webflux")

    // JPA
    //implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Thymeleaf 템플릿 엔진
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")

    // Kotlin Reflection
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Kotlin 표준 라이브러리
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Kotlin 테스트
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Spring Boot 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Reactor 테스트
    testImplementation("io.projectreactor:reactor-test")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")

    // JUnit 플랫폼 런처
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mock 라이브러리
    testImplementation("io.mockk:mockk:1.13.5")

    // Kotest
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")

    // 클래스 타입 처리용
    implementation("com.fasterxml:classmate:1.5.1")

    //Lombok
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // 웹 의존성
    implementation("org.webjars:bootstrap:5.3.2")
    implementation("org.webjars:jquery:3.7.1")

    // QueryDSL
    //implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    //kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")

    // 개발 도구
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

/*
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
*/

tasks.withType<Test> {
    useJUnitPlatform()
}

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.ritense"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.4"))
    implementation(platform("org.apache.camel.springboot:camel-spring-boot-dependencies:4.10.3")) // BOM

    implementation("org.apache.camel.springboot:camel-spring-boot")
    implementation("org.apache.camel.springboot:camel-direct-starter")
    implementation("org.apache.camel.springboot:camel-netty-http-starter")
    implementation("org.apache.camel.springboot:camel-platform-http-starter")
    implementation("org.apache.camel.springboot:camel-jackson-starter")
    implementation("org.apache.camel.springboot:camel-jacksonxml-starter")
    implementation("org.apache.camel.springboot:camel-http-starter")
    implementation("org.apache.camel.springboot:camel-rest-starter")
    implementation("org.apache.camel.springboot:camel-openapi-java-starter")
    implementation("org.apache.camel:camel-yaml-dsl")
    implementation("org.apache.camel:camel-rest-openapi")
    implementation("org.apache.camel:camel-jq")

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.apache.camel:camel-test-spring-junit5")
    testImplementation("org.apache.camel:camel-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

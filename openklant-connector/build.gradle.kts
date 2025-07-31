plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    id("io.spring.dependency-management") version "1.1.7"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.4"))
    implementation(platform("org.apache.camel.springboot:camel-spring-boot-dependencies:4.13.0"))

    implementation("org.apache.camel.springboot:camel-spring-boot")
    implementation("org.apache.camel.springboot:camel-direct-starter")
    implementation("org.apache.camel.springboot:camel-rest-starter")
    implementation("org.apache.camel.springboot:camel-openapi-java-starter")
    implementation("org.apache.camel:camel-rest-openapi")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

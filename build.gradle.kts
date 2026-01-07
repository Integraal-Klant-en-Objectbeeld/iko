import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    base
    id("co.uzzu.dotenv.gradle") version "4.0.0"
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
    kotlin("plugin.allopen") version "2.2.0"
    id("org.springframework.boot") version "3.5.8"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("com.avast.gradle.docker-compose") version "0.17.20"
}

group = "com.ritense"

tasks.bootJar {
    archiveBaseName.set("iko")
    archiveVersion.set("") // removes version
    archiveClassifier.set("") // removes classifier
}

springBoot {
    buildInfo()
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.4"))
    implementation(platform("org.apache.camel.springboot:camel-spring-boot-dependencies:4.13.0")) // BOM

    implementation("org.apache.camel.springboot:camel-spring-boot")
    implementation("org.apache.camel.springboot:camel-direct-starter")
    implementation("org.apache.camel.springboot:camel-netty-http-starter")
    implementation("org.apache.camel.springboot:camel-platform-http-starter")
    implementation("org.apache.camel.springboot:camel-jackson-starter")
    implementation("org.apache.camel.springboot:camel-spring-security-starter")
    implementation("org.apache.camel.springboot:camel-jacksonxml-starter")
    implementation("org.apache.camel.springboot:camel-http-starter")
    implementation("org.apache.camel.springboot:camel-rest-starter")
    implementation("org.apache.camel.springboot:camel-openapi-java-starter")
    implementation("org.apache.camel.springboot:camel-groovy-starter")
    implementation("org.apache.camel:camel-management")
    implementation("org.apache.camel:camel-yaml-dsl")
    implementation("org.apache.camel:camel-rest-openapi")
    implementation("org.apache.camel:camel-jq")
    implementation("org.apache.camel:camel-bean")

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.4.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Database
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Redis Cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("redis.clients:jedis")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // Actuator & Metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

    // developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.apache.camel:camel-test-spring-junit5")
    testImplementation("org.apache.camel:camel-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<BootRun> {
    environment = env.allVariables()
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("integration")
    }
    mustRunAfter(tasks.test)

    environment.putAll(env.allVariables())
}

tasks.named("check") {
    dependsOn(integrationTest)
}

dockerCompose {
    setProjectName(name) // uses projectRoot.name as the container group name
    stopContainers.set(true) // doesn't call `docker-compose down` if set to false; default is true
    removeContainers.set(false) // containers are retained upon composeDown for persistent storage

    createNested("testConfiguration").apply {
        isRequiredBy(tasks.named("integrationTest"))
        setProjectName("Iko-test")
        useComposeFiles.set(listOf("docker-compose-integration-test.yaml"))
        removeVolumes.set(true)
    }
}
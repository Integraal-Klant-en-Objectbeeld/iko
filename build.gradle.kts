import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    base
    alias(libs.plugins.dotenv)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.docker.compose)
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

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    mockitoAgent(libs.mockito.core) { isTransitive = false }
    implementation(platform(libs.spring.boot.dependencies))
    implementation(platform(libs.camel.spring.boot.dependencies)) // BOM

    implementation(libs.camel.spring.boot)
    implementation(libs.camel.direct.starter)
    implementation(libs.camel.netty.http.starter)
    implementation(libs.camel.platform.http.starter)
    implementation(libs.camel.jackson.starter)
    implementation(libs.camel.spring.security.starter)
    implementation(libs.camel.jacksonxml)
    implementation(libs.camel.http.starter)
    implementation(libs.camel.rest.starter)
    implementation(libs.camel.openapi.java.starter)
    implementation(libs.camel.groovy.starter)
    implementation(libs.camel.management)
    implementation(libs.camel.yaml.dsl)
    implementation(libs.camel.rest.openapi)
    implementation(libs.camel.jq)
    implementation(libs.camel.bean)

    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.thymeleaf.layout.dialect)
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.api)
    implementation(libs.jjwt.jackson)

    // Database
    implementation(libs.flyway.database.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.postgresql)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)

    // Redis Cache
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.jedis)

    // Security
    implementation(libs.spring.boot.starter.oauth2.resourceServer)
    implementation(libs.spring.boot.starter.oauth2.client)

    // Actuator & Metrics
    implementation(libs.spring.boot.starter.actuator)
    runtimeOnly(libs.micrometer.registry.prometheus)

    // Logging
    implementation(libs.kotlin.logging)

    // developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.reactor)

    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "com.vaadin.external.google", module = "android-json")
    }
    testImplementation(libs.reactor.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.camel.test.spring.junit5)
    testImplementation(libs.camel.test.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockwebserver)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<BootRun> {
    environment = env.allVariables()
}

tasks.named<Test>("test") {
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
    useJUnitPlatform {
        excludeTags("integration")
    }
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    jvmArgs("-javaagent:${mockitoAgent.asPath}")

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
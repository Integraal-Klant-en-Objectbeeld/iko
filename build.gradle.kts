import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.os.Os.FAMILY_MAC
import org.apache.tools.ant.taskdefs.condition.Os
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
    alias(libs.plugins.spotless)
    alias(libs.plugins.docker.compose)
    alias(libs.plugins.sonarqube)
    jacoco
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

    // Platforms
    implementation(platform(libs.spring.boot.dependencies))
    implementation(platform(libs.camel.spring.boot.dependencies))

    // Spring Boot Starters
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.thymeleaf.layout.dialect)

    // Camel
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

    // Database
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // Redis Cache
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.jedis)

    // Security
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resourceServer)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.jjwt.api)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.jackson)

    // Actuator & Metrics
    implementation(libs.spring.boot.starter.actuator)
    runtimeOnly(libs.micrometer.registry.prometheus)

    // Logging
    implementation(libs.kotlin.logging)

    // Kotlin
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.jackson.module.kotlin)

    // Testing
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = libs.androidJson.get().group, module = libs.androidJson.get().name)
    }
    testImplementation(libs.spring.security.test)
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
        noRecreate.set(true)
        removeContainers.set(true)

        if (Os.isFamily(FAMILY_MAC)) {
            println("Configure docker compose plugin for macOs")
            executable = "/usr/local/bin/docker-compose"
            dockerExecutable = "/usr/local/bin/docker"
        }
    }
}

spotless {
    val prettierConfig = mapOf("tabWidth" to 4)

    kotlin {
        ktlint()
        licenseHeaderFile("templates/licenseHeaderFile.kt.template")
    }
    kotlinGradle {
        ktlint()
        licenseHeaderFile("templates/licenseHeaderFile.kts.template", "((?!\\/\\/)|\\w.*)")
    }
    css {
        target("src/**/*.css")
        prettier().config(prettierConfig)
        licenseHeaderFile("templates/licenseHeaderFile.css.template", "(\\/\\*.*\\*\\/|.*\\{)")
    }
    format("html", {
        target("src/**/*.html")
        prettier().config(prettierConfig)
        licenseHeaderFile("templates/licenseHeaderFile.html.template", "(<(?!!--).*>?)")
    })
}

sonar {
    properties {
        property("sonar.projectKey", "iko")
        property("sonar.organization", "integraal-klant-en-objectbeeld")
        property("sonar.token", System.getenv("SONAR_TOKEN"))
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml,${layout.buildDirectory.get()}/reports/jacoco/integrationTest/jacocoTestReport.xml",
        )
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoIntegrationTestReport = tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
    group = "verification"
    description = "Generates Jacoco coverage reports for the integrationTest task."
    dependsOn(integrationTest)
    executionData(integrationTest.get())
    sourceDirectories.setFrom(sourceSets["main"].allSource.srcDirs)
    classDirectories.setFrom(sourceSets["main"].output)
    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/integrationTest/jacocoTestReport.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/integrationTest/html"))
    }
}
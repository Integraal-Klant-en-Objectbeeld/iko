plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.4"))
    implementation(platform("org.apache.camel.springboot:camel-spring-boot-dependencies:4.13.0"))

    implementation("org.apache.camel.springboot:camel-spring-boot")
    implementation("org.apache.camel.springboot:camel-direct-starter")
    implementation("org.apache.camel.springboot:camel-jackson-starter")
    implementation("org.apache.camel.springboot:camel-spring-security-starter")
    implementation("org.apache.camel.springboot:camel-rest-starter")
    implementation("org.apache.camel:camel-rest-openapi")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.apache.camel:camel-test-spring-junit5")
    testImplementation("org.apache.camel:camel-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
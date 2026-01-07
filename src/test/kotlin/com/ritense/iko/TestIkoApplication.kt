package com.ritense.iko

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.test.context.TestConfiguration

@SpringBootApplication
internal class TestIkoApplication

@TestConfiguration
internal class IkoTestConfiguration {
    // Extra Beans
}

fun main(args: Array<String>) {
    runApplication<TestIkoApplication>(*args)
}
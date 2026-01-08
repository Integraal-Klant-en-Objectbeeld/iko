package com.ritense.iko

import org.apache.camel.test.spring.junit5.CamelSpringBootTest
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [
        TestIkoApplication::class,
        IkoTestConfiguration::class,
    ],
)
@CamelSpringBootTest
@ActiveProfiles("test") // Will merge test yml with main yml
@Tag("integration")
abstract class BaseIntegrationTest
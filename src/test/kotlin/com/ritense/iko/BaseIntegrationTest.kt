package com.ritense.iko

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [
        TestIkoApplication::class,
    ]
)
@ActiveProfiles("test") // Will merge test yml with main yml
@Tag("integration")
abstract class BaseIntegrationTest
package com.ritense.iko

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(
    classes = [
        TestIkoApplication::class,
        // ClientAutoConfigure::class, <-- place here your client auto configure classes
    ]
)
@ExtendWith(value = [SpringExtension::class])
@Tag("integration")
abstract class BaseIntegrationTest
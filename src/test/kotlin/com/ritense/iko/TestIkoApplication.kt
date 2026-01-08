package com.ritense.iko

import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.jwt.JwtDecoder

@SpringBootApplication
internal class TestIkoApplication

@TestConfiguration
internal class IkoTestConfiguration {
    @Bean
    @Primary
    fun jwtDecoder(): JwtDecoder = mock(JwtDecoder::class.java)

    @Bean
    @Primary
    fun clientRegistrationRepository(): ClientRegistrationRepository = mock(ClientRegistrationRepository::class.java)
}

fun main(args: Array<String>) {
    runApplication<TestIkoApplication>(*args)
}
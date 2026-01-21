/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko

import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.jwt.JwtDecoder

@SpringBootApplication(
    exclude = [
        OAuth2ClientAutoConfiguration::class,
        OAuth2ResourceServerAutoConfiguration::class,
    ],
)
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
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

package com.ritense.iko.connectors.camel

import com.ritense.iko.cache.processor.TokenCacheProcessor
import com.ritense.iko.cache.service.CacheService
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.support.DefaultExchange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.Duration

class TokenCacheProcessorTest {
    private val cacheService: CacheService = mock()
    private val processor = TokenCacheProcessor(cacheService)
    private lateinit var camelContext: DefaultCamelContext

    private val fullConfig = mapOf(
        "tokenUrl" to "http://keycloak/realms/test/protocol/openid-connect/token",
        "clientId" to "iko-brp-client",
        "audience" to "haalcentraal",
    )

    @BeforeEach
    fun setup() {
        camelContext = DefaultCamelContext()
        camelContext.start()
        whenever(cacheService.hashString(any())).thenReturn("HASH")
    }

    @AfterEach
    fun teardown() {
        camelContext.stop()
    }

    private fun newExchange(): Exchange = DefaultExchange(camelContext)

    @Nested
    inner class Lookup {
        @Test
        fun `cache hit sets accessToken variable`() {
            val exchange = newExchange()
            exchange.setVariable("configProperties", fullConfig)
            whenever(cacheService.get("token:keycloak:HASH")).thenReturn("cached-token")

            processor.lookup(exchange)

            assertThat(exchange.getVariable("accessToken", String::class.java))
                .isEqualTo("cached-token")
        }

        @Test
        fun `cache miss leaves accessToken variable unset`() {
            val exchange = newExchange()
            exchange.setVariable("configProperties", fullConfig)
            whenever(cacheService.get("token:keycloak:HASH")).thenReturn(null)

            processor.lookup(exchange)

            assertThat(exchange.getVariable("accessToken")).isNull()
        }

        @Test
        fun `missing configProperties variable is a no-op`() {
            val exchange = newExchange()

            processor.lookup(exchange)

            assertThat(exchange.getVariable("accessToken")).isNull()
            verifyNoInteractions(cacheService)
        }

        @Test
        fun `blank required config keys make lookup a no-op`() {
            val exchange = newExchange()
            exchange.setVariable(
                "configProperties",
                mapOf("tokenUrl" to "", "clientId" to "x", "audience" to "y"),
            )

            processor.lookup(exchange)

            assertThat(exchange.getVariable("accessToken")).isNull()
            verifyNoInteractions(cacheService)
        }

        @Test
        fun `cache key includes hashed tokenUrl clientId and audience`() {
            val exchange = newExchange()
            exchange.setVariable("configProperties", fullConfig)
            whenever(cacheService.get(any())).thenReturn(null)

            processor.lookup(exchange)

            verify(cacheService).hashString(
                eq("http://keycloak/realms/test/protocol/openid-connect/token|iko-brp-client|haalcentraal"),
            )
            verify(cacheService).get("token:keycloak:HASH")
        }
    }

    @Nested
    inner class Store {
        @Test
        fun `well-formed response writes to cache and sets variable`() {
            val exchange = newExchange()
            exchange.setVariable("configProperties", fullConfig)
            exchange.message.body = mapOf("access_token" to "new-token", "expires_in" to 300)

            processor.store(exchange)

            val ttl = argumentCaptor<Duration>()
            verify(cacheService).put(
                eq("token:keycloak:HASH"),
                eq("new-token"),
                ttl.capture(),
            )
            assertThat(ttl.firstValue).isEqualTo(Duration.ofSeconds(270))
            assertThat(exchange.getVariable("accessToken", String::class.java))
                .isEqualTo("new-token")
        }

        @Test
        fun `ttl is expires_in times 0_9 rounded down`() {
            val exchange = newExchange()
            exchange.setVariable("configProperties", fullConfig)
            exchange.message.body = mapOf("access_token" to "t", "expires_in" to 60)

            processor.store(exchange)

            val ttl = argumentCaptor<Duration>()
            verify(cacheService).put(any(), any(), ttl.capture())
            assertThat(ttl.firstValue).isEqualTo(Duration.ofSeconds(54))
        }

        @Test
        fun `ttl clamps to at least one second`() {
            val exchange = newExchange()
            exchange.setVariable("configProperties", fullConfig)
            exchange.message.body = mapOf("access_token" to "t", "expires_in" to 1)

            processor.store(exchange)

            val ttl = argumentCaptor<Duration>()
            verify(cacheService).put(any(), any(), ttl.capture())
            assertThat(ttl.firstValue).isEqualTo(Duration.ofSeconds(1))
        }

        @Test
        fun `missing access_token throws`() {
            val exchange = newExchange()
            exchange.setVariable("configProperties", fullConfig)
            exchange.message.body = mapOf("expires_in" to 300)

            assertThrows<IllegalStateException> { processor.store(exchange) }
        }

        @Test
        fun `missing expires_in throws`() {
            val exchange = newExchange()
            exchange.setVariable("configProperties", fullConfig)
            exchange.message.body = mapOf("access_token" to "t")

            assertThrows<IllegalArgumentException> { processor.store(exchange) }
        }

        @Test
        fun `zero expires_in throws`() {
            val exchange = newExchange()
            exchange.setVariable("configProperties", fullConfig)
            exchange.message.body = mapOf("access_token" to "t", "expires_in" to 0)

            assertThrows<IllegalArgumentException> { processor.store(exchange) }
        }

        @Test
        fun `missing configProperties is a no-op`() {
            val exchange = newExchange()
            exchange.message.body = mapOf("access_token" to "t", "expires_in" to 300)

            processor.store(exchange)

            assertThat(exchange.getVariable("accessToken")).isNull()
            verifyNoInteractions(cacheService)
        }
    }
}
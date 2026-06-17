/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
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

package com.ritense.iko.logging

import com.ritense.iko.BaseIntegrationTest
import com.ritense.iko.logging.repository.LoggingEventRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.annotation.Transactional

@Transactional
internal class LoggingAppenderIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var loggingEventRepository: LoggingEventRepository

    @Value("\${spring.datasource.url}")
    private lateinit var jdbcUrl: String

    @Value("\${spring.datasource.username}")
    private lateinit var username: String

    @Value("\${spring.datasource.password}")
    private lateinit var password: String

    @Test
    fun `schema migration creates logging_event table and repository findAll does not throw`() {
        // The Flyway migration must have run; simply calling findAll verifies the table exists
        val events = loggingEventRepository.findAll()
        // No assertion on count — the table may be empty if DB appender is disabled in test env
        assertThat(events).isNotNull
    }

    @Test
    fun `LogDbReadyFilter tableExists returns true when migration has run`() {
        val filter = LogDbReadyFilter()
        filter.jdbcUrl = jdbcUrl
        filter.username = username
        filter.password = password
        filter.driverClassName = "org.postgresql.Driver"

        // Access the private tableExists method via reflection
        val method = LogDbReadyFilter::class.java.getDeclaredMethod("tableExists")
        method.isAccessible = true
        val tableExists = method.invoke(filter) as Boolean

        assertThat(tableExists)
            .withFailMessage("tableExists() should return true after Flyway migration has run")
            .isTrue()
    }
}
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
import com.ritense.iko.logging.domain.LoggingEvent
import com.ritense.iko.logging.domain.LoggingEventException
import com.ritense.iko.logging.domain.LoggingEventExceptionId
import com.ritense.iko.logging.domain.LoggingEventProperty
import com.ritense.iko.logging.domain.LoggingEventPropertyId
import com.ritense.iko.logging.repository.LoggingEventRepository
import jakarta.persistence.EntityManager
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@AutoConfigureMockMvc
@Transactional
internal class LogControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var loggingEventRepository: LoggingEventRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private fun insertEvent(
        eventId: Long,
        message: String,
        level: String = "INFO",
        timestampMs: Long = Instant.now().toEpochMilli(),
    ): LoggingEvent {
        entityManager.createNativeQuery(
            """INSERT INTO logging_event
               (event_id, timestmp, formatted_message, logger_name, level_string,
                thread_name, reference_flag, caller_filename, caller_class, caller_method, caller_line)
               VALUES (:id, :ts, :msg, 'com.test.Logger', :level,
                       'test-thread', 0, 'Test.kt', 'com.test.Logger', 'test', '0001')""",
        ).setParameter("id", eventId)
            .setParameter("ts", timestampMs)
            .setParameter("msg", message)
            .setParameter("level", level)
            .executeUpdate()
        entityManager.flush()
        return loggingEventRepository.findById(eventId).orElseThrow()
    }

    private fun insertProperty(
        eventId: Long,
        key: String,
        value: String,
    ) {
        entityManager.createNativeQuery(
            "INSERT INTO logging_event_property (event_id, mapped_key, mapped_value) VALUES (:id, :k, :v)",
        ).setParameter("id", eventId)
            .setParameter("k", key)
            .setParameter("v", value)
            .executeUpdate()
        entityManager.flush()
    }

    private fun insertException(
        eventId: Long,
        i: Short,
        traceLine: String,
    ) {
        entityManager.createNativeQuery(
            "INSERT INTO logging_event_exception (event_id, i, trace_line) VALUES (:id, :i, :line)",
        ).setParameter("id", eventId)
            .setParameter("i", i)
            .setParameter("line", traceLine)
            .executeUpdate()
        entityManager.flush()
    }

    @Test
    @WithMockUser(authorities = ["ROLE_ADMIN"])
    fun `GET filter with message param returns matching rows`() {
        insertEvent(9001L, "Hello World from test", "INFO")
        insertEvent(9002L, "Other message entirely", "DEBUG")

        mockMvc
            .perform(
                get("/admin/logs/filter")
                    .param("message", "Hello World")
                    .header("Hx-Request", "true")
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(content().string(containsString("Hello World from test")))
    }

    @Test
    @WithMockUser(authorities = ["ROLE_ADMIN"])
    fun `GET filter with level WARN returns WARN and ERROR rows only`() {
        insertEvent(9003L, "Info event", "INFO")
        insertEvent(9004L, "Warn event", "WARN")
        insertEvent(9005L, "Error event", "ERROR")

        val result =
            mockMvc
                .perform(
                    get("/admin/logs/filter")
                        .param("level", "WARN")
                        .header("Hx-Request", "true")
                        .with(csrf()),
                ).andExpect(status().isOk)
                .andReturn()

        val body = result.response.contentAsString
        assert(body.contains("Warn event")) { "Expected 'Warn event' in response" }
        assert(body.contains("Error event")) { "Expected 'Error event' in response" }
        assert(!body.contains("Info event")) { "Did not expect 'Info event' in response" }
    }

    @Test
    @WithMockUser(authorities = ["ROLE_ADMIN"])
    fun `GET filter with property key-value returns matching rows`() {
        insertEvent(9006L, "Correlated event", "INFO")
        insertProperty(9006L, "correlationId", "abc-123")
        insertEvent(9007L, "Unrelated event", "INFO")

        mockMvc
            .perform(
                get("/admin/logs/filter")
                    .param("properties[0].key", "correlationId")
                    .param("properties[0].value", "abc-123")
                    .header("Hx-Request", "true")
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(content().string(containsString("Correlated event")))
    }

    @Test
    @WithMockUser(authorities = ["ROLE_ADMIN"])
    fun `GET detail endpoint returns modal fragment with stacktrace`() {
        insertEvent(9008L, "Event with stacktrace", "ERROR")
        insertException(9008L, 0, "com.example.SomeException: something went wrong")
        insertException(9008L, 1, "\tat com.example.Foo.bar(Foo.kt:42)")

        mockMvc
            .perform(
                get("/admin/logs/9008")
                    .header("Hx-Request", "true")
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(content().string(containsString("Event with stacktrace")))
            .andExpect(content().string(containsString("com.example.SomeException")))
    }

    @Test
    @WithMockUser(authorities = ["ROLE_ADMIN"])
    fun `GET logs list fragment returns 200`() {
        mockMvc
            .perform(
                get("/admin/logs")
                    .header("Hx-Request", "true")
                    .with(csrf()),
            ).andExpect(status().isOk)
    }

    @Test
    @WithMockUser(authorities = ["ROLE_ADMIN"])
    fun `GET logs shows disabled banner when logging db disabled`() {
        mockMvc
            .perform(
                get("/admin/logs")
                    .header("Hx-Request", "true")
                    .with(csrf()),
            ).andExpect(status().isOk)
            .andExpect(content().string(containsString("Logging disabled")))
            .andExpect(content().string(containsString("No new recordings are captured.")))
    }
}
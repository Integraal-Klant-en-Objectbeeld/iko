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
import com.ritense.iko.logging.service.LoggingEventDeletionService
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Transactional
internal class LoggingEventDeletionServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var deletionService: LoggingEventDeletionService

    @Autowired
    private lateinit var repository: LoggingEventRepository

    @Autowired
    private lateinit var properties: LoggingProperties

    @Autowired
    private lateinit var entityManager: EntityManager

    private var originalRetention: Duration = Duration.ZERO

    @BeforeEach
    fun captureRetention() {
        originalRetention = properties.retention
    }

    @AfterEach
    fun restoreRetention() {
        properties.retention = originalRetention
    }

    private fun insertEvent(
        eventId: Long,
        timestampMs: Long,
        message: String = "test message",
        level: String = "INFO",
    ) {
        entityManager.createNativeQuery(
            """INSERT INTO logging_event
               (event_id, timestmp, formatted_message, logger_name, level_string,
                thread_name, reference_flag, caller_filename, caller_class, caller_method, caller_line)
               VALUES (:id, :ts, :msg, 'com.test.Logger', :level,
                       'test-thread', 0, 'Test.kt', 'com.test.Logger', 'deleteOldLogs', '0001')""",
        ).setParameter("id", eventId)
            .setParameter("ts", timestampMs)
            .setParameter("msg", message)
            .setParameter("level", level)
            .executeUpdate()
        entityManager.flush()
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
    fun `deleteOldLogs removes events and children older than retention and keeps recent rows`() {
        // Use a 1-minute retention so that events 2 minutes old are pruned
        properties.retention = Duration.ofMinutes(1)

        val oldTimestamp = Instant.now().minus(2, ChronoUnit.MINUTES).toEpochMilli()
        val recentTimestamp = Instant.now().toEpochMilli()

        // Old event with a property and an exception child
        insertEvent(8001L, oldTimestamp, "old event", "ERROR")
        insertProperty(8001L, "correlationId", "old-correlation")
        insertException(8001L, 0, "com.example.OldException: old error")

        // Recent event with a property child
        insertEvent(8002L, recentTimestamp, "recent event", "INFO")
        insertProperty(8002L, "correlationId", "recent-correlation")

        entityManager.clear()

        deletionService.deleteOldLogs()

        entityManager.flush()
        entityManager.clear()

        val remaining = repository.findAll()
        val ids = remaining.map { it.eventId }

        assertThat(ids)
            .withFailMessage("Recent event 8002 should still exist after deletion")
            .contains(8002L)

        assertThat(ids)
            .withFailMessage("Old event 8001 should have been deleted")
            .doesNotContain(8001L)

        // Verify orphaned property and exception rows were also deleted
        val propertyCount =
            (
                entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM logging_event_property WHERE event_id = 8001")
                    .singleResult as Number
                ).toInt()
        assertThat(propertyCount)
            .withFailMessage("Property rows for old event 8001 should have been deleted")
            .isEqualTo(0)

        val exceptionCount =
            (
                entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM logging_event_exception WHERE event_id = 8001")
                    .singleResult as Number
                ).toInt()
        assertThat(exceptionCount)
            .withFailMessage("Exception rows for old event 8001 should have been deleted")
            .isEqualTo(0)
    }

    @Test
    fun `deleteOldLogs with no old events does not delete any rows`() {
        // Use a 1-minute retention
        properties.retention = Duration.ofMinutes(1)

        val recentTimestamp = Instant.now().toEpochMilli()
        insertEvent(8003L, recentTimestamp, "fresh event", "INFO")

        entityManager.clear()

        deletionService.deleteOldLogs()

        entityManager.flush()
        entityManager.clear()

        val remaining = repository.findAll()
        val ids = remaining.map { it.eventId }
        assertThat(ids)
            .withFailMessage("Fresh event 8003 should not have been deleted")
            .contains(8003L)
    }
}
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

package com.ritense.iko.logging.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Immutable
@Table(name = "logging_event")
internal class LoggingEvent(
    @Id @Column(name = "event_id") val eventId: Long,
    @Column(name = "timestmp") val timestamp: Long,
    @Column(name = "formatted_message") val formattedMessage: String,
    @Column(name = "logger_name") val loggerName: String,
    @Column(name = "level_string") val level: String,
    @Column(name = "thread_name") val threadName: String?,
    @Column(name = "caller_class") val callerClass: String,
    @OneToMany(fetch = LAZY)
    @JoinColumn(name = "event_id", insertable = false, updatable = false)
    val properties: List<LoggingEventProperty> = emptyList(),
    @OneToMany(fetch = LAZY)
    @JoinColumn(name = "event_id", insertable = false, updatable = false)
    @OrderBy("id.i ASC")
    val exceptions: List<LoggingEventException> = emptyList(),
) {
    val timestampDateTime: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())

    val stacktrace: String
        get() = exceptions.joinToString("\n") { it.traceLine }
}
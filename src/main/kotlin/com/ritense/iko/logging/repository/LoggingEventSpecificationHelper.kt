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

package com.ritense.iko.logging.repository

import com.ritense.iko.logging.domain.LoggingEvent
import com.ritense.iko.logging.domain.LoggingEventProperty
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDateTime
import java.time.ZoneId

internal object LoggingEventSpecificationHelper {
    private val LEVELS = listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR")

    fun byLikeFormattedMessage(text: String) = Specification<LoggingEvent> { root, _, cb ->
        cb.like(cb.lower(root.get("formattedMessage")), "%${text.lowercase()}%")
    }

    fun byMinimumLevel(level: String) = Specification<LoggingEvent> { root, _, cb ->
        val allowed = LEVELS.drop(LEVELS.indexOf(level).coerceAtLeast(0))
        root.get<String>("level").`in`(allowed)
    }

    fun byNewerThan(dt: LocalDateTime) = Specification<LoggingEvent> { root, _, cb ->
        cb.greaterThanOrEqualTo(root.get("timestamp"), dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    fun byOlderThan(dt: LocalDateTime) = Specification<LoggingEvent> { root, _, cb ->
        cb.lessThanOrEqualTo(root.get("timestamp"), dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }

    fun byProperty(
        key: String,
        value: String,
    ) = Specification<LoggingEvent> { root, query, cb ->
        val join = root.join<LoggingEvent, LoggingEventProperty>("properties")
        query.distinct(true)
        cb.and(
            cb.equal(join.get<Any>("id").get<String>("mappedKey"), key),
            cb.like(cb.lower(join.get("mappedValue")), "%${value.lowercase()}%"),
        )
    }
}
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

package com.ritense.iko.logging.service

import com.ritense.iko.logging.domain.LoggingEvent
import com.ritense.iko.logging.repository.LoggingEventRepository
import com.ritense.iko.logging.repository.LoggingEventSpecificationHelper.byLikeFormattedMessage
import com.ritense.iko.logging.repository.LoggingEventSpecificationHelper.byMinimumLevel
import com.ritense.iko.logging.repository.LoggingEventSpecificationHelper.byNewerThan
import com.ritense.iko.logging.repository.LoggingEventSpecificationHelper.byOlderThan
import com.ritense.iko.logging.repository.LoggingEventSpecificationHelper.byProperty
import com.ritense.iko.mvc.model.LoggingFilter
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class LoggingEventService(
    private val repository: LoggingEventRepository,
) {
    @Transactional(readOnly = true)
    fun search(
        filter: LoggingFilter,
        pageable: Pageable,
    ): Page<LoggingEvent> {
        val specs =
            buildList {
                filter.message?.takeIf { it.isNotBlank() }?.let { add(byLikeFormattedMessage(it)) }
                filter.level?.takeIf { it.isNotBlank() }?.let { add(byMinimumLevel(it)) }
                filter.from()?.let { add(byNewerThan(it)) }
                filter.to()?.let { add(byOlderThan(it)) }
                filter.properties.filter { it.key.isNotBlank() }.forEach { add(byProperty(it.key, it.value)) }
            }
        val spec = specs.reduceOrNull(Specification<LoggingEvent>::and)
        return repository.findAll(spec, pageable)
    }

    @Transactional(readOnly = true)
    fun findById(eventId: Long): LoggingEvent = repository.findById(eventId).orElseThrow { NoSuchElementException("Log event not found: $eventId") }
        .also {
            // Initialise LAZY collections inside the tx; OSIV is disabled so the
            // detail modal would otherwise hit LazyInitializationException.
            it.properties.size
            it.exceptions.size
        }
}
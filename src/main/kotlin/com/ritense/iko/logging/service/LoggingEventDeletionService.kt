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

import com.ritense.iko.logging.LoggingProperties
import com.ritense.iko.logging.repository.LoggingEventRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
internal class LoggingEventDeletionService(
    private val repository: LoggingEventRepository,
    private val properties: LoggingProperties,
) {
    @Scheduled(cron = "\${iko.logging.deletionCron}")
    @Transactional
    fun deleteOldLogs() {
        val threshold =
            Instant.now()
                .minus(properties.retention)
                .toEpochMilli()
        repository.deletePropertiesOlderThan(threshold)
        repository.deleteExceptionsOlderThan(threshold)
        repository.deleteEventsOlderThan(threshold)
    }
}
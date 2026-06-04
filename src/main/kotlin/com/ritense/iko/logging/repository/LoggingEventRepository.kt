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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface LoggingEventRepository :
    JpaRepository<LoggingEvent, Long>,
    JpaSpecificationExecutor<LoggingEvent> {

    @Modifying
    @Query("DELETE FROM LoggingEventProperty p WHERE p.id.eventId IN (SELECT e.eventId FROM LoggingEvent e WHERE e.timestamp < :threshold)")
    fun deletePropertiesOlderThan(
        @Param("threshold") threshold: Long,
    ): Int

    @Modifying
    @Query("DELETE FROM LoggingEventException x WHERE x.id.eventId IN (SELECT e.eventId FROM LoggingEvent e WHERE e.timestamp < :threshold)")
    fun deleteExceptionsOlderThan(
        @Param("threshold") threshold: Long,
    ): Int

    @Modifying
    @Query("DELETE FROM LoggingEvent e WHERE e.timestamp < :threshold")
    fun deleteEventsOlderThan(
        @Param("threshold") threshold: Long,
    ): Int
}
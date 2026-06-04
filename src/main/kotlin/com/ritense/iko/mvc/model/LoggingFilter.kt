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

package com.ritense.iko.mvc.model

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class PropertyFilter(
    val key: String = "",
    val value: String = "",
)

data class LoggingFilter(
    val message: String? = null,
    val level: String? = null,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val fromDate: LocalDate? = null,
    val fromTime: String? = null,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val toDate: LocalDate? = null,
    val toTime: String? = null,
    val properties: List<PropertyFilter> = emptyList(),
) {
    fun from(): LocalDateTime? {
        val date = fromDate ?: return null
        val time = fromTime?.takeIf { it.isNotBlank() }?.let { parseTime(it) } ?: LocalTime.MIDNIGHT
        return LocalDateTime.of(date, time)
    }

    fun to(): LocalDateTime? {
        val date = toDate ?: return null
        val time = toTime?.takeIf { it.isNotBlank() }?.let { parseTime(it) } ?: LocalTime.of(23, 59, 59)
        return LocalDateTime.of(date, time)
    }

    private fun parseTime(timeStr: String): LocalTime = runCatching { LocalTime.parse(timeStr) }.getOrDefault(LocalTime.MIDNIGHT)
}
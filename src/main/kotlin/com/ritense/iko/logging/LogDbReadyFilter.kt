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

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class LogDbReadyFilter : Filter<ILoggingEvent>() {
    var jdbcUrl: String? = null
    var username: String? = null
    var password: String? = null
    var driverClassName: String? = null

    override fun decide(event: ILoggingEvent): FilterReply {
        if (!enabled()) return FilterReply.DENY
        if (ready.get()) return FilterReply.NEUTRAL

        val now = System.currentTimeMillis()
        val last = lastCheck.get()
        if (now - last < CHECK_INTERVAL_MS || !lastCheck.compareAndSet(last, now)) {
            return FilterReply.DENY
        }
        return if (tableExists()) {
            ready.set(true)
            FilterReply.NEUTRAL
        } else {
            FilterReply.DENY
        }
    }

    private fun enabled(): Boolean = System.getenv("IKO_LOGGING_DB_ENABLED")?.toBoolean() ?: false

    private fun tableExists(): Boolean = try {
        driverClassName?.let { Class.forName(it) }
        DriverManager.getConnection(jdbcUrl, username, password).use { c ->
            c.createStatement().use { it.execute("select 1 from logging_event where 1=0") }
        }
        true
    } catch (e: Exception) {
        false
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 5000L
        private val ready = AtomicBoolean(false)
        private val lastCheck = AtomicLong(0)
    }
}
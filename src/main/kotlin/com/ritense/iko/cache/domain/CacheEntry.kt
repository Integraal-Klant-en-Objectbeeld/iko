/*
 * Copyright (C) 2026 Ritense BV, the Netherlands.
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

package com.ritense.iko.cache.domain

import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.HIT
import com.ritense.iko.cache.domain.CacheEntry.CacheEventType.PUT
import java.time.Duration

data class CacheEntry(
    val type: CacheEventType,
    val key: String,
    val value: String? = null,
    val timeToLive: Duration? = null,
) {
    init {
        if (type == PUT) requireNotNull(value)
        if (type == HIT) requireNotNull(value)
    }

    enum class CacheEventType {
        PUT,
        HIT,
        MISS,
    }
}
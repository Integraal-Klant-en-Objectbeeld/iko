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

package com.ritense.iko.aggregateddataprofile.domain

import com.ritense.iko.cache.domain.CacheSettings
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class AggregatedDataProfileCacheSetting(
    @Column(name = "cache_enabled")
    override val enabled: Boolean = false,
    @Column(name = "cache_ttl")
    // Time to live for cache entries (in ms)
    override val timeToLive: Int = 0,
) : CacheSettings
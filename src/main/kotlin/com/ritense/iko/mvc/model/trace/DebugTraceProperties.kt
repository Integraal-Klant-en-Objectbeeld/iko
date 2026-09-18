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

package com.ritense.iko.mvc.model.trace

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Debug trace viewer settings. [showHeaders] toggles all-or-nothing header display: when `false` (the
 * default) every header value is masked so credentials never reach the browser; when `true` header values
 * are shown as-is. Configured via `iko.debug.show-headers`.
 */
@ConfigurationProperties(prefix = "iko.debug")
internal data class DebugTraceProperties(
    var showHeaders: Boolean = false,
)
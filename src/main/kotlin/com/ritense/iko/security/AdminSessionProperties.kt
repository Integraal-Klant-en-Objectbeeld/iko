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

package com.ritense.iko.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Admin UI session timeout settings. The [timeout] must match
 * `server.servlet.session.timeout`; in `application.yml` the latter is bound to
 * this property so they cannot drift. The client-side inactivity timer reads
 * these values (in seconds) to drive the session timeout modal.
 *
 * Registered as a named component so it can be referenced from Thymeleaf via
 * `${@adminSessionProperties...}`.
 */
@Component("adminSessionProperties")
@ConfigurationProperties(prefix = "iko.security.admin.session")
internal data class AdminSessionProperties(
    val timeout: Duration = Duration.ofMinutes(30),
    val warningBefore: Duration = Duration.ofMinutes(2),
) {
    val timeoutSeconds: Long get() = timeout.seconds

    val warningBeforeSeconds: Long get() = warningBefore.seconds
}
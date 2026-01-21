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

package com.ritense.iko

import com.ritense.iko.MockWebServerExtension.Companion.server
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * MockWebServerExtension is a JUnit 5 extension that manages the lifecycle of the [PetMockServer].
 *
 * It uses Kotlin's `lazy` delegate for the server instance, which guarantees:
 * 1. The initialization block is NOT executed immediately upon class loading.
 * 2. It runs only the first time the [server] property is accessed.
 * 3. After the first access, the same initialized instance is reused.
 * 4. It is thread-safe by default (using LazyThreadSafetyMode.SYNCHRONIZED).
 *
 * This ensures "once-only" initialization even when tests are running in parallel.
 */
class MockWebServerExtension :
    BeforeAllCallback,
    AfterAllCallback {

    companion object {
        val server: PetMockServer by lazy {
            PetMockServer.start()
            return@lazy PetMockServer
        }
    }

    override fun beforeAll(context: ExtensionContext) {
        // Trigger lazy init
        server
    }

    override fun afterAll(context: ExtensionContext) {
        // Optional: do NOT shutdown if reused globally
        // server.shutdown()
    }
}
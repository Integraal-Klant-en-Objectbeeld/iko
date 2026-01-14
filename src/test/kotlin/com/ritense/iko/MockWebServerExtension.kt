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
class MockWebServerExtension : BeforeAllCallback, AfterAllCallback {

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
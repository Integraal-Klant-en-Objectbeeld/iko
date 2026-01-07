package com.ritense.iko

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

object PetMockServer {
    private val server = MockWebServer()

    val url: String
        get() = server.url("/api/pet").toString()

    fun start(port: Int = 0) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.startsWith("/api/pet") -> {
                        when (request.method) {
                            "GET" -> MockResponse().setResponseCode(200).setBody("""{"id": 1, "name": "Mocked Pet"}""")
                            "POST" -> MockResponse().setResponseCode(201).setBody("""{"id": 2, "name": "New Pet"}""")
                            "PUT" -> MockResponse().setResponseCode(200).setBody("""{"id": 1, "name": "Updated Pet"}""")
                            "DELETE" -> MockResponse().setResponseCode(204)
                            else -> MockResponse().setResponseCode(405)
                        }
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start(port)
    }

    fun getPort(): Int = server.port

    fun shutdown() {
        server.shutdown()
    }
}

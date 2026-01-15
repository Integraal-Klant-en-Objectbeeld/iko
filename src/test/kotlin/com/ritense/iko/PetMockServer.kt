package com.ritense.iko

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

object PetMockServer {
    private val server = MockWebServer()
    private val objectMapper = jacksonObjectMapper()
    private val pets = listOf(
        Pet(id = 1, name = "Bello", owners = listOf("Alice", "Bob")),
        Pet(id = 2, name = "Minoes", owners = listOf("Charlie", "Alice")),
        Pet(id = 3, name = "Pip", owners = listOf("Diana")),
        Pet(id = 4, name = "Binky", owners = listOf("Eva", "Bob")),
        Pet(id = 5, name = "Pukkie", owners = listOf("Fleur", "Diana")),
        Pet(id = 6, name = "Tijger", owners = listOf("Gijs", "Charlie")),
        Pet(id = 7, name = "Snuffie", owners = listOf("Hugo", "Fleur")),
        Pet(id = 8, name = "Pluis", owners = listOf("Iris")),
        Pet(id = 9, name = "Blikkie", owners = listOf("Joris", "Eva")),
        Pet(id = 10, name = "Dikkie", owners = listOf("Kiki", "Bob")),
    )

    fun getPort(): Int = server.port

    fun shutdown() {
        server.shutdown()
    }

    val url: String
        get() = server.url("/api/pet").toString()

    fun start(port: Int = 10000) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.startsWith("/api/pet") -> {
                        when (request.method) {
                            "GET" -> {
                                val owners = extractOwners(request)
                                val responsePets = if (owners.isEmpty()) {
                                    pets
                                } else {
                                    pets.filter { pet -> pet.owners.any { owner -> owner in owners } }
                                }
                                val pagedPets = applyPagination(request, responsePets)
                                MockResponse()
                                    .setResponseCode(200)
                                    .setHeader("Content-Type", "application/json")
                                    .setBody(objectMapper.writeValueAsString(pagedPets))
                            }
                            "POST" -> MockResponse()
                                .setResponseCode(201)
                                .setHeader("Content-Type", "application/json")
                                .setBody(objectMapper.writeValueAsString(Pet(id = 3, name = "Pip", owners = listOf("Diana"))))
                            "PUT" -> MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody(objectMapper.writeValueAsString(Pet(id = 1, name = "Binky", owners = listOf("Alice"))))
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

    private fun extractOwners(request: RecordedRequest): Set<String> {
        val values = request.requestUrl?.queryParameterValues("owners").orEmpty()
        return values
            .flatMap { it?.split(",") ?: emptyList() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun applyPagination(request: RecordedRequest, responsePets: List<Pet>): List<Pet> {
        val page = request.requestUrl?.queryParameter("page")?.toIntOrNull()
            ?: request.requestUrl?.queryParameter("pageNumber")?.toIntOrNull()
        val size = request.requestUrl?.queryParameter("size")?.toIntOrNull()
            ?: request.requestUrl?.queryParameter("pageSize")?.toIntOrNull()
        if (page == null || size == null || page < 0 || size <= 0) {
            return responsePets
        }
        val fromIndex = page * size
        if (fromIndex >= responsePets.size) {
            return emptyList()
        }
        val toIndex = (fromIndex + size).coerceAtMost(responsePets.size)
        return responsePets.subList(fromIndex, toIndex)
    }

    private data class Pet(
        val id: Int,
        val name: String,
        val owners: List<String>,
    )
}
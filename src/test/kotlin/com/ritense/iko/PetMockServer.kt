package com.ritense.iko

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

object PetMockServer {
    private val server = MockWebServer()
    private val objectMapper = jacksonObjectMapper()
    private val owners = listOf(
        Owner(id = 1, name = "Alice", gender = "female"),
        Owner(id = 2, name = "Bob", gender = "male"),
        Owner(id = 3, name = "Charlie", gender = "male"),
        Owner(id = 4, name = "Diana", gender = "female"),
        Owner(id = 5, name = "Eva", gender = "female"),
        Owner(id = 6, name = "Fleur", gender = "female"),
        Owner(id = 7, name = "Gijs", gender = "male"),
        Owner(id = 8, name = "Hugo", gender = "male"),
        Owner(id = 9, name = "Iris", gender = "female"),
        Owner(id = 10, name = "Joris", gender = "male"),
        Owner(id = 11, name = "Kiki", gender = "female"),
    )
    private val ownerIdsByName = owners.associate { it.name to it.id }
    private val pets = listOf(
        Pet(id = 1, name = "Bello", ownerId = ownerIdByName("Alice")),
        Pet(id = 2, name = "Minoes", ownerId = ownerIdByName("Alice")),
        Pet(id = 3, name = "Pip", ownerId = ownerIdByName("Diana")),
        Pet(id = 4, name = "Binky", ownerId = ownerIdByName("Eva")),
        Pet(id = 5, name = "Pukkie", ownerId = ownerIdByName("Diana")),
        Pet(id = 6, name = "Tijger", ownerId = ownerIdByName("Charlie")),
        Pet(id = 7, name = "Snuffie", ownerId = ownerIdByName("Fleur")),
        Pet(id = 8, name = "Pluis", ownerId = ownerIdByName("Fleur")),
        Pet(id = 9, name = "Blikkie", ownerId = ownerIdByName("Eva")),
        Pet(id = 10, name = "Dikkie", ownerId = ownerIdByName("Bob")),
    )

    fun getPort(): Int = server.port

    fun shutdown() {
        server.shutdown()
    }

    val url: String
        get() = server.url("/api/pets").toString()

    fun start(port: Int = 10000) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.startsWith("/api/pets") -> {
                        when (request.method) {
                            "GET" -> {
                                val ownerIds = extractOwnerIds(request)
                                val petId = request.requestUrl?.queryParameter("id")?.toIntOrNull()
                                val responsePets = pets
                                    .filter { pet -> petId == null || pet.id == petId }
                                    .filter { pet -> ownerIds.isEmpty() || pet.ownerId in ownerIds }
                                val orderedPets = applyOrdering(request, responsePets)
                                val pagedPets = applyPagination(request, orderedPets)
                                val body = if (petId != null) {
                                    responsePets.firstOrNull()
                                } else {
                                    pagedPets
                                }
                                MockResponse()
                                    .setResponseCode(200)
                                    .setHeader("Content-Type", "application/json")
                                    .setBody(objectMapper.writeValueAsString(body))
                            }
                            "POST" -> MockResponse()
                                .setResponseCode(201)
                                .setHeader("Content-Type", "application/json")
                                .setBody(objectMapper.writeValueAsString(Pet(id = 3, name = "Pip", ownerId = ownerIdByName("Diana"))))
                            "PUT" -> MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody(objectMapper.writeValueAsString(Pet(id = 1, name = "Binky", ownerId = ownerIdByName("Alice"))))
                            "DELETE" -> MockResponse().setResponseCode(204)
                            else -> MockResponse().setResponseCode(405)
                        }
                    }

                    path.startsWith("/api/pet/fail") -> {
                        MockResponse().setResponseCode(500).setBody("""{"error": "Internal Server Error"}""")
                    }

                    path.startsWith("/api/owners") -> {
                        when (request.method) {
                            "GET" -> {
                                val ownerId = request.requestUrl?.queryParameter("id")?.toIntOrNull()
                                val responseOwners = if (ownerId == null) {
                                    owners
                                } else {
                                    owners.filter { it.id == ownerId }
                                }
                                MockResponse()
                                    .setResponseCode(200)
                                    .setHeader("Content-Type", "application/json")
                                    .setBody(objectMapper.writeValueAsString(responseOwners))
                            }

                            else -> MockResponse().setResponseCode(405)
                        }
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start(port)
    }

    private fun extractOwnerIds(request: RecordedRequest): Set<Int> {
        val values = request.requestUrl?.queryParameterValues("ownerId").orEmpty()
        val directIds = values
            .flatMap { it?.split(",") ?: emptyList() }
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
        if (directIds.isNotEmpty()) {
            return directIds
        }
        val ownerNames = request.requestUrl?.queryParameterValues("owners").orEmpty()
        return ownerNames
            .flatMap { it?.split(",") ?: emptyList() }
            .mapNotNull { ownerIdsByName[it.trim()] }
            .toSet()
    }

    private fun ownerIdByName(name: String): Int = ownerIdsByName[name] ?: 0

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

    private fun applyOrdering(request: RecordedRequest, responsePets: List<Pet>): List<Pet> {
        val ordering = request.requestUrl?.queryParameter("ordering")?.trim().orEmpty()
        if (ordering.isEmpty()) {
            return responsePets
        }
        val descending = ordering.startsWith("-")
        val property = if (descending) ordering.substring(1) else ordering
        val comparator = when (property) {
            "id" -> compareBy<Pet> { it.id }
            "name" -> compareBy<Pet> { it.name }
            "ownerId" -> compareBy<Pet> { it.ownerId }
            else -> null
        } ?: return responsePets
        return if (descending) {
            responsePets.sortedWith(comparator.reversed())
        } else {
            responsePets.sortedWith(comparator)
        }
    }

    private data class Pet(
        val id: Int,
        val name: String,
        val ownerId: Int,
    )

    private data class Owner(
        val id: Int,
        val name: String,
        val gender: String,
    )
}
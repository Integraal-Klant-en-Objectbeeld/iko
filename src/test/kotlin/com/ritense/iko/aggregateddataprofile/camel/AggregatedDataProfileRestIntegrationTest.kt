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

package com.ritense.iko.aggregateddataprofile.camel

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ritense.iko.BaseIntegrationTest
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.service.CacheService
import com.ritense.iko.camel.IkoConstants.Variables.ENDPOINT_TRANSFORM_CONTEXT_VARIABLE
import org.apache.camel.CamelContext
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@AutoConfigureMockMvc
internal class AggregatedDataProfileRestIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var aggregatedDataProfileRepository: AggregatedDataProfileRepository

    @Autowired
    private lateinit var cacheService: CacheService

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

    @Autowired
    private lateinit var camelContext: CamelContext

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When a valid ADP is requested via REST then it should route to the dynamic route`() {
        // Act & Assert
        val mvcResult = mockMvc.perform(get("/aggregated-data-profiles/pets?id=externalId"))
            .andExpect(request().asyncStarted()) // Verify it started async if applicable
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andDo(print()) // logs final response
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """[
                        "Bello",
                        "Minoes",
                        "Pip",
                        "Binky",
                        "Pukkie",
                        "Tijger",
                        "Snuffie",
                        "Pluis",
                        "Blikkie",
                        "Dikkie"
                    ]""",
                ),
            )
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When one relation fails then the API should return 500 Internal Server Error`() {
        // Act & Assert
        val mvcResult = mockMvc.perform(get("/aggregated-data-profiles/test-failing-relation?id=externalId"))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andDo(print())
            .andExpect(status().isInternalServerError)
            .andExpect(content().string(containsString("Unexpected error")))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `Should return 500 when a transform returns an unsupported data type`() {
        val mvcResult = mockMvc.perform(
            get("/aggregated-data-profiles/endpoint-transform-result-array?id=externalId"),
        )
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andDo(print())
            .andExpect(status().isInternalServerError)
            .andExpect(
                content().string(
                    containsString("Transform result is unsupported. Expected ObjectNode; got ArrayNode."),
                ),
            )
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When a non-existing ADP is requested via REST then it should return an error`() {
        // Act & Assert
        val result = mockMvc.perform(get("/aggregated-data-profiles/non-existing"))
            .andExpect(request().asyncStarted()) // Verify it started async if applicable
            .andReturn()

        mockMvc.perform(asyncDispatch(result))
            .andDo(print()) // logs final response
            .andExpect(status().isNotFound)
            .andExpect(content().string(containsString("ADP with name: non-existing was not found")))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When adp is marked as cached then it exists in Redis`() {
        val profileName = "test-cached"
        cacheService.evictByPrefix(profileName)

        aggregatedDataProfileRepository.findByName(profileName)?.let { profile ->
            assertThat(cacheService.isCached(profile.id.toString()))
                .withFailMessage { "Cache should not contain an entry for profile $profileName (${profile.id})" }
                .isFalse()
        } ?: throw AssertionError("Profile with name $profileName not found in repository")

        val mvcResult = mockMvc.perform(get("/aggregated-data-profiles/$profileName?id=externalId"))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)

        aggregatedDataProfileRepository.findByName(profileName)?.let { profile ->
            assertThat(cacheService.isCached(profile.id.toString()))
                .withFailMessage { "Cache should contain an entry for profile $profileName (${profile.id})" }
                .isTrue()
        } ?: throw AssertionError("Profile with name $profileName not found in repository")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `Two different residents produce two distinct Redis cache keys`() {
        val profileName = "resident-key-collision"
        val profile = aggregatedDataProfileRepository.findByName(profileName)
            ?: throw AssertionError("Profile with name $profileName not found in repository")
        val keyPrefix = "${profile.id}:"

        cacheService.evictByPrefix(profile.id.toString())
        assertThat(redisTemplate.keys("$keyPrefix*")).isEmpty()

        // Resident A
        val resultA = mockMvc.perform(get("/aggregated-data-profiles/$profileName?id=resident-A"))
            .andExpect(request().asyncStarted())
            .andReturn()
        mockMvc.perform(asyncDispatch(resultA))
            .andExpect(status().isOk)

        // Resident B, within the TTL window of A's entry
        val resultB = mockMvc.perform(get("/aggregated-data-profiles/$profileName?id=resident-B"))
            .andExpect(request().asyncStarted())
            .andReturn()
        mockMvc.perform(asyncDispatch(resultB))
            .andExpect(status().isOk)

        // Pre-fix the two residents collide on a single key; after the fix the resident
        // identifier is always part of the key, so two distinct keys must exist.
        assertThat(redisTemplate.keys("$keyPrefix*"))
            .withFailMessage {
                "Expected two distinct cache keys for two residents under prefix '$keyPrefix', " +
                    "found ${redisTemplate.keys("$keyPrefix*").size}"
            }
            .hasSize(2)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `Concurrent relation requests with distinct residents never mutate the shared context and resolve their own data`() {
        // The relation branch reads the per-request endpointTransformContext variable and injects the
        // parent body under `.source`. Under Camel's shallow-copy multicast/split semantics that ObjectNode
        // is shared by reference across sibling branches, so mutating it in place lets one branch (or, under
        // the shared default pool, one request) observe another's `.source`. The fix deep-copies before the
        // write. This test drives the deployed relation route directly with two distinct residents in parallel
        // and asserts (a) the caller-owned context ObjectNode is never mutated in place -- the deterministic
        // regression guard that fails on the pre-fix in-place mutation -- and (b) each request resolves its own
        // resident's owner.
        val profile = aggregatedDataProfileRepository.findByName("pet-household")
            ?: throw AssertionError("Profile with name pet-household not found in repository")
        val ownerRelation = profile.relations.first { it.propertyName == "owner" }
        val relationUri = "direct:relation_${ownerRelation.id}"

        // Two residents mapped to two distinct owners: ownerId 1 -> "Alice", ownerId 5 -> "Eva".
        val residents = listOf(
            ResidentCase(idParam = "resident-A", ownerId = 1, expectedOwner = "Alice"),
            ResidentCase(idParam = "resident-B", ownerId = 5, expectedOwner = "Eva"),
        )

        val producerTemplate = camelContext.createProducerTemplate()
        val executor = Executors.newFixedThreadPool(residents.size)
        try {
            val futures = residents.map { resident ->
                executor.submit<ResidentResult> {
                    // A fresh, caller-owned context per request. If the relation branch mutates this in place,
                    // it gains a `source` field; the fix keeps it as the immutable request context.
                    val sharedContext: ObjectNode = objectMapper.createObjectNode().apply {
                        put("idParam", resident.idParam)
                        set<JsonNode>("sortParams", objectMapper.createObjectNode())
                        set<JsonNode>("filterParams", objectMapper.createObjectNode())
                    }
                    val parentBody: JsonNode = objectMapper.createObjectNode().put("ownerId", resident.ownerId)

                    val result = producerTemplate.send(relationUri) { exchange ->
                        exchange.setVariable(ENDPOINT_TRANSFORM_CONTEXT_VARIABLE, sharedContext)
                        exchange.message.body = parentBody
                    }
                    result.exception?.let { throw it }

                    ResidentResult(
                        contextMutatedInPlace = sharedContext.has("source"),
                        responseBody = result.message.getBody(String::class.java),
                        expectedOwner = resident.expectedOwner,
                    )
                }
            }

            val results = futures.map { it.get(30, TimeUnit.SECONDS) }

            results.forEach { result ->
                assertThat(result.contextMutatedInPlace)
                    .withFailMessage {
                        "Relation branch mutated the shared endpointTransformContext in place " +
                            "(a `source` field leaked onto the caller's request context); it must deep-copy first"
                    }
                    .isFalse()
                assertThat(result.responseBody)
                    .withFailMessage {
                        "Expected relation to resolve owner '${result.expectedOwner}', got '${result.responseBody}'"
                    }
                    .contains(result.expectedOwner)
            }
        } finally {
            executor.shutdownNow()
            producerTemplate.stop()
        }
    }

    private data class ResidentCase(
        val idParam: String,
        val ownerId: Int,
        val expectedOwner: String,
    )

    private data class ResidentResult(
        val contextMutatedInPlace: Boolean,
        val responseBody: String?,
        val expectedOwner: String,
    )

    @Test
    @WithMockUser(roles = ["UNKNOWN"])
    fun `Get adp pets returns 4XX when authenticated user lacks ROLE_ADMIN`() {
        // Act & Assert
        val mvcResult = mockMvc.perform(get("/aggregated-data-profiles/pets?id=externalId"))
            .andExpect(request().asyncStarted()) // Verify it started async if applicable
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andDo(print()) // logs final response
            .andExpect(status().isUnauthorized)
    }

    private fun encodeContainerParam(containerParam: ContainerParam): String {
        val json = objectMapper.writeValueAsString(containerParam)
        return Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
    }
}
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

package com.ritense.iko.aggregateddataprofile.camel

import com.ritense.iko.BaseIntegrationTest
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.cache.service.CacheService
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Base64

@AutoConfigureMockMvc
internal class AggregatedDataProfileRestIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var aggregatedDataProfileRepository: AggregatedDataProfileRepository

    @Autowired
    private lateinit var cacheService: CacheService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

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
    fun `When the pets ADP is requested via REST then it should aggregate the data`() {
        // Act & Assert
        val containerParam = ContainerParam(
            containerId = "pets",
            filters = mapOf("id" to "4"),
        )
        val encodedContainerParam = encodeContainerParam(containerParam)

        val mvcResult = mockMvc.perform(
            get("/aggregated-data-profiles/pet-household")
                .queryParam("containerParam", encodedContainerParam),
        )
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andDo(print()) // logs final response
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """{
                        "owner": "Eva",
                        "pets": [
                            "Binky",
                            "Blikkie"
                        ]
                    }""",
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
    fun `When a non-existing ADP is requested via REST then it should return an error`() {
        // Act & Assert
        val result = mockMvc.perform(get("/aggregated-data-profiles/non-existing"))
            .andExpect(request().asyncStarted()) // Verify it started async if applicable
            .andReturn()

        mockMvc.perform(asyncDispatch(result))
            .andDo(print()) // logs final response
            .andExpect(status().isNotFound)
            .andExpect(content().string(containsString("ADP with name: non-existing, not found")))
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
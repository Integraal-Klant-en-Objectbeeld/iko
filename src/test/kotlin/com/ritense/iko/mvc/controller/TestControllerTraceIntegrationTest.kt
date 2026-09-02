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

package com.ritense.iko.mvc.controller

import com.ritense.iko.BaseIntegrationTest
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
internal class TestControllerTraceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var aggregatedDataProfileRepository: AggregatedDataProfileRepository

    @Test
    @WithMockUser(authorities = ["ROLE_ADMIN"])
    fun `debug embeds a trace graph with a connector node, operation label and paired http status`() {
        val version = aggregatedDataProfileRepository.findByName("pets")!!.version.value

        mockMvc.perform(
            post("/admin/aggregated-data-profiles/debug")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "pets")
                .param("version", version)
                .param("resultTransform", ".")
                .param(
                    "endpointTransformContext",
                    """{"idParam": "", "sortParams": {}, "filterParams": {}}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("id=\"trace-graph-data\"")))
            // typed, node-level business steps reconstructed from the real route/node ids
            .andExpect(content().string(containsString("\"type\":\"ADP_ENTRY\"")))
            .andExpect(content().string(containsString("\"type\":\"ADP_ENDPOINT_TRANSFORM\"")))
            .andExpect(content().string(containsString("\"type\":\"CONNECTOR_OPERATION\"")))
            // the real outgoing call is its own HTTP step (the rest-openapi toD node)
            .andExpect(content().string(containsString("\"type\":\"HTTP\"")))
            .andExpect(content().string(containsString("\"type\":\"ADP_RESULT_TRANSFORM\"")))
            .andExpect(content().string(containsString("\"type\":\"ADP_RESULT\"")))
            // HTTP method + status live in the typed http block on the HTTP step
            .andExpect(content().string(containsString("\"status\":200")))
            // the real outgoing request rest-openapi built is captured at the HttpClient layer
            .andExpect(content().string(containsString("Outgoing URL")))
            // each node carries its branch (exchangeId) so the SVG can assign lanes
            .andExpect(content().string(containsString("\"branch\":")))
    }

    @Test
    @WithMockUser(authorities = ["ROLE_ADMIN"])
    fun `debug of a failing relation embeds a FAILED node with an exception`() {
        val version = aggregatedDataProfileRepository.findByName("test-failing-relation")!!.version.value

        mockMvc.perform(
            post("/admin/aggregated-data-profiles/debug")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "test-failing-relation")
                .param("version", version)
                .param("resultTransform", ".")
                .param(
                    "endpointTransformContext",
                    """{"idParam": "", "sortParams": {}, "filterParams": {}}""",
                ),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("id=\"trace-graph-data\"")))
            .andExpect(content().string(containsString("\"status\":\"FAILED\"")))
            // relation branch + aggregation merge node are reconstructed as their own typed steps
            .andExpect(content().string(containsString("\"type\":\"RELATION_START\"")))
            .andExpect(content().string(containsString("\"type\":\"AGGREGATION\"")))
    }
}
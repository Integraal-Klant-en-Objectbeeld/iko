package com.ritense.iko.aggregateddataprofile.camel

import com.ritense.iko.BaseIntegrationTest
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

@AutoConfigureMockMvc
internal class AggregatedDataProfileRestIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When a valid ADP is requested via REST then it should route to the dynamic route`() {
        // Act & Assert
        val mvcResult = mockMvc.perform(get("/aggregated-data-profiles/test/externalId"))
            .andExpect(request().asyncStarted()) // Verify it started async if applicable
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(content().json("""{"id": 1, "name": "Mocked Pet"}"""))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When a non-existing ADP is requested via REST then it should return an error`() {
        // Act & Assert
        val result = mockMvc.perform(get("/aggregated-data-profiles/non-existing/externalId"))
            .andDo(print()) // logs async start response
            .andReturn()

        mockMvc.perform(asyncDispatch(result))
            .andDo(print()) // logs final response
            .andExpect(status().isNotFound)
            .andExpect(content().string(containsString("ADP with name: non-existing, not found")))
    }
}
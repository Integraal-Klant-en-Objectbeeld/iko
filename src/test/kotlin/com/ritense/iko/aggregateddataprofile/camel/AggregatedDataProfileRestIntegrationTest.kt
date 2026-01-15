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
            .andDo(print()) // logs final response
            .andExpect(status().isOk)
            .andExpect(content().json("""{"id": 1, "name": "Mocked Pet"}"""))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When a valid ADP with relations is requested via REST then it should aggregate the data`() {
        // Act & Assert
        val mvcResult = mockMvc.perform(get("/aggregated-data-profiles/test-with-relations/externalId"))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andDo(print()) // logs final response
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """
                {
                  "left": {"id": 1, "name": "Mocked Pet"},
                  "right": {
                    "pet1": {"id": 1, "name": "Mocked Pet"},
                    "pet2": {"id": 1, "name": "Mocked Pet"}
                  }
                }
                    """.trimIndent(),
                ),
            )
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When one relation fails then the API should return 500 Internal Server Error`() {
        // Act & Assert
        val mvcResult = mockMvc.perform(get("/aggregated-data-profiles/test-failing-relation/externalId"))
            .andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andDo(print())
            .andExpect(status().isInternalServerError)
            .andExpect(
                content().json(
                    """
            {
                "error":"HttpOperationFailedException",
                "message":"HTTP operation failed invoking http://localhost:10000/api/pet/fail with statusCode: 500"
            }
                    """.trimIndent(),
                ),
            )
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When a non-existing ADP is requested via REST then it should return an error`() {
        // Act & Assert
        val result = mockMvc.perform(get("/aggregated-data-profiles/non-existing/externalId"))
            .andExpect(request().asyncStarted()) // Verify it started async if applicable
            .andReturn()

        mockMvc.perform(asyncDispatch(result))
            .andDo(print()) // logs final response
            .andExpect(status().isNotFound)
            .andExpect(content().string(containsString("ADP with name: non-existing, not found")))
    }
}
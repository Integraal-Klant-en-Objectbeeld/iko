package com.ritense.iko.aggregateddataprofile.camel

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.iko.BaseIntegrationTest
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Base64

@AutoConfigureMockMvc
internal class ContainerParamRestIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When an ADP is requested with a single containerParam then it is handled`() {
        val uriTemplate = "/aggregated-data-profiles/pets"
        val containerParam = ContainerParam(
            containerId = "pets",
            filters = mapOf("status" to "available"),
        )
        val encodedContainerParam = encodeContainerParam(containerParam)

        val mvcResult = mockMvc.perform(
            get(uriTemplate)
                .queryParam("containerParam", encodedContainerParam),
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
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
            .andReturn()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When an ADP is requested with multiple containerParams then they are handled`() {
        val uriTemplate = "/aggregated-data-profiles/pets"
        val petContainerParam = ContainerParam(
            containerId = "pets",
            filters = mapOf("status" to "pending"),
        )
        val encodedPetContainerParam = encodeContainerParam(petContainerParam)

        val mvcResult = mockMvc.perform(
            get(uriTemplate)
                .queryParam("containerParam", encodedPetContainerParam),
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
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
            .andReturn()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When an ADP is requested with pageable containerParams then they are handled`() {
        val uriTemplate = "/aggregated-data-profiles/pets"
        val containerParam = ContainerParam(
            containerId = "pets",
            pageable = PageRequest.of(1, 5, Sort.by("name").descending()),
        )
        val encodedContainerParam = encodeContainerParam(containerParam)

        val mvcResult = mockMvc.perform(
            get(uriTemplate)
                .queryParam("containerParam", encodedContainerParam)
                .queryParam("id", "externalId"),
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """[
                        "Tijger",
                        "Snuffie",
                        "Pluis",
                        "Blikkie",
                        "Dikkie"
                    ]""",
                ),
            )
            .andReturn()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When an ADP is requested with unpaged sorting then it returns sorted pets`() {
        val uriTemplate = "/aggregated-data-profiles/pets"
        val containerParam = ContainerParam(
            containerId = "pets",
            pageable = Pageable.unpaged(Sort.by("name").ascending()),
        )
        val encodedContainerParam = encodeContainerParam(containerParam)

        val mvcResult = mockMvc.perform(
            get(uriTemplate)
                .queryParam("containerParam", encodedContainerParam),
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """[
                        "Bello",
                        "Binky",
                        "Blikkie",
                        "Dikkie",
                        "Minoes",
                        "Pip",
                        "Pluis",
                        "Pukkie",
                        "Snuffie",
                        "Tijger"
                    ]""",
                ),
            )
            .andReturn()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When an ADP is requested with pageable sorting then it returns the selected page`() {
        val uriTemplate = "/aggregated-data-profiles/pets"
        val containerParam = ContainerParam(
            containerId = "pets",
            pageable = PageRequest.of(0, 3, Sort.by("name").descending()),
        )
        val encodedContainerParam = encodeContainerParam(containerParam)

        val mvcResult = mockMvc.perform(
            get(uriTemplate)
                .queryParam("containerParam", encodedContainerParam),
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """[
                        "Tijger",
                        "Snuffie",
                        "Pukkie"
                    ]""",
                ),
            )
            .andReturn()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When an ADP is requested with a later page then it returns the correct slice`() {
        val uriTemplate = "/aggregated-data-profiles/pets"
        val containerParam = ContainerParam(
            containerId = "pets",
            pageable = PageRequest.of(1, 4, Sort.by("name").ascending()),
        )
        val encodedContainerParam = encodeContainerParam(containerParam)

        val mvcResult = mockMvc.perform(
            get(uriTemplate)
                .queryParam("containerParam", encodedContainerParam),
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """[
                        "Minoes",
                        "Pip",
                        "Pluis",
                        "Pukkie"
                    ]""",
                ),
            )
            .andReturn()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When the ADP is queried with ownerId filter then it returns matching pets`() {
        val uriTemplate = "/aggregated-data-profiles/pets"
        val containerParams = encodeContainerParam(
            ContainerParam(
                containerId = "pets",
                pageable = Pageable.unpaged(Sort.by("name").descending()),
                filters = mapOf("ownerId" to "5"),
            ),
        )

        val mvcResult = mockMvc.perform(
            get(uriTemplate)
                .queryParam("containerParam", containerParams),
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """[
                        "Blikkie",
                        "Binky"
                    ]""",
                ),
            )
            .andReturn()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When containerParam is invalid base64 then it returns bad request`() {
        val uriTemplate = "/aggregated-data-profiles/pets"
        val invalidContainerParam = "not-base64"

        val mvcResult = mockMvc.perform(
            get(uriTemplate)
                .queryParam("containerParam", invalidContainerParam),
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)
            .andExpect(content().string(containsString("containerParam")))
            .andReturn()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `When containerParam has malformed json then it returns bad request`() {
        val uriTemplate = "/aggregated-data-profiles/pets"
        val malformedJsonParam = encodeContainerParamRaw("""{"containerId":"pets","filters":{"ownerId":"5"}""")

        val mvcResult = mockMvc.perform(
            get(uriTemplate)
                .queryParam("containerParam", malformedJsonParam),
        ).andExpect(request().asyncStarted())
            .andReturn()

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isBadRequest)
            .andExpect(content().string(containsString("containerParam")))
            .andReturn()
    }

    private fun encodeContainerParam(containerParam: ContainerParam): String {
        val json = objectMapper.writeValueAsString(containerParam)
        return encodeContainerParamRaw(json)
    }

    private fun encodeContainerParamRaw(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
}
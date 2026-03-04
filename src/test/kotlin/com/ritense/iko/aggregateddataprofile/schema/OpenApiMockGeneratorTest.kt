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

package com.ritense.iko.aggregateddataprofile.schema

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenApiMockGeneratorTest {

    private val generator = OpenApiMockGenerator(ObjectMapper())

    @Test
    fun `loadSpec loads pet-api from classpath`() {
        val openApi = generator.loadSpec("classpath:pet-api.yaml")

        assertThat(openApi.info.title).isEqualTo("Pet Mock API")
        assertThat(openApi.paths).containsKey("/pets")
    }

    @Test
    fun `generateMock returns array with Pet properties for GetPets`() {
        val openApi = generator.loadSpec("classpath:pet-api.yaml")

        val mock = generator.generateMock(openApi, "GetPets")

        assertThat(mock.isArray).isTrue()
        assertThat(mock.size()).isEqualTo(1)
        val pet = mock.first()
        assertThat(pet.has("id")).isTrue()
        assertThat(pet.has("name")).isTrue()
        assertThat(pet.has("ownerId")).isTrue()
        assertThat(pet.get("id").isInt).isTrue()
        assertThat(pet.get("name").isTextual).isTrue()
        assertThat(pet.get("ownerId").isInt).isTrue()
    }

    @Test
    fun `generateMock returns array with Owner properties for GetOwners`() {
        val openApi = generator.loadSpec("classpath:pet-api.yaml")

        val mock = generator.generateMock(openApi, "GetOwners")

        assertThat(mock.isArray).isTrue()
        val owner = mock.first()
        assertThat(owner.has("id")).isTrue()
        assertThat(owner.has("name")).isTrue()
        assertThat(owner.has("gender")).isTrue()
        assertThat(owner.get("gender").asText()).isEqualTo("female")
    }

    @Test
    fun `generateMock returns NullNode for unknown operationId`() {
        val openApi = generator.loadSpec("classpath:pet-api.yaml")

        val mock = generator.generateMock(openApi, "NonExistentOperation")

        assertThat(mock.isNull).isTrue()
    }

    @Test
    fun `generateMock returns NullNode for operation with no 2xx response`() {
        val openApi = generator.loadSpec("classpath:pet-api.yaml")

        val mock = generator.generateMock(openApi, "GetPetFail")

        assertThat(mock.isNull).isTrue()
    }

    @Test
    fun `generateMock resolves ref to component schema`() {
        val openApi = generator.loadSpec("classpath:pet-api.yaml")

        val mock = generator.generateMock(openApi, "GetPets")

        val pet = mock.first()
        assertThat(pet.isObject).isTrue()
        assertThat(pet.size()).isEqualTo(3)
    }

    @Test
    fun `generateMock returns correct types for OpenAPI 3_1 spec`() {
        val openApi = generator.loadSpec("classpath:complex-api.yaml")

        val mock = generator.generateMock(openApi, "GetItems")

        assertThat(mock.isArray).isTrue()
        val item = mock.first()
        assertThat(item.get("id").isInt).isTrue()
        assertThat(item.get("name").isTextual).isTrue()
        assertThat(item.get("active").isBoolean).isTrue()
        assertThat(item.get("score").isNumber).isTrue()
    }

    @Test
    fun `generateMock resolves discriminator to concrete subtype`() {
        val openApi = generator.loadSpec("classpath:complex-api.yaml")

        val mock = generator.generateMock(openApi, "QueryPersons")

        assertThat(mock.isObject).isTrue()
        assertThat(mock.has("personen")).isTrue()
        assertThat(mock.get("personen").isArray).isTrue()
        val persoon = mock.get("personen").first()
        assertThat(persoon.get("burgerservicenummer").asText()).isEqualTo("999993653")
    }

    @Test
    fun `generateMock resolves allOf wrapping single enum ref`() {
        val openApi = generator.loadSpec("classpath:complex-api.yaml")

        val mock = generator.generateMock(openApi, "GetCases")

        assertThat(mock.isArray).isTrue()
        val case = mock.first()
        assertThat(case.get("status").isTextual).isTrue()
        assertThat(case.get("status").asText()).isEqualTo("open")
    }

    @Test
    fun `generateMock resolves allOf with enum ref and description overlay`() {
        val openApi = generator.loadSpec("classpath:complex-api.yaml")

        val mock = generator.generateMock(openApi, "GetCases")

        assertThat(mock.isArray).isTrue()
        val case = mock.first()
        assertThat(case.get("priority").isTextual).isTrue()
        assertThat(case.get("priority").asText()).isEqualTo("high")
    }
}
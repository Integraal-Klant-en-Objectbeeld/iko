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

package com.ritense.iko

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PetMockServerIntegrationTest : BaseIntegrationTest() {

    private val client = OkHttpClient()

    @Test
    fun `test GET pets`() {
        val request = Request.Builder()
            .url(PetMockServer.url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
            val body = response.body?.string()
            assertThat(body).contains(""""id":1""")
            assertThat(body).contains(""""name":"Bello"""")
            assertThat(body).contains(""""ownerId":1""")
        }
    }

    @Test
    fun `test POST pet`() {
        val request = Request.Builder()
            .url(PetMockServer.url)
            .post("{}".toRequestBody())
            .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(201)
            val body = response.body?.string()
            assertThat(body).contains(""""id":3""")
            assertThat(body).contains(""""name":"Pip"""")
            assertThat(body).contains(""""ownerId":4""")
        }
    }

    @Test
    fun `test PUT pet`() {
        val request = Request.Builder()
            .url(PetMockServer.url)
            .put("{}".toRequestBody())
            .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
            val body = response.body?.string()
            assertThat(body).contains(""""name":"Binky"""")
            assertThat(body).contains(""""ownerId":1""")
        }
    }

    @Test
    fun `test DELETE pet`() {
        val request = Request.Builder()
            .url(PetMockServer.url)
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(204)
        }
    }
}
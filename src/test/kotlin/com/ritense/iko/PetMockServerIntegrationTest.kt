package com.ritense.iko

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PetMockServerIntegrationTest : BaseIntegrationTest() {

    private val client = OkHttpClient()

    @Test
    fun `test GET pet`() {
        val request = Request.Builder()
            .url(PetMockServer.url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
            val body = response.body?.string()
            assertThat(body).contains(""""id": 1""")
            assertThat(body).contains(""""name": "Mocked Pet"""")
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
            assertThat(response.body?.string()).contains(""""id": 2""")
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
            assertThat(response.body?.string()).contains(""""name": "Updated Pet"""")
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
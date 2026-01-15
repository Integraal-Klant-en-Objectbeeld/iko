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
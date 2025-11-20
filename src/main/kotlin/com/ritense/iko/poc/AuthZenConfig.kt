package com.ritense.iko.poc

import com.fasterxml.jackson.databind.ObjectMapper
import com.ritense.authzenk.Client
import com.ritense.authzenk.EvaluationContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import java.net.http.HttpClient

@Service
class AuthZenConfig(
    @param:Value("\${pdp.host}") private val host: String,
    val objectMapper: ObjectMapper
) {

    @Bean
    fun pdpClient(): Client {
        return Client(
            EvaluationContext(
                httpClient = HttpClient.newHttpClient(),
                this.host,
                objectMapper
            )
        )
    }
}
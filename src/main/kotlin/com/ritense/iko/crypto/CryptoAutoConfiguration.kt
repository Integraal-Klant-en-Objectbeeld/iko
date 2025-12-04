package com.ritense.iko.crypto

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

@Configuration
class CryptoAutoConfiguration {
    @Bean
    fun aesSecretKeySpec(
        @Value("\${iko.crypto.key}") secretKeyString: String,
    ): SecretKeySpec {
        val decoded = Base64.getDecoder().decode(secretKeyString)
        return SecretKeySpec(decoded, "AES")
    }
}
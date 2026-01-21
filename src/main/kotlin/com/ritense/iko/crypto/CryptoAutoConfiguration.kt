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
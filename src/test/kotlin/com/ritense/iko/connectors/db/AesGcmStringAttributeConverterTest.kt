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

package com.ritense.iko.connectors.db

import com.ritense.iko.crypto.AesGcmEncryptionService
import com.ritense.iko.crypto.AesGcmStringAttributeConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

class AesGcmStringAttributeConverterTest {
    private lateinit var converter: AesGcmStringAttributeConverter

    @BeforeEach
    fun setupKey() {
        // 32 bytes key (256-bit), fixed for test stability
        val keyBytes = ByteArray(32) { (it + 1).toByte() }
        val keyB64 = Base64.getEncoder().encodeToString(keyBytes)

        val decoded = Base64.getDecoder().decode(keyB64)
        val key = SecretKeySpec(decoded, "AES")
        converter =
            AesGcmStringAttributeConverter(
                AesGcmEncryptionService(key),
            )
    }

    @Test
    fun `encrypt decrypt roundtrip`() {
        val plaintext = "superSecretValue!"
        val db = converter.convertToDatabaseColumn(plaintext)
        assertThat(db).isNotNull()
        // should be base64 and not equals to plaintext
        assertThat(db).isNotEqualTo(plaintext)
        val decoded = Base64.getDecoder().decode(db!!)
        assertThat(decoded.size).isGreaterThan(12) // includes 12-byte IV + ciphertext

        val out = converter.convertToEntityAttribute(db)
        assertThat(out).isEqualTo(plaintext)
    }

    @Test
    fun `encryption uses random IV so ciphertext differs`() {
        val plaintext = "sameInput"
        val c1 = converter.convertToDatabaseColumn(plaintext)
        val c2 = converter.convertToDatabaseColumn(plaintext)
        assertThat(c1).isNotEqualTo(c2)
    }

    @Test
    fun `null handling`() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull()
        assertThat(converter.convertToEntityAttribute(null)).isNull()
    }

    @Test
    fun `invalid base64 throws`() {
        assertThrows<IllegalStateException> {
            converter.convertToEntityAttribute("not-base64!!")
        }
    }
}
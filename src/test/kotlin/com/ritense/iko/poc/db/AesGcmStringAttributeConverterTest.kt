package com.ritense.iko.poc.db

import com.ritense.iko.crypto.AesGcmEncryptionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

class AesGcmStringAttributeConverterTest {

    private lateinit var converter : AesGcmStringAttributeConverter

    @BeforeEach
    fun setupKey() {
        // 32 bytes key (256-bit), fixed for test stability
        val keyBytes = ByteArray(32) { (it + 1).toByte() }
        val keyB64 = Base64.getEncoder().encodeToString(keyBytes)

        val decoded = Base64.getDecoder().decode(keyB64)
        val key = SecretKeySpec(decoded, "AES")
        converter = AesGcmStringAttributeConverter(
            AesGcmEncryptionService(key)
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

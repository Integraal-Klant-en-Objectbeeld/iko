package com.ritense.iko.poc.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

class AesGcmStringAttributeConverterTest {

    private val converter = AesGcmStringAttributeConverter()

    @BeforeEach
    fun setupKey() {
        // 32 bytes key (256-bit), fixed for test stability
        val keyBytes = ByteArray(32) { (it + 1).toByte() }
        val keyB64 = Base64.getEncoder().encodeToString(keyBytes)
        System.setProperty("iko.crypto.key", keyB64)
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

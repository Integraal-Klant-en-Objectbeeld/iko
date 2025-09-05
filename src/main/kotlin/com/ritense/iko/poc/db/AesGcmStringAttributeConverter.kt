package com.ritense.iko.poc.db

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM AttributeConverter for encrypting/decrypting String values stored in DB.
 *
 * Key configuration (must be 32 bytes = 256 bits):
 * - System property: iko.crypto.key (Base64 or Hex)
 * - or Environment variable: IKO_CRYPTO_KEY (Base64 or Hex)
 *
 * Storage format: Base64 encoding of (IV || CIPHERTEXT_WITH_TAG), where
 * - IV is 12 random bytes (96 bits)
 * - GCM auth tag is included at the end of the ciphertext by JCE
 */
@Converter(autoApply = false)
class AesGcmStringAttributeConverter : AttributeConverter<String?, String?> {

    private val secureRandom = SecureRandom()

    override fun convertToDatabaseColumn(attribute: String?): String? {
        attribute ?: return null
        val keyBytes = loadKey()
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(128, iv) // 128-bit auth tag
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val plaintextBytes = attribute.toByteArray(Charsets.UTF_8)
        val cipherBytes = cipher.doFinal(plaintextBytes) // includes auth tag at end

        val out = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, out, 0, iv.size)
        System.arraycopy(cipherBytes, 0, out, iv.size, cipherBytes.size)

        return Base64.getEncoder().encodeToString(out)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        dbData ?: return null
        val keyBytes = loadKey()
        val allBytes = try {
            Base64.getDecoder().decode(dbData)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Encrypted column is not valid Base64", e)
        }
        if (allBytes.size < 13) { // must at least contain IV + 1 byte
            throw IllegalStateException("Encrypted column too short: missing IV/ciphertext")
        }
        val iv = allBytes.copyOfRange(0, 12)
        val cipherBytes = allBytes.copyOfRange(12, allBytes.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val plaintext = cipher.doFinal(cipherBytes)
        return plaintext.toString(Charsets.UTF_8)
    }

    private fun loadKey(): ByteArray {
        val candidate = System.getProperty("iko.crypto.key")
            ?: System.getenv("IKO_CRYPTO_KEY")
            ?: throw IllegalStateException("Missing AES key: set system property 'iko.crypto.key' or env 'IKO_CRYPTO_KEY' (Base64 or Hex, 32 bytes)")

        val keyBytes = decodeKey(candidate)
        if (keyBytes.size != 32) {
            throw IllegalStateException("Invalid AES key size: expected 32 bytes (256-bit), got ${'$'}{keyBytes.size}")
        }
        return keyBytes
    }

    private fun decodeKey(input: String): ByteArray {
        val trimmed = input.trim()
        // Try Base64 first
        try {
            val b64 = Base64.getDecoder().decode(trimmed)
            if (b64.isNotEmpty()) return b64
        } catch (ignored: IllegalArgumentException) {
            // not base64
        }
        // Try hex
        return hexToBytes(trimmed)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.removePrefix("0x").removePrefix("0X").replace("\n", "").replace(" ", "")
        require(clean.length % 2 == 0) { "Hex key must have even length" }
        val out = ByteArray(clean.length / 2)
        var i = 0
        while (i < clean.length) {
            val byte = clean.substring(i, i + 2).toInt(16)
            out[i / 2] = byte.toByte()
            i += 2
        }
        return out
    }
}

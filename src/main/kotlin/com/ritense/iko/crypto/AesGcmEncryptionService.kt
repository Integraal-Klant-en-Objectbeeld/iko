package com.ritense.iko.crypto

import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM AttributeConverter for encrypting/decrypting String values stored in DB.
 *
 * Storage format: Base64 encoding of (IV || CIPHERTEXT_WITH_TAG), where
 * - IV is 12 random bytes (96 bits)
 * - GCM auth tag is included at the end of the ciphertext by JCE
 */
@Service
class AesGcmEncryptionService(
    private val aesGcmSecretKeySpec: SecretKeySpec
) {

    fun encrypt(plainText: String): String {
        // Generate a random IV
        val iv = ByteArray(IV_LENGTH_ENCRYPT)
        SECURE_RANDOM.nextBytes(iv)

        // Initialize cipher in AES-GCM mode
        val cipher = CIPHER_THREAD_LOCAL.get()
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_ENCRYPT * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, aesGcmSecretKeySpec, gcmSpec)

        // Encrypt the plaintext
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        // Combine IV and encrypted text
        val combinedIvAndCipherText = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combinedIvAndCipherText, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combinedIvAndCipherText, iv.size, encryptedBytes.size)

        return BASE64_ENCODER.encodeToString(combinedIvAndCipherText)
    }

    fun decrypt(cipherText: String): String {
        // Decode Base64
        val combined = try {
            BASE64_DECODER.decode(cipherText)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Invalid Base64 for ciphertext", e)
        }
        require(combined.size > IV_LENGTH_ENCRYPT) { "Ciphertext too short" }

        // Extract IV and encrypted bytes (includes tag)
        val iv = combined.copyOfRange(0, IV_LENGTH_ENCRYPT)
        val encrypted = combined.copyOfRange(IV_LENGTH_ENCRYPT, combined.size)

        // Init cipher for decrypt
        val cipher = CIPHER_THREAD_LOCAL.get()
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_ENCRYPT * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, aesGcmSecretKeySpec, gcmSpec)

        val plainBytes = cipher.doFinal(encrypted)
        return String(plainBytes, StandardCharsets.UTF_8)
    }

    companion object {
        // Reuse encoders/decoders and SecureRandom for performance
        private val BASE64_ENCODER: Base64.Encoder = Base64.getEncoder()
        private val BASE64_DECODER: Base64.Decoder = Base64.getDecoder()
        private val SECURE_RANDOM: SecureRandom = SecureRandom()

        // Use per-thread Cipher to avoid repeated getInstance cost; Cipher is not thread-safe
        private val CIPHER_THREAD_LOCAL: ThreadLocal<Cipher> = ThreadLocal.withInitial {
            Cipher.getInstance(AES_ALGORITHM_GCM)
        }

        const val AES_ALGORITHM_GCM: String = "AES/GCM/NoPadding"
        const val IV_LENGTH_ENCRYPT: Int = 12
        const val TAG_LENGTH_ENCRYPT: Int = 16
    }

}
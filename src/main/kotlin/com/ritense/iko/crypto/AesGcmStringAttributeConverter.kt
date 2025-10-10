package com.ritense.iko.crypto

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class AesGcmStringAttributeConverter(
    private val aesGcmEncryptionService: AesGcmEncryptionService
) : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute == null) return null
        return aesGcmEncryptionService.encrypt(plainText = attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData == null) return null
        return aesGcmEncryptionService.decrypt(cipherText = dbData)
    }
}
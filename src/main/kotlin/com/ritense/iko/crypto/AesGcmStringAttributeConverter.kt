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

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class AesGcmStringAttributeConverter(
    private val aesGcmEncryptionService: AesGcmEncryptionService,
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
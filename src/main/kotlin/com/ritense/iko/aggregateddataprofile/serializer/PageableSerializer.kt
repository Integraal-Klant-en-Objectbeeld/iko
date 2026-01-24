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

package com.ritense.iko.aggregateddataprofile.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class PageableSerializer : JsonSerializer<Pageable>() {
    override fun serialize(value: Pageable, gen: JsonGenerator, serializers: SerializerProvider) {
        if (!value.isPaged) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeNumberField("pageNumber", value.pageNumber)
        gen.writeNumberField("pageSize", value.pageSize)
        if (value.sort.isSorted) {
            gen.writeArrayFieldStart("sort")
            value.sort.forEach { order ->
                gen.writeStartObject()
                gen.writeStringField("property", order.property)
                gen.writeStringField("direction", order.direction.name)
                if (order.isIgnoreCase) {
                    gen.writeBooleanField("ignoreCase", true)
                }
                if (order.nullHandling != Sort.NullHandling.NATIVE) {
                    gen.writeStringField("nullHandling", order.nullHandling.name)
                }
                gen.writeEndObject()
            }
            gen.writeEndArray()
        }
        gen.writeEndObject()
    }
}
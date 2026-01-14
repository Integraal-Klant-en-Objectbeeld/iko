package com.ritense.iko.json.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class PageableSerializer : JsonSerializer<Pageable>() {
    override fun serialize(value: Pageable, gen: JsonGenerator, serializers: SerializerProvider) {
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

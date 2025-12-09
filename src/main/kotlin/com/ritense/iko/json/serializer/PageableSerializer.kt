package com.ritense.iko.json.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class PageableSerializer : JsonSerializer<Pageable>() {
    override fun serialize(value: Pageable, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("pageNumber", value.pageNumber)
        gen.writeNumberField("pageSize", value.pageSize)
        if (value.sort.isSorted) {
            gen.writeFieldName("sort")
            serializers.defaultSerializeValue(value.sort, gen)
        }
        gen.writeEndObject()
    }
}
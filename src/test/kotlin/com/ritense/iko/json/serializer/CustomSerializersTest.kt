package com.ritense.iko.json.serializer

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class CustomSerializersTest {
    private val mapper = jacksonObjectMapper().registerModule(
        SimpleModule()
            .addSerializer(Pageable::class.java, PageableSerializer())
            .addDeserializer(Pageable::class.java, PageableDeserializer()),
    )

    @Test
    fun `serializes paged pageable with sort`() {
        val pageable = PageRequest.of(1, 5, Sort.by(Sort.Order.desc("name")))

        val json = mapper.writeValueAsString(pageable)
        val node = mapper.readTree(json)

        assertThat(node["pageNumber"].intValue()).isEqualTo(1)
        assertThat(node["pageSize"].intValue()).isEqualTo(5)
        val sortNode = node["sort"]
        assertThat(sortNode.isArray).isTrue()
        assertThat(sortNode[0]["property"].asText()).isEqualTo("name")
        assertThat(sortNode[0]["direction"].asText()).isEqualTo("DESC")
    }

    @Test
    fun `serializes unpaged pageable as null`() {
        val json = mapper.writeValueAsString(Pageable.unpaged())
        assertThat(json).isEqualTo("null")
    }

    @Test
    fun `deserializes null pageable to unpaged`() {
        val pageable: Pageable = mapper.readValue("null")
        assertThat(pageable.isPaged).isFalse()
    }

    @Test
    fun `deserializes object pageable`() {
        val json = """{"pageNumber":2,"pageSize":10,"sort":[{"property":"name","direction":"ASC"}]}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.pageNumber).isEqualTo(2)
        assertThat(pageable.pageSize).isEqualTo(10)
        assertThat(pageable.sort.getOrderFor("name")?.direction).isEqualTo(Sort.Direction.ASC)
    }

    @Test
    fun `deserializes query string pageable`() {
        val json = mapper.writeValueAsString("page=3&size=20&sort=name,desc")

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.pageNumber).isEqualTo(3)
        assertThat(pageable.pageSize).isEqualTo(20)
        assertThat(pageable.sort.getOrderFor("name")?.direction).isEqualTo(Sort.Direction.DESC)
    }

    @Test
    fun `deserializes invalid pageable to unpaged`() {
        val pageable: Pageable = mapper.readValue("""{"pageNumber":1,"pageSize":-1}""")

        assertThat(pageable.isPaged).isFalse()
    }
}
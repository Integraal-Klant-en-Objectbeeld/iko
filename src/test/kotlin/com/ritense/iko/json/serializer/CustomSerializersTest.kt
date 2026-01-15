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
    fun `deserializes alternative page and size keys`() {
        val json = """{"page":4,"size":15}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.pageNumber).isEqualTo(4)
        assertThat(pageable.pageSize).isEqualTo(15)
    }

    @Test
    fun `deserializes number and limit keys`() {
        val json = """{"number":7,"limit":25}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.pageNumber).isEqualTo(7)
        assertThat(pageable.pageSize).isEqualTo(25)
    }

    @Test
    fun `deserializes pageNumber and pageSize keys`() {
        val json = """{"pageNumber":9,"pageSize":50}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.pageNumber).isEqualTo(9)
        assertThat(pageable.pageSize).isEqualTo(50)
    }

    @Test
    fun `deserializes page and pageSize mixed keys`() {
        val json = """{"page":2,"pageSize":30}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.pageNumber).isEqualTo(2)
        assertThat(pageable.pageSize).isEqualTo(30)
    }

    @Test
    fun `deserializes pageNumber and size mixed keys`() {
        val json = """{"pageNumber":5,"size":12}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.pageNumber).isEqualTo(5)
        assertThat(pageable.pageSize).isEqualTo(12)
    }

    @Test
    fun `deserializes number and size mixed keys`() {
        val json = """{"number":3,"size":8}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.pageNumber).isEqualTo(3)
        assertThat(pageable.pageSize).isEqualTo(8)
    }

    @Test
    fun `deserializes number and pageSize mixed keys`() {
        val json = """{"number":4,"pageSize":9}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.pageNumber).isEqualTo(4)
        assertThat(pageable.pageSize).isEqualTo(9)
    }

    @Test
    fun `deserializes sort object with ignoreCase and nullHandling`() {
        val json = """{"pageNumber":1,"pageSize":3,"sort":{"property":"name","direction":"desc","ignoreCase":true,"nullHandling":"last"}}"""

        val pageable: Pageable = mapper.readValue(json)

        val order = pageable.sort.getOrderFor("name")
        assertThat(order?.direction).isEqualTo(Sort.Direction.DESC)
        assertThat(order?.isIgnoreCase).isTrue()
        assertThat(order?.nullHandling).isEqualTo(Sort.NullHandling.NATIVE)
    }

    @Test
    fun `deserializes sort array with shorthand field`() {
        val json = """{"pageNumber":0,"pageSize":5,"sort":[{"name":"asc"}]}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.sort.getOrderFor("name")?.direction).isEqualTo(Sort.Direction.ASC)
    }

    @Test
    fun `deserializes sort from query string with multiple fields`() {
        val json = mapper.writeValueAsString("page=1&size=2&sort=name,desc;ownerId,asc")

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.sort.getOrderFor("name")?.direction).isEqualTo(Sort.Direction.DESC)
        assertThat(pageable.sort.getOrderFor("ownerId")?.direction).isEqualTo(Sort.Direction.ASC)
    }

    @Test
    fun `deserializes invalid sort direction as asc`() {
        val json = """{"pageNumber":0,"pageSize":5,"sort":[{"property":"name","direction":"nope"}]}"""

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.sort.getOrderFor("name")?.direction).isEqualTo(Sort.Direction.ASC)
    }

    @Test
    fun `deserializes sort string single field defaults asc`() {
        val json = mapper.writeValueAsString("page=0&size=5&sort=name")

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.sort.getOrderFor("name")?.direction).isEqualTo(Sort.Direction.ASC)
    }

    @Test
    fun `deserializes sort string with explicit direction`() {
        val json = mapper.writeValueAsString("page=0&size=5&sort=name,DESC")

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.sort.getOrderFor("name")?.direction).isEqualTo(Sort.Direction.DESC)
    }

    @Test
    fun `deserializes sort string with multiple pairs`() {
        val json = mapper.writeValueAsString("page=0&size=5&sort=name,desc,ownerId,asc")

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.sort.getOrderFor("name")?.direction).isEqualTo(Sort.Direction.DESC)
        assertThat(pageable.sort.getOrderFor("ownerId")?.direction).isEqualTo(Sort.Direction.ASC)
    }

    @Test
    fun `deserializes sort string with extra whitespace`() {
        val json = mapper.writeValueAsString("page=0&size=5&sort= name , desc ; ownerId , ASC ")

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.sort.getOrderFor("name")?.direction).isEqualTo(Sort.Direction.DESC)
        assertThat(pageable.sort.getOrderFor("ownerId")?.direction).isEqualTo(Sort.Direction.ASC)
    }

    @Test
    fun `deserializes empty sort string as unsorted`() {
        val json = mapper.writeValueAsString("page=0&size=5&sort=")

        val pageable: Pageable = mapper.readValue(json)

        assertThat(pageable.sort.isSorted).isFalse()
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
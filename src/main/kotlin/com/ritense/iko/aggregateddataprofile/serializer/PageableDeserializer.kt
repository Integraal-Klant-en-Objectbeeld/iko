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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class PageableDeserializer : JsonDeserializer<Pageable>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Pageable {
        val node = parser.codec.readTree<JsonNode>(parser)
        return parsePageableNode(node)
    }

    override fun getNullValue(ctxt: DeserializationContext): Pageable = Pageable.unpaged()

    override fun getEmptyValue(ctxt: DeserializationContext): Pageable = Pageable.unpaged()

    private fun parsePageableNode(node: JsonNode?): Pageable {
        if (node == null || node.isNull) {
            return Pageable.unpaged()
        }
        if (node.isTextual) {
            return parseFromQuery(node.asText())
        }
        if (!node.isObject) {
            return Pageable.unpaged()
        }
        val pageNumber = node["page"]?.intValue()
            ?: node["pageNumber"]?.intValue()
            ?: node["number"]?.intValue()
            ?: 0
        val sizeValue = node["size"]?.intValue()
            ?: node["pageSize"]?.intValue()
            ?: node["limit"]?.intValue()
            ?: -1
        if (sizeValue < 0) {
            return Pageable.unpaged()
        }
        val sort = parseSortNode(node["sort"])
        return PageRequest.of(pageNumber, sizeValue, sort)
    }

    private fun parseFromQuery(raw: String): Pageable {
        val params = splitQueryParams(raw).mapNotNull { part ->
            val trimmed = part.trim()
            if (trimmed.isBlank()) {
                return@mapNotNull null
            }
            val keyValue = trimmed.split("=", limit = 2)
            val rawKey = keyValue.getOrNull(0)?.trim() ?: return@mapNotNull null
            val rawValue = keyValue.getOrNull(1)?.trim() ?: ""
            val decodedKey = URLDecoder.decode(rawKey, StandardCharsets.UTF_8).lowercase()
            val decodedValue = URLDecoder.decode(rawValue, StandardCharsets.UTF_8)
            decodedKey to decodedValue
        }.toMap()
        val pageNumber = params["page"]?.toIntOrNull()
            ?: params["pagenumber"]?.toIntOrNull()
            ?: params["number"]?.toIntOrNull()
            ?: 0
        val sizeValue = params["size"]?.toIntOrNull()
            ?: params["pagesize"]?.toIntOrNull()
            ?: -1
        if (sizeValue < 0) {
            return Pageable.unpaged()
        }
        val sort = parseSortFromString(params["sort"])
        return PageRequest.of(pageNumber, sizeValue, sort)
    }

    private fun splitQueryParams(raw: String): List<String> {
        if (raw.isBlank()) {
            return emptyList()
        }
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var hasEquals = false
        var index = 0
        while (index < raw.length) {
            val ch = raw[index]
            if ((ch == '&' || ch == ';') && hasEquals && shouldSplit(raw, index + 1)) {
                parts.add(current.toString())
                current.setLength(0)
                hasEquals = false
                index++
                continue
            }
            if (ch == '=' && !hasEquals) {
                hasEquals = true
            }
            current.append(ch)
            index++
        }
        if (current.isNotEmpty()) {
            parts.add(current.toString())
        }
        return parts
    }

    private fun shouldSplit(raw: String, startIndex: Int): Boolean {
        var idx = startIndex
        while (idx < raw.length) {
            val ch = raw[idx]
            if (ch == '&' || ch == ';') {
                return false
            }
            if (ch == '=') {
                return true
            }
            idx++
        }
        return false
    }

    private fun parseSortNode(node: JsonNode?): Sort {
        if (node == null || node.isNull) {
            return Sort.unsorted()
        }
        return when {
            node.isTextual -> parseSortFromString(node.asText())
            node.isArray -> {
                val orders = node.mapNotNull { parseOrder(it) }
                if (orders.isEmpty()) Sort.unsorted() else Sort.by(orders)
            }

            node.isObject -> {
                parseOrder(node)?.let { Sort.by(it) } ?: Sort.unsorted()
            }

            else -> Sort.unsorted()
        }
    }

    private fun parseOrder(node: JsonNode): Sort.Order? {
        val propertyNode = node["property"]
        if (propertyNode != null && propertyNode.isTextual) {
            var order = Sort.Order(parseDirection(node["direction"]?.asText()), propertyNode.asText())
            if (node["ignoreCase"]?.asBoolean() == true) {
                order = order.ignoreCase()
            }
            node["nullHandling"]?.asText()?.let {
                order = order.with(parseNullHandling(it))
            }
            return order
        }
        val fieldIterator = node.fieldNames()
        while (fieldIterator.hasNext()) {
            val field = fieldIterator.next()
            if (field == "direction" || field == "ignoreCase" || field == "nullHandling" || field == "property") {
                continue
            }
            val value = node[field]
            if (value != null && value.isTextual) {
                return Sort.Order(parseDirection(value.asText()), field)
            }
        }
        return null
    }

    private fun parseSortFromString(text: String?): Sort {
        if (text.isNullOrBlank()) {
            return Sort.unsorted()
        }
        val orders = mutableListOf<Sort.Order>()
        val segments = text.split(";")
        for (segment in segments) {
            val tokens = segment.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (tokens.isEmpty()) continue
            if (tokens.size == 1) {
                orders.add(Sort.Order(Sort.Direction.ASC, tokens[0]))
                continue
            }
            val chunked = tokens.chunked(2)
            for (chunk in chunked) {
                val property = chunk.getOrNull(0) ?: continue
                val direction = parseDirection(chunk.getOrNull(1))
                orders.add(Sort.Order(direction, property))
            }
        }
        return if (orders.isEmpty()) Sort.unsorted() else Sort.by(orders)
    }

    private fun parseDirection(value: String?): Sort.Direction = try {
        Sort.Direction.valueOf(value?.trim()?.uppercase() ?: Sort.Direction.ASC.name)
    } catch (ex: IllegalArgumentException) {
        Sort.Direction.ASC
    }

    private fun parseNullHandling(value: String?): Sort.NullHandling = try {
        Sort.NullHandling.valueOf(value?.trim()?.uppercase() ?: Sort.NullHandling.NATIVE.name)
    } catch (ex: IllegalArgumentException) {
        Sort.NullHandling.NATIVE
    }
}
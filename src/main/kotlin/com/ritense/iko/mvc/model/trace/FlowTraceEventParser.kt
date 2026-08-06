/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
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

package com.ritense.iko.mvc.model.trace

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.camel.spi.BacklogTracerEventMessage
import org.springframework.stereotype.Component

/**
 * A single [BacklogTracerEventMessage] parsed into the flat shape the [FlowTraceGraphBuilder] works with:
 * scalars lifted off the message plus its exchange variables, headers and (unescaped) body.
 */
internal data class FlowTraceEvent(
    val uid: Long,
    val exchangeId: String,
    val correlationExchangeId: String?,
    val done: Boolean,
    val failed: Boolean,
    val elapsed: Long,
    val routeId: String?,
    val toNodeShortName: String?,
    val toNodeLabel: String?,
    val variables: Map<String, String>,
    val headers: Map<String, String>,
    val body: String?,
    val httpStatus: Int?,
    val truncated: Boolean,
    val exception: FlowTraceExceptionInfo?,
)

/** Parses the BacklogTracer's JSON message dumps into [FlowTraceEvent]s. */
@Component
internal class FlowTraceEventParser(private val objectMapper: ObjectMapper) {

    fun parse(msg: BacklogTracerEventMessage): FlowTraceEvent {
        val root: JsonNode? = runCatching { objectMapper.readTree(msg.messageAsJSon) }.getOrNull()
        val message = root?.get("message")

        val variables = arrayToMap(message?.get("exchangeVariables"))
        val headers = arrayToMap(message?.get("headers"))
        val bodyNode = message?.get("body")
        val rawBody = bodyNode?.get("value")?.takeIf { !it.isNull }?.asText()
        val body = rawBody?.let { unescape(it) }
        val bodySize = bodyNode?.get("size")?.asLong()
        val truncated = (bodySize != null && body != null && bodySize > body.length) ||
            (body != null && body.length >= BODY_MAX_CHARS)

        return FlowTraceEvent(
            uid = msg.uid,
            exchangeId = msg.exchangeId,
            correlationExchangeId = msg.correlationExchangeId,
            done = msg.isDone,
            failed = msg.isFailed,
            elapsed = msg.elapsed,
            routeId = msg.routeId,
            toNodeShortName = msg.toNodeShortName,
            toNodeLabel = msg.toNodeLabel,
            variables = variables,
            headers = headers,
            body = body,
            httpStatus = headers[HTTP_RESPONSE_CODE_HEADER]?.toIntOrNull(),
            truncated = truncated,
            exception = if (msg.hasException()) parseException(msg.exceptionAsJSon) else null,
        )
    }

    private fun parseException(json: String?): FlowTraceExceptionInfo? {
        val node = json?.let { runCatching { objectMapper.readTree(it) }.getOrNull() }?.get("exception")
            ?: return FlowTraceExceptionInfo(null, null, null)
        return FlowTraceExceptionInfo(
            type = node.get("type")?.takeIf { !it.isNull }?.asText(),
            message = node.get("message")?.takeIf { !it.isNull }?.asText(),
            stacktrace = node.get("stackTrace")?.takeIf { !it.isNull }?.asText()?.let { unescape(it) },
        )
    }

    /**
     * Reverse Camel's `Jsoner.escape` applied to body/stacktrace values. Camel escapes these once more
     * than the surrounding JSON, so after Jackson parses the dump the value still carries JSON escape
     * sequences. This undoes exactly that one layer; input without backslashes is returned untouched.
     */
    private fun unescape(raw: String): String {
        if (!raw.contains('\\')) return raw
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                when (val n = raw[i + 1]) {
                    '"' -> sb.append('"')

                    '\\' -> sb.append('\\')

                    '/' -> sb.append('/')

                    'n' -> sb.append('\n')

                    'r' -> sb.append('\r')

                    't' -> sb.append('\t')

                    'b' -> sb.append('\b')

                    'f' -> sb.append('\u000C')

                    'u' -> {
                        val hex = if (i + 6 <= raw.length) raw.substring(i + 2, i + 6).toIntOrNull(16) else null
                        if (hex != null) {
                            sb.append(hex.toChar())
                            i += 6
                            continue
                        }
                        sb.append(c).append(n)
                    }

                    else -> sb.append(c).append(n)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun arrayToMap(arrayNode: JsonNode?): Map<String, String> {
        if (arrayNode == null || !arrayNode.isArray) return emptyMap()
        val map = LinkedHashMap<String, String>()
        arrayNode.forEach { entry ->
            val key = entry.get("key")?.asText() ?: return@forEach
            val value = entry.get("value")
            if (value != null && !value.isNull) {
                map[key] = value.asText()
            }
        }
        return map
    }

    companion object {
        private const val HTTP_RESPONSE_CODE_HEADER = "CamelHttpResponseCode"
        private const val BODY_MAX_CHARS = 32768
    }
}
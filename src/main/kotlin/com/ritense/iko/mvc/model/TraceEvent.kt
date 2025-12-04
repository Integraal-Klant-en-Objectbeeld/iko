package com.ritense.iko.mvc.model

import org.apache.camel.CamelContext
import org.apache.camel.spi.BacklogTracerEventMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TraceEvent(
    val id: String,
    val timestamp: String,
    val route: String,
    val routeDescription: String? = null,
    val processingThreadName: String,
    val toNode: String,
    val elapsed: String,
    val exchangeId: String,
    val location: String,
    val status: String,
) {
    companion object {
        private val dtf =
            DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                .withZone(ZoneId.systemDefault())

        fun from(backlogTracerEventMessage: BacklogTracerEventMessage, description: String? = null): TraceEvent {
            val status =
                when {
                    backlogTracerEventMessage.isFailed -> "FAILED"
                    backlogTracerEventMessage.isDone -> "OK"
                    else -> "IN_PROGRESS"
                }
            val timestamp = dtf.format(Instant.ofEpochMilli(backlogTracerEventMessage.timestamp))
            return TraceEvent(
                id = backlogTracerEventMessage.uid.toString(),
                timestamp = timestamp,
                route = backlogTracerEventMessage.routeId,
                routeDescription = description,
                processingThreadName = backlogTracerEventMessage.processingThreadName,
                toNode = backlogTracerEventMessage.toNode ?: "",
                elapsed = backlogTracerEventMessage.elapsed.toString() + "ms",
                exchangeId = backlogTracerEventMessage.exchangeId,
                location = backlogTracerEventMessage.location ?: "",
                status = status,
            )
        }
    }
}
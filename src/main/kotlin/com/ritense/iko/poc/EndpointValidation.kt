package com.ritense.iko.poc

import com.ritense.iko.poc.db.ConnectorEndpointRepository
import com.ritense.iko.poc.db.ConnectorInstanceRepository
import org.apache.camel.builder.RouteBuilder

class EndpointValidation(
    val connectorEndpointRepository: ConnectorEndpointRepository,
    val connectorInstanceRepository: ConnectorInstanceRepository

) : RouteBuilder() {
    override fun configure() {
        from(Iko.endpoint("validate"))
            .errorHandler(noErrorHandler())
            .process { ex ->
                val connector = ex.getVariable("connector", String::class.java)
                val config = ex.getVariable("config", String::class.java)
                val operation = ex.getVariable("operation", String::class.java)

                val connectorEndpoint = connectorEndpointRepository.findByConnectorTagAndOperation(connector, operation)
                    ?: throw EndpointValidationFailed("Unknown operation: $connector.$operation")
                val connectorInstance = connectorInstanceRepository.findByConnectorTagAndTag(connector, config)
                    ?: throw EndpointValidationFailed("Unknown instance: $connector.$config")

                ex.setVariable("connectorEndpointId", connectorEndpoint.id)
                ex.setVariable("connectorInstanceId", connectorInstance.id)
            }
    }
}
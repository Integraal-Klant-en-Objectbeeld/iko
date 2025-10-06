package com.ritense.iko.connectors

import com.ritense.iko.connectors.db.ConnectorInstanceRepository
import org.apache.camel.builder.RouteBuilder
import java.util.UUID

class ConnectorConfig(val connectorInstanceRepository: ConnectorInstanceRepository) : RouteBuilder() {
    override fun configure() {
        from("direct:iko:config")
            .process { exchange ->
                val connectorInstance =
                    (connectorInstanceRepository.findById(exchange.getVariable("connectorInstanceId", UUID::class.java))
                        ?: throw NoSuchElementException("Connector instance not found")).get()

                exchange.setVariable("configProperties", connectorInstance.config)
            }
    }
}
package com.ritense.iko.mvc.connector.pages

import com.ritense.iko.poc.db.Connector

class ConnectorListPage(
    val connectors: List<Connector>
) : Page<ConnectorListPage>("connector-list") {

    init {
        this.viewName = "fragments/internal/connector/listPage"
    }

    override fun attributes(): Map<String, Any> {
        return mapOf(
            "connectors" to connectors,
        )
    }

}
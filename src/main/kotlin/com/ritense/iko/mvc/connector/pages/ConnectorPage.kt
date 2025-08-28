package com.ritense.iko.mvc.connector.pages

import com.ritense.iko.mvc.controller.HomeController
import com.ritense.iko.poc.db.Connector
import com.ritense.iko.poc.db.ConnectorEndpoint
import com.ritense.iko.poc.db.ConnectorInstance
import org.springframework.web.servlet.ModelAndView

class ConnectorPage(
    val connector: Connector,
    val instances: List<ConnectorInstance>,
    val endpoints: List<ConnectorEndpoint>
) : Page<ConnectorPage>("details") {

    init {
        this.viewName = "fragments/internal/connector/detailsPage"
    }

    override fun attributes(): Map<String, Any> {
        return mapOf(
            "connector" to connector,
            "instances" to instances,
            "endpoints" to endpoints,
        )

    }

}
package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.controller.HomeController.Companion.HX_REQUEST_HEADER
import com.ritense.iko.mvc.model.Connector
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

/**
 * Controller responsible for displaying a list of available connectors and
 * allowing administrators to view configuration details for a specific
 * connector.  Connectors defined here represent the different external
 * services integrated into IKO (e.g., BAG, BRP, Open Zaak and the Objects API).
 */
@Controller
@RequestMapping("/admin")
class ConnectorController {

    /** List of connectors supported by the application.  Each connector
     * exposes an id used in paths, a display name, a brief description and
     * the route for its detail page.
     */
    private val connectors: List<Connector> = listOf(
        Connector(
            id = "bag",
            name = "BAG",
            description = "Kadaster BAG connector",
            route = "/admin/connectors/bag"
        ),
        Connector(
            id = "brp",
            name = "BRP",
            description = "Rijksdienst BRP connector",
            route = "/admin/connectors/brp"
        ),
        Connector(
            id = "openzaak",
            name = "Open Zaak",
            description = "Open Zaak API connector",
            route = "/admin/connectors/openzaak"
        ),
        Connector(
            id = "objectenapi",
            name = "Objects API",
            description = "Objects API connector",
            route = "/admin/connectors/objectenapi"
        ),
    )

    /**
     * Displays an overview of all connectors.  The list is rendered in a
     * dedicated template which can use HTMX to load connector details
     * asynchronously.  Menu items are provided so the sidebar remains
     * consistent.
     */
    @GetMapping("/connectors")
    fun list(
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        return if (isHxRequest) {
            ModelAndView("fragments/internal/connector/list")
        } else {
            ModelAndView("fragments/internal/connector/listPage")
        }.apply {
            addObject("connectors", connectors)
            addObject("menuItems", HomeController.menuItems)
        }
    }

    /**
     * Displays detailed configuration information for a specific connector.
     * In this simple implementation the configuration is static, but this
     * method could be extended to load environment variables or database
     * settings.  The detail page may be loaded directly or via HTMX into a
     * section of the list page.
     */
    @GetMapping("/connectors/{id}")
    fun details(
        @PathVariable id: String,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connector = connectors.firstOrNull { it.id == id }

        return if (isHxRequest) {
            ModelAndView("fragments/internal/connector/details")
        } else {
            ModelAndView("fragments/internal/connector/detailsPage")
        }.apply {
            addObject("connector", connector)
            addObject("menuItems", HomeController.menuItems)
        }
    }
}

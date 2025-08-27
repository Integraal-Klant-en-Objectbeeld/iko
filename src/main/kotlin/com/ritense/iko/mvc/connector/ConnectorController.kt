package com.ritense.iko.mvc.connector

import com.ritense.iko.mvc.controller.HomeController
import com.ritense.iko.mvc.controller.HomeController.Companion.HX_REQUEST_HEADER
import com.ritense.iko.poc.db.Connector
import com.ritense.iko.poc.db.ConnectorEndpoint
import com.ritense.iko.poc.db.ConnectorEndpointRepository
import com.ritense.iko.poc.db.ConnectorEndpointRole
import com.ritense.iko.poc.db.ConnectorEndpointRoleRepository
import com.ritense.iko.poc.db.ConnectorInstance
import com.ritense.iko.poc.db.ConnectorInstanceRepository
import com.ritense.iko.poc.db.ConnectorRepository
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import java.util.UUID

/**
 * Controller responsible for displaying a list of available connectors and
 * allowing administrators to view configuration details for a specific
 * connector.  Connectors defined here represent the different external
 * services integrated into IKO (e.g., BAG, BRP, Open Zaak and the Objects API).
 */
@Controller
@RequestMapping("/admin/connectors")
class ConnectorController(
    val connectorRepository: ConnectorRepository,
    val connectorInstanceRepository: ConnectorInstanceRepository,
    val connectorEndpointRepository: ConnectorEndpointRepository,
    val connectorEndpointRoleRepository: ConnectorEndpointRoleRepository
) {
    /**
     * Displays an overview of all connectors.  The list is rendered in a
     * dedicated template which can use HTMX to load connector details
     * asynchronously.  Menu items are provided so the sidebar remains
     * consistent.
     */
    @GetMapping()
    fun list(
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connectors = connectorRepository.findAll()

        return when (isHxRequest) {
            true -> ModelAndView("fragments/internal/connector/list")
            false -> ModelAndView("fragments/internal/connector/listPage")
        }.apply {
            addObject("connectors", connectors)
            addObject("menuItems", HomeController.Companion.menuItems)
        }
    }

    /**
     * Displays detailed configuration information for a specific connector.
     * In this simple implementation the configuration is static, but this
     * method could be extended to load environment variables or database
     * settings.  The detail page may be loaded directly or via HTMX into a
     * section of the list page.
     */
    @GetMapping("/{id}")
    fun details(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        val instances = connectorInstanceRepository.findByConnector(connector)
        val endpoints = connectorEndpointRepository.findByConnector(connector)

        return hxRequest(
            isHxRequest, "fragments/internal/connector/detailsPage", "details", mapOf(
                "connector" to connector,
                "instances" to instances,
                "endpoints" to endpoints,
                "menuItems" to HomeController.Companion.menuItems
            )
        )
    }

    @GetMapping("/{id}/edit")
    fun getEditConnectorPage(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        return hxRequest(
            isHxRequest, "fragments/internal/connector/editConnectorPage", "edit", mapOf(
                "connector" to connector,
                "form" to ConnectorEditForm(
                    connector.name,
                    connector.description,
                    connector.tag,
                    connector.connectorCode,
                ),
                "menuItems" to HomeController.Companion.menuItems,
                "saved" to false
            )
        )
    }

    @PutMapping("/{id}")
    fun editConnector(
        @PathVariable id: UUID,
        @Valid @ModelAttribute form: ConnectorEditForm,
        bindingResult: BindingResult,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        if (bindingResult.hasErrors()) {
            return getEditConnectorPage(id, isHxRequest)
        }

        connector.name = form.name
        connector.description = form.description
        connector.tag = form.reference
        connector.connectorCode = form.connectorCode
        connectorRepository.save(connector)

        return details(id, isHxRequest)
    }

    @PostMapping("")
    fun createConnector(
        @Valid @ModelAttribute form: ConnectorEditForm,
        bindingResult: BindingResult,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connector = Connector(
            id = UUID.randomUUID(),
            name = form.name,
            description = form.description,
            tag = form.reference,
            connectorCode = form.connectorCode
        )

        connectorRepository.save(connector)

        return details(connector.id, isHxRequest)
    }

    @GetMapping("/create")
    fun createConnector(
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        return hxRequest(
            isHxRequest, "fragments/internal/connector/createConnectorPage", "create", mapOf(
                "menuItems" to HomeController.Companion.menuItems,
                "created" to false
            )
        )
    }

    fun hxRequest(isHxRequest: Boolean, template: String, target: String, properties: Map<String, Any>): ModelAndView {
        return when (isHxRequest) {
            true -> ModelAndView("$template :: $target", properties)
            false -> ModelAndView(template, properties)
        }
    }

    @GetMapping("/{id}/instances/create")
    fun createConnectorInstancePage(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        return hxRequest(
            isHxRequest, "fragments/internal/connector/createConnectorInstancePage", "create", mapOf(
                "connector" to connectorRepository.findById(id).orElseThrow(),
                "menuItems" to HomeController.Companion.menuItems,
                "created" to false
            )
        )
    }

    @GetMapping("/{id}/instances/{instanceId}")
    fun editConnectorInstancePage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        val connectorRoles = connectorEndpointRoleRepository.findAllByConnectorInstance(
            connectorInstance
        )

        return hxRequest(
            isHxRequest, "fragments/internal/connector/editConnectorInstancePage", "edit", mapOf(
                "connector" to connector,
                "form" to ConnectorInstanceEditForm(
                    name = connectorInstance.name,
                    description = connectorInstance.description,
                    reference = connectorInstance.tag,
                ),
                "connectorRoles" to connectorRoles,
                "connectorInstance" to connectorInstance,
                "menuItems" to HomeController.Companion.menuItems,
                "saved" to false
            )
        )
    }

    @GetMapping("/{id}/instances/{instanceId}/config")
    fun ConnectorInstanceConfigPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        return hxRequest(
            isHxRequest, "fragments/internal/connector/createConnectorInstanceConfigPage", "create", mapOf(
                "connector" to connector,
                "connectorInstance" to connectorInstance,
                "menuItems" to HomeController.Companion.menuItems,
                "created" to false
            )
        )
    }


    @GetMapping("/{id}/instances/{instanceId}/roles")
    fun ConnectorInstanceRolesPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        val connectorEndpoints = connectorEndpointRepository.findAll()

        return hxRequest(
            isHxRequest, "fragments/internal/connector/createConnectorInstanceRolePage", "create", mapOf(
                "connector" to connector,
                "connectorInstance" to connectorInstance,
                "connectorEndpoints" to connectorEndpoints,
                "menuItems" to HomeController.Companion.menuItems,
                "created" to false
            )
        )
    }

    @PostMapping("/{id}/instances/{instanceId}/config")
    fun createConnectorInstanceConfigPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        @Valid @ModelAttribute form: ConnectorInstanceConfigEditForm,
        bindingResult: BindingResult,
    ): ModelAndView {
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }

        connectorInstance.config = connectorInstance.config.toMutableMap().apply {
            put(form.key, form.value)
        }

        connectorInstanceRepository.save(connectorInstance)

        return editConnectorInstancePage(id, instanceId, isHxRequest)
    }

    @PostMapping("/{id}/instances/{instanceId}/roles")
    fun createConnectorInstanceRolesPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        @Valid @ModelAttribute form: ConnectorInstanceRolesEditForm,
        bindingResult: BindingResult,
    ): ModelAndView {
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connectorEndpoint = connectorEndpointRepository.findById(form.endpointId)
            .orElseThrow { NoSuchElementException("Connector endpoint not found") }

        val role = ConnectorEndpointRole(
            id = UUID.randomUUID(),
            connectorInstance = connectorInstance,
            role = form.role,
            connectorEndpoint = connectorEndpoint
        )

        connectorEndpointRoleRepository.save(role)

        return editConnectorInstancePage(id, instanceId, isHxRequest)
    }

    @PostMapping("/{id}/instances")
    fun createConnectorInstance(
        @PathVariable id: UUID,
        @Valid @ModelAttribute form: ConnectorInstanceEditForm,
        bindingResult: BindingResult,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        response: HttpServletResponse
    ): ModelAndView {
        val connectorInstance = ConnectorInstance(
            id = UUID.randomUUID(),
            name = form.name,
            description = form.description,
            tag = form.reference,
            connector = connectorRepository.findById(id).orElseThrow(),
            config = mapOf()
        )

        connectorInstanceRepository.save(connectorInstance)
        response.setHeader("HX-Push-Url", "/admin/connectors/$id/instances/${connectorInstance.id}")
        return editConnectorInstancePage(id, connectorInstance.id, isHxRequest)
    }

    @GetMapping("/{id}/endpoints/create")
    fun createEndpointPage(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        return hxRequest(
            isHxRequest, "fragments/internal/connector/createEndpointPage", "create", mapOf(
                "connector" to connector,
                "menuItems" to HomeController.Companion.menuItems,
                "created" to false
            )
        )
    }


    @GetMapping("/{id}/endpoints/{endpointId}")
    fun editEndpointPage(
        @PathVariable id: UUID,
        @PathVariable endpointId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        val endpoint = connectorEndpointRepository.findById(endpointId)
            .orElseThrow { NoSuchElementException("Endpoint not found") }

        return hxRequest(
            isHxRequest, "fragments/internal/connector/editEndpointPage", "edit", mapOf(
                "connector" to connector,
                "endpoint" to endpoint,
                "form" to ConnectorEndpointConfigForm(
                    name = endpoint.name,
                    description = endpoint.description,
                    operation = endpoint.operation,
                ),
                "menuItems" to HomeController.Companion.menuItems,
                "saved" to false
            )
        )
    }

    @PutMapping("/{id}/endpoints/{endpointId}")
    fun editEndpoint(
        @PathVariable id: UUID,
        @PathVariable endpointId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        @Valid @ModelAttribute form: ConnectorEndpointConfigForm,
        bindingResult: BindingResult,
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        val endpoint = connectorEndpointRepository.findById(endpointId)
            .orElseThrow { NoSuchElementException("Endpoint not found") }

        endpoint.name = form.name
        endpoint.description = form.description
        endpoint.operation = form.operation

        connectorEndpointRepository.save(endpoint)

        return details(id, isHxRequest)
    }

    @PostMapping("/{id}/endpoints")
    fun createEndpoint(
        @PathVariable id: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        @Valid @ModelAttribute form: ConnectorEndpointConfigForm,
        bindingResult: BindingResult,
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        var connectorEndpoint = ConnectorEndpoint(
            id = UUID.randomUUID(),
            name = form.name,
            description = form.description,
            connector = connector,
            operation = form.operation,
        )

        connectorEndpoint = connectorEndpointRepository.save(connectorEndpoint)

        return details(id, isHxRequest)
    }

    @DeleteMapping("/{id}/endpoints/{endpointId}")
    fun deleteEndpoint(
        @PathVariable id: UUID,
        @PathVariable endpointId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        connectorEndpointRepository.deleteById(endpointId)

        return details(id, isHxRequest)
    }

    @DeleteMapping("/{id}/instances/{instanceId}")
    fun deleteInstance(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        connectorInstanceRepository.deleteById(instanceId)

        return details(id, isHxRequest)
    }

    @DeleteMapping("/{id}/instances/{instanceId}/roles/{roleId}")
    fun deleteRole(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @PathVariable roleId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        connectorEndpointRoleRepository.deleteById(roleId)

        return editConnectorInstancePage(id, instanceId, isHxRequest)
    }

    @DeleteMapping("/{id}/instances/{instanceId}/config/{configKey}")
    fun deleteConfig(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @PathVariable configKey: String,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        val instance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }

        instance.config = instance.config.toMutableMap().apply {
            remove(configKey)
        }

        connectorInstanceRepository.save(instance)

        return editConnectorInstancePage(id, instanceId, isHxRequest)
    }

    @GetMapping("/{id}/instances/{instanceId}/roles/{roleId}")
    fun createConnectorInstanceRolesPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @PathVariable roleId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
       val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        val connectorEndpoints = connectorEndpointRepository.findAll()
        val connectorEndpointRole = connectorEndpointRoleRepository.findById(roleId).orElseThrow { NoSuchElementException("Connector endpoint role not found") }

        return hxRequest(
            isHxRequest, "fragments/internal/connector/editConnectorInstanceRolePage", "edit", mapOf(
                "connector" to connector,
                "connectorInstance" to connectorInstance,
                "connectorEndpoints" to connectorEndpoints,
                "role" to connectorEndpointRole,
                "menuItems" to HomeController.Companion.menuItems,
                "form" to ConnectorInstanceRolesEditForm(
                    role = connectorEndpointRole.role,
                    endpointId = connectorEndpointRole.connectorEndpoint.id,
                ),
            )
        )
    }

    @PutMapping("/{id}/instances/{instanceId}/roles/{roleId}")
    fun createConnectorInstanceRolesPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @PathVariable roleId: UUID,
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        @Valid @ModelAttribute form: ConnectorInstanceRolesEditForm,
        bindingResult: BindingResult,
    ): ModelAndView {
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connectorEndpoint = connectorEndpointRepository.findById(form.endpointId)
            .orElseThrow { NoSuchElementException("Connector endpoint not found") }
        val role = connectorEndpointRoleRepository.findById(roleId).orElseThrow { NoSuchElementException("Connector endpoint role not found") }

        role.role = form.role
        role.connectorEndpoint = connectorEndpoint

        connectorEndpointRoleRepository.save(role)

        return editConnectorInstancePage(id, instanceId, isHxRequest)
    }
}
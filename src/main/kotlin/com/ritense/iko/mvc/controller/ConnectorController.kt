package com.ritense.iko.mvc.controller

import com.ritense.iko.connectors.domain.Connector
import com.ritense.iko.connectors.domain.ConnectorEndpoint
import com.ritense.iko.connectors.domain.ConnectorEndpointRole
import com.ritense.iko.connectors.domain.ConnectorInstance
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorEndpointRoleRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import com.ritense.iko.connectors.repository.ConnectorRepository
import com.ritense.iko.mvc.model.connector.ConnectorCreateForm
import com.ritense.iko.mvc.model.connector.ConnectorEditForm
import com.ritense.iko.mvc.model.connector.ConnectorEndpointConfigForm
import com.ritense.iko.mvc.model.connector.ConnectorInstanceConfigEditForm
import com.ritense.iko.mvc.model.connector.ConnectorInstanceEditForm
import com.ritense.iko.mvc.model.connector.ConnectorInstanceRolesEditForm
import com.ritense.iko.mvc.provider.SecurityContextHelper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.apache.camel.CamelContext
import org.apache.camel.support.PluginHelper
import org.apache.camel.support.ResourceHelper
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
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
    val connectorEndpointRoleRepository: ConnectorEndpointRoleRepository,
    val camelContext: CamelContext,
) {

    /**
     * Displays an overview of all connectors.  The list is rendered in a
     * dedicated template which can use HTMX to load connector details
     * asynchronously.  Menu items are provided so the sidebar remains
     * consistent.
     */
    @GetMapping()
    fun list(
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connectors = connectorRepository.findAll()

        return ModelAndView(
            "fragments/internal/connector/listPageConnectors" + when (isHxRequest) {
                true -> ":: connector-list"
                false -> ""
            },
            mapOf(
                "connectors" to connectors,
                "username" to SecurityContextHelper.getUserPropertyByKey("name"),
                "email" to SecurityContextHelper.getUserPropertyByKey("email")
            )
        )
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
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        val instances = connectorInstanceRepository.findByConnector(connector)
        val endpoints = connectorEndpointRepository.findByConnector(connector)

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnector" + when (isHxRequest) {
                true -> ":: details"
                false -> ""
            },
            mapOf(
                "connector" to connector,
                "instances" to instances,
                "endpoints" to endpoints,
                "username" to SecurityContextHelper.getUserPropertyByKey("name"),
                "email" to SecurityContextHelper.getUserPropertyByKey("email")
            )
        )
    }

    @GetMapping("/{id}/edit")
    fun getEditConnectorPage(
        @PathVariable id: UUID,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        return ModelAndView(
            "fragments/internal/connector/formEditConnectorCode",
            mapOf(
                "connector" to connector,
            )
        )
    }

    @PutMapping("/{id}")
    fun editConnector(
        @PathVariable id: UUID,
        @Valid @ModelAttribute form: ConnectorEditForm,
        bindingResult: BindingResult,
        httpServletResponse: HttpServletResponse
    ): ModelAndView {
        if (bindingResult.hasErrors()) {
            return ModelAndView(
                "fragments/internal/connector/formEditConnectorCode :: form",
                mapOf(
                    "connector" to form,
                    "errors" to bindingResult
                )
            )
        }
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        try {
            val resource = ResourceHelper.fromBytes(
                "${connector.tag}.yaml", form.connectorCode.toByteArray()
            )
            PluginHelper.getRoutesLoader(camelContext).loadRoutes(resource)
        } catch (e: Exception) {
            logger.error(e) { "Failed to log error code" }
            bindingResult.addError(FieldError("connectorEditForm", "connectorCode", "Not valid connector code"))
            return ModelAndView(
                "fragments/internal/connector/formEditConnectorCode :: form", mapOf(
                    "connector" to form,
                    "errors" to bindingResult
                )
            )
        }

        connector.connectorCode = form.connectorCode
        connectorRepository.save(connector)

        httpServletResponse.setHeader("HX-Retarget", "#connector-code")
        httpServletResponse.setHeader("HX-Trigger", "close-modal")
        httpServletResponse.setHeader("HX-Reswap", "outerHTML")

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnector :: connector-code", mapOf(
                "connector" to connector
            )
        )
    }

    @DeleteMapping("/{id}")
    fun deleteConnector(
        @PathVariable id: UUID,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        httpServletResponse: HttpServletResponse
    ): ModelAndView {
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        connectorEndpointRepository.findByConnector(connector).forEach { endpoint ->
            connectorEndpointRepository.delete(endpoint)
        }

        connectorInstanceRepository.findByConnector(connector).forEach { instance ->
            connectorEndpointRoleRepository.findAllByConnectorInstance(instance).forEach { role ->
                connectorEndpointRoleRepository.delete(role)
            }
            connectorInstanceRepository.delete(instance)
        }

        connectorRepository.delete(connector)

        httpServletResponse.setHeader("HX-Push-Url", "/admin/connectors")
        httpServletResponse.setHeader("HX-Retarget", "#view-panel")

        return list(isHxRequest)
    }

    @PostMapping("")
    fun createConnector(
        @Valid @ModelAttribute form: ConnectorCreateForm,
        bindingResult: BindingResult,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        httpServletResponse: HttpServletResponse
    ): ModelAndView {
        if (bindingResult.hasErrors()) {
            return ModelAndView(
                "fragments/internal/connector/formCreateConnector :: form", mapOf(
                    "connector" to form,
                    "errors" to bindingResult
                )
            )
        }

        val connector = Connector(
            id = UUID.randomUUID(),
            name = form.name,
            tag = form.reference,
            connectorCode = form.connectorCode
        )

        try {
            val resource = ResourceHelper.fromBytes(
                "${connector.tag}.yaml", connector.connectorCode.toByteArray()
            )
            PluginHelper.getRoutesLoader(camelContext).loadRoutes(resource)
        } catch (e: Exception) {
            logger.error(e) { "failed to load connector code" }
            bindingResult.addError(FieldError("connectorEditForm", "connectorCode", "Not valid connector code"))
            return ModelAndView(
                "fragments/internal/connector/formCreateConnector :: form", mapOf(
                    "connector" to form,
                    "errors" to bindingResult
                )
            )
        }

        connectorRepository.save(connector)

        httpServletResponse.setHeader("HX-Push-Url", "/admin/connectors/${connector.id}")
        httpServletResponse.setHeader("HX-Retarget", "#view-panel")
        httpServletResponse.setHeader("HX-Trigger", "close-modal")

        return details(connector.id, isHxRequest)
    }

    @GetMapping("/create")
    fun createConnector(): ModelAndView {
        return ModelAndView(
            "fragments/internal/connector/formCreateConnector",
            mapOf<String, Any?>()
        )
    }


    @GetMapping("/{id}/instances/create")
    fun createConnectorInstancePage(
        @PathVariable id: UUID,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        return ModelAndView(
            "fragments/internal/connector/formCreateConnectorInstance",
            mapOf<String, Any?>(
                "connectorId" to id
            )
        )
    }

    @GetMapping("/{id}/instances/{instanceId}")
    fun editConnectorInstancePage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @RequestHeader(HomeController.HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        val connectorRoles = connectorEndpointRoleRepository.findAllByConnectorInstance(
            connectorInstance
        )

        return hxRequest(
            isHxRequest, "fragments/internal/connector/detailsPageConnectorInstance", "details", mapOf(
                "connectorId" to connector.id,
                "instanceId" to connectorInstance.id,
                "connector" to connector,
                "instance" to connectorInstance,
                "connectorRoles" to connectorRoles,
                "connectorInstance" to connectorInstance
            )
        )
    }

    @GetMapping("/{id}/instances/{instanceId}/config")
    fun connectorInstanceConfigPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        return ModelAndView(
            "fragments/internal/connector/formCreateConnectorInstanceConfig",
            mapOf(
                "connectorId" to id,
                "instanceId" to instanceId
            )
        )
    }

    @PostMapping("/{id}/instances/{instanceId}/config")
    fun createConnectorInstanceConfigPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @Valid @ModelAttribute form: ConnectorInstanceConfigEditForm,
        bindingResult: BindingResult,
        httpServletResponse: HttpServletResponse
    ): ModelAndView {
        if (bindingResult.hasErrors()) {
            return ModelAndView(
                "fragments/internal/connector/formCreateConnectorInstanceConfig :: form", mapOf(
                    "connectorId" to id,
                    "instanceId" to instanceId,
                    "errors" to bindingResult
                )
            )
        }

        var connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }

        connectorInstance.config = connectorInstance.config.toMutableMap().apply {
            put(form.key, form.value)
        }

        connectorInstance = connectorInstanceRepository.save(connectorInstance)

        httpServletResponse.setHeader("HX-Retarget", "#config-table")
        httpServletResponse.setHeader("HX-Trigger", "close-modal")
        httpServletResponse.setHeader("HX-Reswap", "outerHTML")

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnectorInstance :: config-table", mapOf(
                "connector" to connectorInstance.connector,
                "connectorInstance" to connectorInstance,
                "instanceId" to instanceId,
            )
        )
    }

    @GetMapping("/{id}/instances/{instanceId}/roles")
    fun ConnectorInstanceRolesPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        return ModelAndView(
            "fragments/internal/connector/formCreateConnectorInstanceRole",
            mapOf(
                "connector" to connector,
                "connectorInstance" to connectorInstance,
                "connectorEndpoints" to connectorEndpointRepository.findByConnector(
                    connector
                )
            )
        )
    }


    @PostMapping("/{id}/instances/{instanceId}/roles")
    fun createConnectorInstanceRolesPage(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @Valid @ModelAttribute form: ConnectorInstanceRolesEditForm,
        bindingResult: BindingResult,
        httpServletResponse: HttpServletResponse
    ): ModelAndView {
        if (bindingResult.hasErrors()) {
            val connector =
                connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

            return ModelAndView(
                "fragments/internal/connector/formCreateConnectorInstanceRole :: form", mapOf(
                    "role" to form,
                    "errors" to bindingResult,
                    "connectorEndpoints" to connectorEndpointRepository.findByConnector(connector)
                )
            )
        }

        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val connectorEndpoint = connectorEndpointRepository.findById(UUID.fromString(form.endpointId))
            .orElseThrow { NoSuchElementException("Connector endpoint not found") }

        val role = ConnectorEndpointRole(
            id = UUID.randomUUID(),
            connectorInstance = connectorInstance,
            role = form.role,
            connectorEndpoint = connectorEndpoint
        )

        connectorEndpointRoleRepository.save(role)

        httpServletResponse.setHeader("HX-Retarget", "#roles-table")
        httpServletResponse.setHeader("HX-Trigger", "close-modal")
        httpServletResponse.setHeader("HX-Reswap", "outerHTML")

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnectorInstance :: roles-table", mapOf(
                "connector" to connectorInstance.connector,
                "connectorInstance" to connectorInstance,
                "instanceId" to instanceId,
                "connectorRoles" to connectorEndpointRoleRepository.findAllByConnectorInstance(
                    connectorInstance
                )
            )
        )
    }

    @PostMapping("/{id}/instances")
    fun createConnectorInstance(
        @PathVariable id: UUID,
        @Valid @ModelAttribute form: ConnectorInstanceEditForm,
        bindingResult: BindingResult,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        response: HttpServletResponse
    ): ModelAndView {
        if (bindingResult.hasErrors()) {
            return ModelAndView(
                "fragments/internal/connector/formCreateConnectorInstance :: form", mapOf(
                    "connectorId" to id,
                    "errors" to bindingResult
                )
            )
        }

        val connectorInstance = ConnectorInstance(
            id = UUID.randomUUID(),
            name = form.name,
            tag = form.reference,
            connector = connectorRepository.findById(id).orElseThrow(),
            config = mapOf()
        )

        connectorInstanceRepository.save(connectorInstance)

        response.setHeader("HX-Push-Url", "/admin/connectors/$id/instances/${connectorInstance.id}")
        response.setHeader("HX-Retarget", "#view-panel")
        response.setHeader("HX-Trigger", "close-modal")

        return editConnectorInstancePage(id, connectorInstance.id, isHxRequest)
    }

    @GetMapping("/{id}/endpoints/create")
    fun createEndpointPage(
        @PathVariable id: UUID,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    ): ModelAndView {
        return ModelAndView(
            "fragments/internal/connector/formCreateConnectorEndpoint", mapOf(
                "connectorId" to id
            )
        )
    }

    @GetMapping("/{id}/endpoints/{endpointId}")
    fun editEndpointPage(
        @PathVariable id: UUID,
        @PathVariable endpointId: UUID,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false,
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
                    operation = endpoint.operation,
                ),
                "menuItems" to HomeController.menuItems,
                "saved" to false
            )
        )
    }

    @PostMapping("/{id}/endpoints")
    fun createEndpoint(
        @PathVariable id: UUID,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        @Valid @ModelAttribute form: ConnectorEndpointConfigForm,
        bindingResult: BindingResult,
        response: HttpServletResponse,
    ): ModelAndView {
        if (bindingResult.hasErrors()) {
            return ModelAndView(
                "fragments/internal/connector/formCreateConnectorEndpoint :: form", mapOf(
                    "connectorId" to id,
                    "endpoint" to form,
                    "errors" to bindingResult
                )
            )
        }

        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        var connectorEndpoint = ConnectorEndpoint(
            id = UUID.randomUUID(),
            name = form.name,
            connector = connector,
            operation = form.operation,
        )

        connectorEndpointRepository.save(connectorEndpoint)

        response.setHeader("HX-Retarget", "#endpoints-table")
        response.setHeader("HX-Trigger", "close-modal")
        response.setHeader("HX-Reswap", "outerHTML")

        val endpoints = connectorEndpointRepository.findByConnector(connector)

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnector :: endpoints-table", mapOf(
                "connector" to connector,
                "endpoints" to endpoints,
            )
        )
    }

    @DeleteMapping("/{id}/endpoints/{endpointId}")
    fun deleteEndpoint(
        @PathVariable id: UUID,
        @PathVariable endpointId: UUID,
    ): ModelAndView {
        connectorEndpointRepository.deleteById(endpointId)

        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnector :: endpoints-table", mapOf(
                "connector" to connector,
                "endpoints" to connectorEndpointRepository.findByConnector(
                    connector
                )
            )
        )
    }

    @DeleteMapping("/{id}/instances/{instanceId}")
    fun deleteInstance(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
    ): ModelAndView {
        connectorInstanceRepository.deleteById(instanceId)

        val connector = connectorRepository.findById(id).orElseThrow { NoSuchElementException("Connector not found") }
        val instances = connectorInstanceRepository.findByConnector(connector)

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnector :: instance-table", mapOf(
                "connector" to connector,
                "instances" to instances
            )
        )
    }

    @DeleteMapping("/{id}/instances/{instanceId}/roles/{roleId}")
    fun deleteRole(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @PathVariable roleId: UUID,
    ): ModelAndView {
        connectorEndpointRoleRepository.deleteById(roleId)
        val connectorInstance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnectorInstance :: roles-table", mapOf(
                "connector" to connectorInstance.connector,
                "connectorInstance" to connectorInstance,
                "connectorRoles" to connectorEndpointRoleRepository.findAllByConnectorInstance(connectorInstance)
            )
        )
    }

    @DeleteMapping("/{id}/instances/{instanceId}/config/{configKey}")
    fun deleteConfig(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @PathVariable configKey: String,
    ): ModelAndView {
        val instance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }

        instance.config = instance.config.toMutableMap().apply {
            remove(configKey)
        }

        connectorInstanceRepository.save(instance)

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnectorInstance :: config-table", mapOf(
                "connector" to instance.connector,
                "connectorInstance" to instance,
            )
        )
    }

    @GetMapping("/{id}/instances/{instanceId}/config/{configKey}")
    fun getConfigValue(
        @PathVariable id: UUID,
        @PathVariable instanceId: UUID,
        @PathVariable configKey: String
    ): ModelAndView {
        val instance = connectorInstanceRepository.findById(instanceId)
            .orElseThrow { NoSuchElementException("Connector instance not found") }
        val value = instance.config[configKey]
        return ModelAndView(
            "fragments/internal/connector/configValue", mapOf(
                "configValue" to value
            )
        )
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        fun hxRequest(
            isHxRequest: Boolean,
            template: String,
            target: String,
            properties: Map<String, Any>
        ): ModelAndView {
            return when (isHxRequest) {
                true -> ModelAndView("$template :: $target", properties)
                false -> ModelAndView(template, properties)
            }
        }
    }
}
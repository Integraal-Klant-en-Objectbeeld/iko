---
date: 2026-01-28T15:45:00Z
researcher: Claude
git_commit: b99beb5272268dcc5063e61ded4ca31b91b7d7d6
branch: main
repository: iko
topic: "Implementation Plan: Draft Versioning System"
tags: [implementation-plan, versioning, aggregated-data-profile, connector, camel, semver]
status: completed
last_updated: 2026-01-30
last_updated_by: Claude
last_updated_note: "ALL PHASES COMPLETED (1-9). Fixed: duplicate tag migration, @Service annotation for CGLIB proxy, template fragment visibility (th:block)."
---

# Implementation Plan: Draft Versioning System

## Progress Summary

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 1 | ✅ COMPLETED | Route groups for ADP and Connector |
| Phase 2 | ✅ COMPLETED | ADP name immutability, DB migration (handles duplicate tags) |
| Phase 3 | ✅ COMPLETED | Version embeddable, domain model updates |
| Phase 4 | ✅ COMPLETED | Repository version query methods |
| Phase 5 | ✅ COMPLETED | Service layer (activateVersion, createNewVersion) |
| Phase 6 | ✅ COMPLETED | TestController lazy loading for non-active versions |
| Phase 7 | ✅ COMPLETED | Admin UI (version dropdown, create version modal, activate button) |
| Phase 8 | ✅ COMPLETED | Remove automatic route loading on create/edit |
| Phase 9 | ✅ COMPLETED | CSS styles for versioned page header |

### Key Files Modified (Phases 1-6)

**Domain:**
- `AggregatedDataProfile.kt` - Added `version`, `isActive`, `createNewVersion()`
- `Relation.kt` - Added `copyForNewVersion()`
- `Connector.kt` - Added `version`, `isActive`, `createNewVersion()`
- `ConnectorInstance.kt` - Added `copyForNewConnector()`
- `Version.kt` (new) - Embeddable with semver validation

**Repositories:**
- `AggregatedDataProfileRepository.kt` - Version query methods
- `ConnectorRepository.kt` - Version query methods
- `ConnectorEndpointRepository.kt` - Added `findByConnectorAndName()`

**Services:**
- `AggregatedDataProfileService.kt` - `activateVersion()`, `createNewVersion()`, startup loads only active, converted to `@Service` annotation (was `@Bean` factory)
- `ConnectorService.kt` - `activateVersion()`, `createNewVersion()`
- `ConnectorConfiguration.kt` - Startup loads only active connectors

**Configuration:**
- `AggregatedDataProfileConfiguration.kt` - Removed `@Bean aggregatedDataProfileService()` (now uses `@Service` on class)

**Controllers:**
- `AggregatedDataProfileController.kt` - Changed to `internal` visibility
- `TestController.kt` - Lazy route loading/suspension for non-active versions, changed to `internal` visibility

**MVC/Validation:**
- `AggregatedDataProfileEditForm.kt` - Removed `name` field
- `TestAggregatedDataProfileForm.kt` - Added `version` field
- `ValidSemver.kt`, `ValidSemverValidator.kt` (new)

**Templates:**
- `layout-internal.html` - Fixed `page-header-versioned` fragment to use `th:block` (prevents rendering on non-versioned pages)
- `edit.html` - Name field now disabled
- `debug.html` - Added version input, updated hx-include

**Database:**
- `V2026.01.29.1__add_versioning.sql` - Version and is_active columns

## Overview

This plan describes how to implement a draft/versioning system for AggregatedDataProfiles and Connectors where:
- Editing does not affect currently running routes until explicitly activated
- Versions use semantic versioning (semver)
- Only one version is active at a time per entity
- Non-active versions can be tested via the Preview tab (lazy loading in TestController)
- Relations and ConnectorInstances are duplicated per version (not shared)

## Strategy

**Option 1: Version field on domain entity** - Add `version: String` (semver format "1.0.0") and `isActive: Boolean` to ADP and Connector entities. Each version is a separate row in the database with the same logical "name" but different version.

## Phase 1: Route Group Implementation ✅ COMPLETED

Instead of explicitly tracking and removing individual route IDs, we use Camel's **route groups** feature. Each ADP and Connector gets a group name based on its entity ID. When removing routes, we simply find all routes in that group and remove them - no need to track individual route ID suffixes.

### 1.1 Add Route Groups to AggregatedDataProfileRouteBuilder ✅

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/AggregatedDataProfileRouteBuilder.kt`

**Current Problem**: Routes are created with various suffixes (`_root`, `_endpoint_transform`, `_multicast`, `_map`, `_array`, `_loop`) but `removeRoutes()` tries to remove with wrong suffixes (`_direct`).

**Solution**: Add `.group()` to all routes with the ADP ID as the group name.

**Changes to `configure()` method (lines 68-103)**:

```kotlin
// profile root route entrypoint
from("direct:aggregated_data_profile_${aggregatedDataProfile.id}")
    .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_root")
    .group("adp_${aggregatedDataProfile.id}")  // ADD THIS
    .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
    .routeDescription("[ADP Root]")
    // ... rest of route ...

from("direct:aggregated_data_profile_${aggregatedDataProfile.id}_endpoint_transform")
    .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_endpoint_transform")
    .group("adp_${aggregatedDataProfile.id}")  // ADD THIS
    .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
    // ... rest of route ...

// In the multicast block (lines 127-140):
var multicast = from("direct:multicast_${aggregatedDataProfile.id}")
    .routeId("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")
    .group("adp_${aggregatedDataProfile.id}")  // ADD THIS
    .multicast(MapAggregator)
    // ...
```

**Changes to `buildRelationRoute()` method (lines 143-265)**:

All relation routes should also use the **ADP's group** (not the relation's ID), so they're removed together with the parent ADP:

```kotlin
from("direct:relation_${currentRelation.id}")
    .routeId("relation_${currentRelation.id}_root")
    .group("adp_${aggregatedDataProfile.id}")  // ADD THIS - uses parent ADP ID
    .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
    // ...

from("direct:relation_${currentRelation.id}_map")
    .routeId("relation_${currentRelation.id}_map")
    .group("adp_${aggregatedDataProfile.id}")  // ADD THIS
    .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
    // ...

from("direct:relation_${currentRelation.id}_array")
    .routeId("relation_${currentRelation.id}_array")
    .group("adp_${aggregatedDataProfile.id}")  // ADD THIS
    .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
    // ...

from("direct:relation_${currentRelation.id}_loop")
    .routeId("relation_${currentRelation.id}_loop")
    .group("adp_${aggregatedDataProfile.id}")  // ADD THIS
    .routeConfigurationId(GLOBAL_ERROR_HANDLER_CONFIGURATION)
    // ...

// For nested relation multicast (lines 251-264):
var multicast = from("direct:multicast_${currentRelation.id}")
    .routeId("relation_${currentRelation.id}_multicast")
    .group("adp_${aggregatedDataProfile.id}")  // ADD THIS
    .multicast(MapAggregator)
    // ...
```

### 1.2 Simplify Route Removal in AggregatedDataProfileService ✅

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/service/AggregatedDataProfileService.kt`

**Current Code (lines 51-59)** - buggy, doesn't remove all routes:
```kotlin
fun removeRoutes(aggregatedDataProfile: AggregatedDataProfile) {
    removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_direct")  // WRONG suffix
    removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")
    aggregatedDataProfile.relations.forEach { relation ->
        removeRoute("relation_${relation.id}_direct")  // WRONG suffix
        removeRoute("relation_${relation.id}_multicast")
    }
}
```

**New Code** - uses route groups:
```kotlin
fun removeRoutes(aggregatedDataProfile: AggregatedDataProfile) {
    val groupName = "adp_${aggregatedDataProfile.id}"
    camelContext.getRoutesByGroup(groupName).forEach { route ->
        camelContext.routeController.stopRoute(route.id)
        camelContext.removeRoute(route.id)
    }
}
```

This is simpler, correct, and automatically handles any new route types added in the future.

### 1.3 Create ConnectorService and Consolidate Route Loading ✅

**Current Problem**:
1. Connector route loading code is duplicated in `ConnectorConfiguration.kt` (startup) and `ConnectorController.kt` (create/edit)
2. Routes loaded from YAML have no group, making them impossible to remove without parsing YAML to find route IDs

**Solution**:
1. Create a `ConnectorService` that centralizes all connector route operations
2. Use Camel's `RoutesLoader.findRoutesBuilders()` to parse YAML into `RouteDefinition` objects
3. Modify `RouteDefinition.setGroup()` before loading into CamelContext
4. This avoids YAML string manipulation and uses Camel's native API

**File**: `src/main/kotlin/com/ritense/iko/connectors/service/ConnectorService.kt` (new file)

```kotlin
@Service
internal class ConnectorService(
    private val connectorRepository: ConnectorRepository,
    private val camelContext: CamelContext,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Loads connector routes into CamelContext with group set for easy removal.
     * Uses Camel's RouteDefinition API instead of YAML string manipulation.
     *
     * Pattern: Parse YAML → Modify RouteDefinitions → Add via addRoutes()
     * This matches the existing AggregatedDataProfileService pattern.
     */
    fun loadConnectorRoutes(connector: Connector) {
        val groupName = "connector_${connector.id}"

        // Step 1: Create resource from YAML (filename must end in .yaml)
        val resource = ResourceHelper.fromString(
            "${connector.tag}.yaml",
            connector.connectorCode
        )

        // Step 2: Parse YAML to RoutesBuilder objects WITHOUT loading into context
        val loader = PluginHelper.getRoutesLoader(camelContext)
        val builders = loader.findRoutesBuilders(listOf(resource))

        // Step 3: For each builder, configure it, modify route definitions, then add to context
        builders.forEach { builder ->
            val routeBuilder = builder as RouteBuilder
            routeBuilder.setCamelContext(camelContext)
            routeBuilder.configure()

            // Set group on each route definition BEFORE adding to context
            routeBuilder.routeCollection.routes.forEach { routeDef ->
                routeDef.group(groupName)
            }

            // Add routes using standard CamelContext.addRoutes() - same pattern as AggregatedDataProfileService
            camelContext.addRoutes(routeBuilder)
        }

        logger.info { "Loaded ${builders.size} route builder(s) for connector ${connector.tag} with group $groupName" }
    }

    /**
     * Validates connector YAML by attempting to parse it.
     * Returns true if valid, throws exception with details if invalid.
     */
    fun validateConnectorCode(connectorCode: String, tag: String): Boolean {
        val resource = ResourceHelper.fromString("$tag.yaml", connectorCode)
        val loader = PluginHelper.getRoutesLoader(camelContext)

        // This will throw if YAML is invalid
        val builders = loader.findRoutesBuilders(listOf(resource))

        // Validate by calling configure() without adding to context
        builders.forEach { builder ->
            val routeBuilder = builder as RouteBuilder
            routeBuilder.setCamelContext(camelContext)
            routeBuilder.configure()
        }

        return true
    }

    /**
     * Removes all routes belonging to a connector using route groups.
     */
    fun removeConnectorRoutes(connector: Connector) {
        val groupName = "connector_${connector.id}"
        camelContext.getRoutesByGroup(groupName).forEach { route ->
            camelContext.routeController.stopRoute(route.id)
            camelContext.removeRoute(route.id)
        }
        logger.info { "Removed routes for connector ${connector.tag} (group: $groupName)" }
    }
}
```

**Note**: This uses `camelContext.addRoutes(routeBuilder)` which is the same pattern used in `AggregatedDataProfileService.addRoutes()` (line 62-70). The route definitions are modified (group set) before the RouteBuilder is added to the context.

### 1.4 Update ConnectorConfiguration to Use ConnectorService ✅

**File**: `src/main/kotlin/com/ritense/iko/connectors/autoconfiguration/ConnectorConfiguration.kt`

**Remove** the `loadAllConnectorsAtStartup()` method (lines 78-98) and replace with delegation to ConnectorService:

```kotlin
@Configuration
class ConnectorConfiguration(
    val connectorRepository: ConnectorRepository,
    val connectorService: ConnectorService,  // ADD dependency
) {
    // ... existing @Bean methods ...

    @EventListener(ApplicationReadyEvent::class)
    fun loadAllConnectorsAtStartup(event: ApplicationReadyEvent) {
        connectorRepository.findAll().forEach { connector ->
            try {
                connectorService.loadConnectorRoutes(connector)
            } catch (e: Exception) {
                logger.error(e) { "Failed to load connector ${connector.tag}" }
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
```

### 1.5 Create ValidConnectorCode Annotation and Validator ✅

Instead of manual try/catch in the controller, create a custom validation annotation like `ValidTransform`. This integrates with Spring's `@Valid` annotation for cleaner code.

**File**: `src/main/kotlin/com/ritense/iko/mvc/model/validation/ValidConnectorCode.kt` (new file)

```kotlin
package com.ritense.iko.mvc.model.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidConnectorCodeValidator::class])
annotation class ValidConnectorCode(
    val message: String = "Invalid connector code",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
```

**File**: `src/main/kotlin/com/ritense/iko/mvc/model/validation/ValidConnectorCodeValidator.kt` (new file)

```kotlin
package com.ritense.iko.mvc.model.validation

import com.ritense.iko.connectors.service.ConnectorService
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidConnectorCodeValidator(
    private val connectorService: ConnectorService,
) : ConstraintValidator<ValidConnectorCode, String> {

    override fun isValid(
        connectorCode: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (connectorCode.isNullOrBlank()) return true // let @NotBlank handle this

        return try {
            // Use a placeholder tag for validation - the tag doesn't affect YAML parsing
            connectorService.validateConnectorCode(connectorCode, "validation-check")
            true
        } catch (e: Exception) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate("Invalid connector code: ${e.message}")
                .addConstraintViolation()
            false
        }
    }
}
```

### 1.6 Update Form Classes with ValidConnectorCode Annotation ✅

**File**: `src/main/kotlin/com/ritense/iko/mvc/model/connector/ConnectorCreateForm.kt`

```kotlin
package com.ritense.iko.mvc.model.connector

import com.ritense.iko.mvc.model.validation.ValidConnectorCode
import jakarta.validation.constraints.NotBlank

data class ConnectorCreateForm(
    @field:NotBlank(message = "Please provide a name.")
    val name: String,
    @field:NotBlank(message = "Please provide a reference.")
    val reference: String,
    @field:NotBlank(message = "Please provide a connector code.")
    @field:ValidConnectorCode  // ADD THIS
    val connectorCode: String,
)
```

**File**: `src/main/kotlin/com/ritense/iko/mvc/model/connector/ConnectorEditForm.kt`

```kotlin
package com.ritense.iko.mvc.model.connector

import com.ritense.iko.mvc.model.validation.ValidConnectorCode
import jakarta.validation.constraints.NotBlank

data class ConnectorEditForm(
    @field:NotBlank(message = "Please provide a connector code.")
    @field:ValidConnectorCode  // ADD THIS
    val connectorCode: String,
)
```

### 1.7 Update ConnectorController to Remove Manual Validation ✅

**File**: `src/main/kotlin/com/ritense/iko/mvc/controller/ConnectorController.kt`

**Remove** the `camelContext` dependency and manual try/catch validation blocks. The `@Valid` annotation on form parameters will now trigger `ValidConnectorCodeValidator` automatically via `bindingResult.hasErrors()`.

### 1.8 Update Templates to Display Validation Errors ✅

**Files**:
- `src/main/resources/templates/fragments/internal/connector/formCreateConnector.html`
- `src/main/resources/templates/fragments/internal/connector/detailsPageConnector.html`

Added conditional `monaco-editor-error` class to Monaco editor divs and error message display using `cds--form-requirement` class, following the same pattern used in other Monaco editors in the codebase.

```kotlin
@Controller
@RequestMapping("/admin/connectors")
class ConnectorController(
    val connectorRepository: ConnectorRepository,
    val connectorInstanceRepository: ConnectorInstanceRepository,
    val connectorEndpointRepository: ConnectorEndpointRepository,
    val connectorEndpointRoleRepository: ConnectorEndpointRoleRepository,
    // REMOVE: val camelContext: CamelContext,
) {
    // In editConnector() - REMOVE lines 162-176 (try/catch block)
    // The @Valid @ModelAttribute form: ConnectorEditForm handles validation
    @PutMapping("/{id}")
    fun editConnector(
        @PathVariable id: UUID,
        @Valid @ModelAttribute form: ConnectorEditForm,
        bindingResult: BindingResult,
        httpServletResponse: HttpServletResponse,
    ): Any {
        val connector = connectorRepository.findById(id).orElseThrow()

        if (bindingResult.hasErrors()) {
            return ModelAndView(
                "fragments/internal/connector/detailsPageConnector :: connector-code",
                mapOf(
                    "connector" to connector,
                    "errors" to bindingResult,
                ),
            )
        }

        connector.connectorCode = form.connectorCode
        connectorRepository.save(connector)

        // Note: Don't load routes here - versioning will handle activation

        httpServletResponse.setHeader("HX-Retarget", "#connector-code")
        httpServletResponse.setHeader("HX-Trigger", "close-modal")
        httpServletResponse.setHeader("HX-Reswap", "outerHTML")

        return ModelAndView(
            "fragments/internal/connector/detailsPageConnector :: connector-code",
            mapOf("connector" to connector),
        )
    }

    // In createConnector() - REMOVE lines 246-263 (try/catch block)
    // The @Valid @ModelAttribute form: ConnectorCreateForm handles validation
    @PostMapping("")
    fun createConnector(
        @Valid @ModelAttribute form: ConnectorCreateForm,
        bindingResult: BindingResult,
        @RequestHeader(HomeController.Companion.HX_REQUEST_HEADER) isHxRequest: Boolean = false,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        if (bindingResult.hasErrors()) {
            return ModelAndView(
                "fragments/internal/connector/formCreateConnector :: form",
                mapOf(
                    "connector" to form,
                    "errors" to bindingResult,
                ),
            )
        }

        val connector = Connector(
            id = UUID.randomUUID(),
            name = form.name,
            tag = form.reference,
            connectorCode = form.connectorCode,
        )
        connectorRepository.save(connector)

        // Note: Don't load routes here - versioning will handle activation

        httpServletResponse.setHeader("HX-Push-Url", "/admin/connectors/${connector.id}")
        httpServletResponse.setHeader("HX-Retarget", "#view-panel")
        httpServletResponse.setHeader("HX-Trigger", "close-modal")

        return details(connector.id, isHxRequest)
    }
}
```

### 1.8 Update Templates to Display Validation Errors

**File**: `src/main/resources/templates/fragments/internal/connector/detailsPageConnector.html`

Update the `connector-code` fragment (around line 97-121) to display validation errors:

```html
<div
    id="connector-code"
    style="margin: 0.5em"
    th:fragment="connector-code"
>
    <textarea
        id="connectorCode"
        name="connectorCode"
        th:text="${connector?.connectorCode}"
        hidden
    ></textarea>

    <!-- Monaco container -->
    <div
        id="connector-editor"
        data-monaco
        data-language="yaml"
        data-textarea="#connectorCode"
        data-readonly="true"
        th:data-initial="${connector.connectorCode}"
        class="monaco-editor-style-full-height"
    ></div>

    <!-- Error display for validation errors -->
    <div
        id="monaco-error"
        class="cds--form-requirement"
        th:if="${errors?.getFieldError('connectorCode') != null}"
        th:text="${errors?.getFieldError('connectorCode')?.defaultMessage}"
    ></div>
</div>
```

**File**: `src/main/resources/templates/fragments/internal/connector/formCreateConnector.html`

The template already has error binding (lines 82-86) - no changes needed. It already displays `errors?.getFieldError('connectorCode')?.defaultMessage`.

### 1.10 Summary of Refactoring

| Before | After |
|--------|-------|
| `ConnectorConfiguration.loadAllConnectorsAtStartup()` - direct Camel calls | Delegates to `ConnectorService.loadConnectorRoutes()` |
| `ConnectorController.editConnector()` - manual try/catch validation | `@ValidConnectorCode` annotation + `bindingResult.hasErrors()` |
| `ConnectorController.createConnector()` - manual try/catch validation | `@ValidConnectorCode` annotation + `bindingResult.hasErrors()` |
| No route groups | All routes get `group("connector_{id}")` automatically |
| Cannot remove connector routes cleanly | `ConnectorService.removeConnectorRoutes()` uses `getRoutesByGroup()` |
| Error returned as HTTP 422 plain text | Error bound to form field, displayed in template |

**Benefits of custom validation annotation:**
1. **Consistent pattern**: Same as `ValidTransform` for JQ expressions
2. **Declarative**: Validation defined on form class, not in controller
3. **Automatic binding**: Errors automatically appear in `bindingResult`
4. **Template integration**: Errors displayed via `errors?.getFieldError('connectorCode')`
5. **Reusable**: Can be used on any field containing connector YAML

**Benefits of using RouteDefinition API over YAML manipulation:**
1. **Type-safe**: Work with Camel's native RouteDefinition objects
2. **No string parsing**: Avoid regex/JSON manipulation of YAML
3. **Validation included**: `findRoutesBuilders()` validates YAML syntax
4. **Future-proof**: Uses official Camel API, not workarounds
5. **Efficient**: Single parse, no serialize/deserialize cycle

### Summary of Route Group Pattern

| Entity Type | Group Name Pattern | Example |
|-------------|-------------------|---------|
| AggregatedDataProfile | `adp_{adp.id}` | `adp_550e8400-e29b-41d4-a716-446655440000` |
| Connector | `connector_{connector.id}` | `connector_660f9500-f39c-52e5-b827-557766551111` |

**Benefits**:
1. **Simple removal**: One call to `getRoutesByGroup()` finds all related routes
2. **Future-proof**: Adding new route types doesn't require updating removal code
3. **No tracking needed**: Don't need to track relation IDs or route suffixes
4. **Consistent pattern**: Same approach for ADP and Connector

## Phase 2: Database Schema Changes and Naming Constraints ✅ COMPLETED

### 2.0 Disable ADP Name Changes After Creation ✅

**Rationale**: With versioning, the ADP `name` becomes a logical identifier that groups all versions together. Allowing name changes after creation would cause:
- Confusion about which versions belong together
- Potential orphaned routes still using the old name
- API endpoint inconsistency (`/aggregated-data-profiles/{name}` would change)
- Version history fragmentation

**Current State**: The `AggregatedDataProfileEditForm` includes a `name` field with `@UniqueAggregatedDataProfileCheck` validation, allowing renames as long as the new name is unique or unchanged.

**Solution**: Remove the `name` field from the edit form entirely. The name is immutable after creation.

#### 2.0.1 Update AggregatedDataProfileEditForm

**File**: `src/main/kotlin/com/ritense/iko/mvc/model/AggregatedDataProfileEditForm.kt`

Remove the `name` field and the `UniqueAggregatedDataProfile` interface:

```kotlin
// REMOVE: @UniqueAggregatedDataProfileCheck
data class AggregatedDataProfileEditForm(
    val id: UUID,
    // REMOVE: override val name: String,
    @field:NotBlank(message = "Please provide roles.")
    @field:Pattern(
        regexp = ROLES_PATTERN,
        message = "Roles must be a comma-separated list of values (e.g., ROLE_ADMIN,ROLE_USER).",
    )
    val roles: String,
    val connectorInstanceId: UUID,
    val connectorEndpointId: UUID,
    @field:ValidTransform
    @field:NotBlank(message = "Please provide a transform expression.")
    val endpointTransform: String,
    @field:ValidTransform
    @field:NotBlank(message = "Please provide a transform expression.")
    val resultTransform: String,
    val cacheEnabled: Boolean,
    @field:Min(value = 0)
    val cacheTimeToLive: Int,
) // REMOVE: : UniqueAggregatedDataProfile
```

#### 2.0.2 Update AggregatedDataProfileController Edit Method

**File**: `src/main/kotlin/com/ritense/iko/mvc/controller/AggregatedDataProfileController.kt`

In the `edit()` method, remove the line that updates the name:

```kotlin
// REMOVE: aggregatedDataProfile.name = form.name
```

#### 2.0.3 Update Edit Form Template

**File**: `src/main/resources/templates/fragments/internal/aggregated-data-profile/formEditADP.html`

Either:
1. Remove the name input field entirely, OR
2. Make it read-only/disabled to show the name without allowing edits:

```html
<!-- Option 2: Show name as read-only -->
<cds-text-input
    label="Name"
    th:value="${aggregatedDataProfile.name}"
    disabled
    helper-text="Name cannot be changed after creation">
</cds-text-input>
```

#### 2.0.4 Keep UniqueAggregatedDataProfileCheck for Creation Only

The `@UniqueAggregatedDataProfileCheck` annotation remains on `AggregatedDataProfileAddForm` for validating new ADP names during creation. It can be removed from `AggregatedDataProfileEditForm` since the name field is removed.

**Summary of Changes**:
| File | Change |
|------|--------|
| `AggregatedDataProfileEditForm.kt` | Remove `name` field, remove `UniqueAggregatedDataProfile` interface, remove `@UniqueAggregatedDataProfileCheck` |
| `AggregatedDataProfileController.kt` | Remove `aggregatedDataProfile.name = form.name` in edit method |
| `formEditADP.html` | Remove or disable name input field |
| `AggregatedDataProfileAddForm.kt` | No changes - keeps `@UniqueAggregatedDataProfileCheck` for creation |

### 2.1 Migration: Add Version Columns ✅ COMPLETED

**Important**: The original migration assumed that `connector.tag` was unique, but this is NOT the case. There are duplicate tags in the existing data. The migration must handle this by renaming duplicate tags before creating the unique constraint.

**File**: `src/main/resources/db/migration/V2026.01.29.1__add_versioning.sql`

```sql
-- ============================================================================
-- STEP 1: Handle duplicate connector tags BEFORE adding version columns
-- ============================================================================
-- The tag column was not unique, so duplicates may exist.
-- We rename duplicates by appending a suffix: tag_2, tag_3, etc.
-- The first occurrence (by id) keeps the original tag name.

-- Create a temporary table to identify duplicates and assign new names
WITH ranked_connectors AS (
    SELECT
        id,
        tag,
        ROW_NUMBER() OVER (PARTITION BY tag ORDER BY id) as rn
    FROM connector
),
duplicates_to_rename AS (
    SELECT
        id,
        tag,
        tag || '_' || rn as new_tag
    FROM ranked_connectors
    WHERE rn > 1
)
UPDATE connector c
SET tag = d.new_tag
FROM duplicates_to_rename d
WHERE c.id = d.id;

-- Also update the connector name to reflect the tag change (optional but recommended)
-- This keeps name and tag in sync for renamed connectors
WITH ranked_connectors AS (
    SELECT
        id,
        tag,
        name,
        ROW_NUMBER() OVER (PARTITION BY tag ORDER BY id) as rn
    FROM connector
),
duplicates_to_rename AS (
    SELECT
        id,
        name || ' (duplicate ' || rn || ')' as new_name
    FROM ranked_connectors
    WHERE rn > 1
)
UPDATE connector c
SET name = d.new_name
FROM duplicates_to_rename d
WHERE c.id = d.id;

-- ============================================================================
-- STEP 2: Add version and is_active columns
-- ============================================================================

-- Add version and is_active columns to aggregated_data_profile
ALTER TABLE aggregated_data_profile ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE aggregated_data_profile ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Add version and is_active columns to connector
ALTER TABLE connector ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE connector ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- ============================================================================
-- STEP 3: Create unique constraints
-- ============================================================================

-- Drop existing unique constraint on name (ADP) if it exists
ALTER TABLE aggregated_data_profile DROP CONSTRAINT IF EXISTS uq_profile_name;

-- Create new unique constraint on name + version combination
ALTER TABLE aggregated_data_profile ADD CONSTRAINT aggregated_data_profile_name_version_unique
    UNIQUE (name, version);

-- Drop existing unique constraint on tag (Connector) if it exists
ALTER TABLE connector DROP CONSTRAINT IF EXISTS connector_tag_key;

-- Create new unique constraint on tag + version combination
ALTER TABLE connector ADD CONSTRAINT connector_tag_version_unique
    UNIQUE (tag, version);

-- ============================================================================
-- STEP 4: Create partial unique indexes for active versions
-- ============================================================================

-- Only one version can be active per name (ADP)
-- Partial unique index: ensures only one is_active=true per name
CREATE UNIQUE INDEX idx_adp_name_active ON aggregated_data_profile (name)
    WHERE is_active = TRUE;

-- Only one version can be active per tag (Connector)
CREATE UNIQUE INDEX idx_connector_tag_active ON connector (tag)
    WHERE is_active = TRUE;
```

**What the migration does:**

1. **Identifies duplicate tags** using `ROW_NUMBER()` partitioned by tag, ordered by id
2. **Renames duplicates** by appending `_2`, `_3`, etc. to the tag (e.g., `d` → `d_2`, `d_3`)
3. **Updates connector names** to indicate they were duplicates (optional cleanup)
4. **Adds version columns** with default `'1.0.0'`
5. **Creates unique constraints** on `(tag, version)` - now safe because tags are unique

**Example transformation:**

| Before | After |
|--------|-------|
| `tag: d, name: Connector D` | `tag: d, name: Connector D` (first occurrence, unchanged) |
| `tag: d, name: Another D` | `tag: d_2, name: Another D (duplicate 2)` |
| `tag: d, name: Third D` | `tag: d_3, name: Third D (duplicate 3)` |

**Note**: After migration, administrators should review connectors with `_2`, `_3` suffixes and either:
- Delete them if they were accidental duplicates
- Rename them to meaningful unique tags

## Phase 3: Domain Model Changes ✅ COMPLETED

### 3.0 Create Version Embeddable Class ✅

Create an embeddable `Version` class similar to `Transform`, with semver validation in the constructor. This pattern ensures validation happens at domain construction time, regardless of how the object is created.

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/Version.kt` (new file)

```kotlin
package com.ritense.iko.aggregateddataprofile.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Version(
    @Column(name = "version")
    val value: String,
) {
    init {
        validate(value)
    }

    companion object {
        // Semver regex: major.minor.patch (e.g., 1.0.0, 2.1.3)
        private val SEMVER_PATTERN = Regex("""^\d+\.\d+\.\d+$""")

        fun validate(version: String) {
            require(SEMVER_PATTERN.matches(version)) {
                "Version must be in semver format (e.g., 1.0.0), got: $version"
            }
        }

        /**
         * Check if a version string is valid without throwing an exception.
         * Useful for validators.
         */
        fun isValid(version: String?): Boolean {
            if (version.isNullOrBlank()) return false
            return SEMVER_PATTERN.matches(version)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Version
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value
}
```

### 3.0.1 Create ValidSemver Annotation for MVC Validation ✅

Create a validation annotation that uses `Version.isValid()` for form validation, following the same pattern as `ValidTransform`.

**File**: `src/main/kotlin/com/ritense/iko/mvc/model/validation/ValidSemver.kt` (new file)

```kotlin
package com.ritense.iko.mvc.model.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidSemverValidator::class])
annotation class ValidSemver(
    val message: String = "Version must be in semver format (e.g., 1.0.0)",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
```

**File**: `src/main/kotlin/com/ritense/iko/mvc/model/validation/ValidSemverValidator.kt` (new file)

```kotlin
package com.ritense.iko.mvc.model.validation

import com.ritense.iko.aggregateddataprofile.domain.Version
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidSemverValidator : ConstraintValidator<ValidSemver, String> {

    override fun isValid(
        version: String?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (version.isNullOrBlank()) return true // let @NotBlank handle empty values
        return Version.isValid(version)
    }
}
```

**Usage**: The `Version` class validates at domain construction. The `@ValidSemver` annotation validates at the MVC layer to provide user-friendly error messages before attempting domain object creation.

### 3.1 Update AggregatedDataProfile Entity ✅

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt`

Add fields using the embedded `Version` class:
```kotlin
@Embedded
val version: Version = Version("1.0.0"),

@Column(name = "is_active", nullable = false)
var isActive: Boolean = false,
```

**Note**: `isActive` defaults to `false` because new versions (including the initial version) should not be active until explicitly activated. This ensures routes are only loaded when the user intentionally activates a version.

Add method to create a new version:
```kotlin
fun createNewVersion(newVersion: String): AggregatedDataProfile {
    // Validation happens in Version constructor
    return AggregatedDataProfile(
        id = UUID.randomUUID(),
        name = this.name,
        version = Version(newVersion),
        isActive = false,
        connectorInstanceId = this.connectorInstanceId,
        connectorEndpointId = this.connectorEndpointId,
        endpointTransform = this.endpointTransform,
        resultTransform = this.resultTransform,
        roles = this.roles,
        aggregatedDataProfileCacheSetting = this.aggregatedDataProfileCacheSetting,
        relations = mutableListOf() // Relations will be copied separately
    )
}
```

### 3.2 Update Relation Entity ✅

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/Relation.kt`

No version field needed on Relation itself since relations are owned by ADP with cascade. When creating a new ADP version, relations are deep-copied with new IDs.

Add copy method:
```kotlin
fun copyForNewVersion(newAdpId: UUID): Relation {
    return Relation(
        id = UUID.randomUUID(),
        aggregatedDataProfileId = newAdpId,
        sourceId = null, // Will be remapped after all relations are copied
        connectorInstanceId = this.connectorInstanceId,
        connectorEndpointId = this.connectorEndpointId,
        propertyName = this.propertyName,
        endpointTransform = this.endpointTransform,
        resultTransform = this.resultTransform,
        relationCacheSettings = this.relationCacheSettings
    )
}
```

### 3.3 Update Connector Entity ✅

**File**: `src/main/kotlin/com/ritense/iko/connectors/domain/Connector.kt`

Add fields using the embedded `Version` class:
```kotlin
@Embedded
val version: Version = Version("1.0.0"),

@Column(name = "is_active", nullable = false)
var isActive: Boolean = false,
```

**Note**: Same as ADP - `isActive` defaults to `false` so connector routes are only loaded when explicitly activated.

Add method to create new version:
```kotlin
fun createNewVersion(newVersion: String): Connector {
    // Validation happens in Version constructor
    return Connector(
        id = UUID.randomUUID(),
        name = this.name,
        tag = this.tag,
        version = Version(newVersion),
        isActive = false,
        connectorCode = this.connectorCode
    )
}
```

### 3.4 Update ConnectorInstance Entity ✅

**File**: `src/main/kotlin/com/ritense/iko/connectors/domain/ConnectorInstance.kt`

ConnectorInstances reference a specific Connector. When a new Connector version is created, instances should be copied:

Add copy method:
```kotlin
fun copyForNewConnector(newConnectorId: UUID): ConnectorInstance {
    return ConnectorInstance(
        id = UUID.randomUUID(),
        name = this.name,
        tag = this.tag,
        connector = null, // Will be set by caller
        config = this.config.toMutableMap()
    )
}
```

## Phase 4: Repository Changes ✅ COMPLETED

### 4.1 Update AggregatedDataProfileRepository ✅

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/repository/AggregatedDataProfileRepository.kt`

Add methods:
```kotlin
// Find active version by name (for API calls without version override)
fun findByNameAndIsActiveTrue(name: String): AggregatedDataProfile?

// Find specific version by name and version
fun findByNameAndVersion(name: String, version: String): AggregatedDataProfile?

// Find all versions of a profile by name
fun findAllByNameOrderByVersionDesc(name: String): List<AggregatedDataProfile>

// Find all active profiles
fun findAllByIsActiveTrue(): List<AggregatedDataProfile>

// List projection for all versions of a name
@Query("SELECT new com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileListProjection(a.id, a.name, a.version, a.isActive) FROM AggregatedDataProfile a WHERE a.name = :name ORDER BY a.version DESC")
fun findVersionsByName(name: String): List<AggregatedDataProfileVersionProjection>
```

Add projection:
```kotlin
data class AggregatedDataProfileVersionProjection(
    val id: UUID,
    val name: String,
    val version: String,
    val isActive: Boolean
)
```

### 4.2 Update ConnectorRepository ✅

**File**: `src/main/kotlin/com/ritense/iko/connectors/repository/ConnectorRepository.kt`

Add methods:
```kotlin
fun findByTagAndIsActiveTrue(tag: String): Connector?
fun findByTagAndVersion(tag: String, version: String): Connector?
fun findAllByTagOrderByVersionDesc(tag: String): List<Connector>
fun findAllByIsActiveTrue(): List<Connector>

@Query("SELECT new com.ritense.iko.connectors.repository.ConnectorVersionProjection(c.id, c.name, c.tag, c.version, c.isActive) FROM Connector c WHERE c.tag = :tag ORDER BY c.version DESC")
fun findVersionsByTag(tag: String): List<ConnectorVersionProjection>
```

## Phase 5: Service Layer Changes ✅ COMPLETED

### 5.1 Update AggregatedDataProfileService ✅

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/service/AggregatedDataProfileService.kt`

**Change startup loading to only load active profiles**:
```kotlin
@EventListener(ApplicationReadyEvent::class)
fun loadAllAggregatedDataProfilesAtStartup(event: ApplicationReadyEvent) {
    aggregatedDataProfileRepository.findAllByIsActiveTrue().forEach { aggregatedDataProfile ->
        camelContext.addRoutes(AggregatedDataProfileRouteBuilder(...))
    }
}
```

**Add method to activate a version**:
```kotlin
@Transactional
fun activateVersion(id: UUID) {
    val profileToActivate = aggregatedDataProfileRepository.findById(id)
        .orElseThrow { AggregatedDataProfileNotFound(id) }

    // Prevent reactivation of already active version
    if (profileToActivate.isActive) {
        return // Already active, nothing to do
    }

    // Find and deactivate currently active version
    val currentActive = aggregatedDataProfileRepository.findByNameAndIsActiveTrue(profileToActivate.name)
    if (currentActive != null) {
        removeRoutes(currentActive)
        currentActive.isActive = false
        aggregatedDataProfileRepository.save(currentActive)
    }

    // Activate new version
    profileToActivate.isActive = true
    aggregatedDataProfileRepository.save(profileToActivate)
    addRoutes(profileToActivate)
}
```

**Add method to create new version**:

Relations form a tree structure where:
- Level 1 relations have `sourceId` pointing to the ADP's ID
- Nested relations have `sourceId` pointing to their parent relation's ID

When copying relations, we must:
1. Create new IDs for all relations
2. Remap `sourceId` references to point to the new IDs
3. Preserve the tree structure

```
Original tree:                    Copied tree:
ADP (id: A)                       ADP (id: A')
├── Relation (id: R1)             ├── Relation (id: R1')
│   ├── Relation (id: R2)         │   ├── Relation (id: R2')
│   │   ├── Relation (id: R5)     │   │   ├── Relation (id: R5')
│   │   └── Relation (id: R6)     │   │   └── Relation (id: R6')
│   └── Relation (id: R3)         │   └── Relation (id: R3')
└── Relation (id: R4)             └── Relation (id: R4')
    └── Relation (id: R7)             └── Relation (id: R7')

Flat list in source.relations:
[R1, R2, R3, R4, R5, R6, R7]  (order may vary)

sourceId mappings (original):     sourceId mappings (copied):
R1.sourceId = A                   R1'.sourceId = A'
R2.sourceId = R1                  R2'.sourceId = R1'
R3.sourceId = R1                  R3'.sourceId = R1'
R4.sourceId = A                   R4'.sourceId = A'
R5.sourceId = R2                  R5'.sourceId = R2'
R6.sourceId = R2                  R6'.sourceId = R2'
R7.sourceId = R4                  R7'.sourceId = R4'
```

**Algorithm walkthrough:**

```
Input: source.relations = [R1, R2, R3, R4, R5, R6, R7]

Step 1: Initialize idMapping with ADP mapping
  idMapping = { A → A' }

Step 2: First pass - copy all relations and build ID mapping
  Copy R1 → R1' (new UUID), idMapping = { A → A', R1 → R1' }
  Copy R2 → R2' (new UUID), idMapping = { A → A', R1 → R1', R2 → R2' }
  Copy R3 → R3' (new UUID), idMapping = { A → A', R1 → R1', R2 → R2', R3 → R3' }
  Copy R4 → R4' (new UUID), idMapping = { A → A', R1 → R1', R2 → R2', R3 → R3', R4 → R4' }
  Copy R5 → R5' (new UUID), idMapping = { ..., R5 → R5' }
  Copy R6 → R6' (new UUID), idMapping = { ..., R6 → R6' }
  Copy R7 → R7' (new UUID), idMapping = { ..., R7 → R7' }

  All new relations have sourceId = null at this point.

Step 3: Second pass - remap sourceId using idMapping
  R1'.sourceId = idMapping[R1.sourceId] = idMapping[A] = A'   ✓
  R2'.sourceId = idMapping[R2.sourceId] = idMapping[R1] = R1' ✓
  R3'.sourceId = idMapping[R3.sourceId] = idMapping[R1] = R1' ✓
  R4'.sourceId = idMapping[R4.sourceId] = idMapping[A] = A'   ✓
  R5'.sourceId = idMapping[R5.sourceId] = idMapping[R2] = R2' ✓
  R6'.sourceId = idMapping[R6.sourceId] = idMapping[R2] = R2' ✓
  R7'.sourceId = idMapping[R7.sourceId] = idMapping[R4] = R4' ✓

Result: Complete tree copy with all sourceId references correctly remapped.
```

```kotlin
@Transactional
fun createNewVersion(sourceId: UUID, newVersion: String): AggregatedDataProfile {
    val source = aggregatedDataProfileRepository.findById(sourceId)
        .orElseThrow { AggregatedDataProfileNotFound(sourceId) }

    // Check version doesn't already exist
    if (aggregatedDataProfileRepository.findByNameAndVersion(source.name, newVersion) != null) {
        throw VersionAlreadyExists(source.name, newVersion)
    }

    // Create new ADP (without relations)
    val newAdp = source.createNewVersion(newVersion)
    aggregatedDataProfileRepository.save(newAdp)

    // Build ID mapping: old ID -> new ID
    // Include the ADP ID mapping for level 1 relations
    val idMapping = mutableMapOf<UUID, UUID>()
    idMapping[source.id] = newAdp.id

    // First pass: copy all relations and build ID mapping
    val newRelations = source.relations.map { oldRelation ->
        val newRelation = oldRelation.copyForNewVersion(newAdp.id)
        idMapping[oldRelation.id] = newRelation.id
        oldRelation to newRelation
    }

    // Second pass: remap sourceId using the ID mapping
    newRelations.forEach { (oldRelation, newRelation) ->
        // sourceId points to either the ADP or a parent relation
        // Use the mapping to get the new ID
        newRelation.sourceId = idMapping[oldRelation.sourceId]
    }

    // Add all copied relations to the new ADP
    newAdp.relations.addAll(newRelations.map { it.second })
    return aggregatedDataProfileRepository.save(newAdp)
}
```

**Remove reloadRoutes() calls from edit methods** - Editing should not reload routes. Only activateVersion() loads routes.

### 5.2 Extend ConnectorService with Versioning Methods ✅

**File**: `src/main/kotlin/com/ritense/iko/connectors/service/ConnectorService.kt`

The `ConnectorService` was created in Phase 1.3 with route loading/removal. Now extend it with versioning methods:

```kotlin
@Service
internal class ConnectorService(
    private val connectorRepository: ConnectorRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val connectorEndpointRoleRepository: ConnectorEndpointRoleRepository,
    private val camelContext: CamelContext,
) {
    private val logger = KotlinLogging.logger {}

    // loadConnectorRoutes(), validateConnectorCode(), removeConnectorRoutes()
    // ... already defined in Phase 1.3 ...

    @Transactional
    fun activateVersion(id: UUID) {
        val connectorToActivate = connectorRepository.findById(id)
            .orElseThrow { ConnectorNotFound(id) }

        // Prevent reactivation of already active version
        if (connectorToActivate.isActive) {
            return // Already active, nothing to do
        }

        // Find and deactivate current active
        val currentActive = connectorRepository.findByTagAndIsActiveTrue(connectorToActivate.tag)
        if (currentActive != null) {
            removeConnectorRoutes(currentActive)
            currentActive.isActive = false
            connectorRepository.save(currentActive)
        }

        // Activate new version
        connectorToActivate.isActive = true
        connectorRepository.save(connectorToActivate)
        loadConnectorRoutes(connectorToActivate)
    }

    @Transactional
    fun createNewVersion(sourceId: UUID, newVersion: String): Connector {
        val source = connectorRepository.findById(sourceId)
            .orElseThrow { ConnectorNotFound(sourceId) }

        if (connectorRepository.findByTagAndVersion(source.tag, newVersion) != null) {
            throw VersionAlreadyExists(source.tag, newVersion)
        }

        // Create new connector
        val newConnector = source.createNewVersion(newVersion)
        connectorRepository.save(newConnector)

        // Copy endpoints
        val endpoints = connectorEndpointRepository.findByConnector(source)
        endpoints.forEach { endpoint ->
            val newEndpoint = ConnectorEndpoint(
                id = UUID.randomUUID(),
                connector = newConnector,
                name = endpoint.name
            )
            connectorEndpointRepository.save(newEndpoint)
        }

        // Copy instances
        val instances = connectorInstanceRepository.findByConnector(source)
        instances.forEach { instance ->
            val newInstance = instance.copyForNewConnector(newConnector.id)
            newInstance.connector = newConnector
            connectorInstanceRepository.save(newInstance)

            // Copy endpoint roles for this instance
            val roles = connectorEndpointRoleRepository.findAllByConnectorInstance(instance)
            roles.forEach { role ->
                // Map old endpoint to new endpoint by name
                val newEndpoint = connectorEndpointRepository.findByConnectorAndName(newConnector, role.connectorEndpoint.name)
                if (newEndpoint != null) {
                    val newRole = ConnectorEndpointRole(
                        id = UUID.randomUUID(),
                        connectorInstance = newInstance,
                        connectorEndpoint = newEndpoint,
                        role = role.role
                    )
                    connectorEndpointRoleRepository.save(newRole)
                }
            }
        }

        return newConnector
    }
}
```

## Phase 6: Lazy Loading for Testing Non-Active Versions ✅ COMPLETED

Instead of adding version override to the REST API, we enable testing non-active ADP versions via the TestController (Preview tab in the admin UI). The version is passed from the currently viewed ADP detail page.

### 6.1 Update TestAggregatedDataProfileForm ✅

**File**: `src/main/kotlin/com/ritense/iko/mvc/model/TestAggregatedDataProfileForm.kt`

Add `version` field to identify which ADP version to test:

```kotlin
data class TestAggregatedDataProfileForm(
    @field:NotBlank(message = "Please provide a valid json object.")
    val endpointTransformContext: String,
    @field:NotBlank(message = "Please provide a transform expression.")
    @field:ValidTransform
    val resultTransform: String,
    val name: String,
    val version: String,  // ADD: Version of the ADP being tested
)
```

### 6.2 Update debug.html Template ✅

**File**: `src/main/resources/templates/fragments/internal/aggregated-data-profile/debug.html`

Add hidden `version` input that gets its value from the currently viewed ADP:

```html
<input
    type="hidden"
    id="name"
    name="name"
    th:value="${aggregatedDataProfile.name}"
/>
<input
    type="hidden"
    id="version"
    name="version"
    th:value="${aggregatedDataProfile.version.value}"
/>
```

Update `hx-include` to include the version field:

```html
<cds-button
    kind="primary"
    hx-post="/admin/aggregated-data-profiles/debug"
    hx-target="#profile-debug"
    hx-swap="outerHTML"
    hx-include="[id='endpointTransformContext'], [id='resultTransform'], [id='name'], [id='version']"
    ...
>
```

### 6.3 Update TestController with Lazy Loading and Route Suspension ✅

**File**: `src/main/kotlin/com/ritense/iko/mvc/controller/TestController.kt`

Add `AggregatedDataProfileService` dependency and implement lazy route loading with suspension after test. On Camel 4.x, we suspend (stop) routes rather than removing them, and resume if already suspended:

```kotlin
@Controller
@RequestMapping("/admin")
class TestController(
    private val producerTemplate: ProducerTemplate,
    private val camelContext: CamelContext,
    private val objectMapper: ObjectMapper,
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,  // ADD
    private val aggregatedDataProfileService: AggregatedDataProfileService,        // ADD
) {
    @PostMapping(
        path = ["/aggregated-data-profiles/debug"],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
    )
    fun test(
        @Valid @ModelAttribute form: TestAggregatedDataProfileForm,
        httpServletResponse: HttpServletResponse,
    ): ModelAndView {
        // Lookup the ADP by name and version
        val aggregatedDataProfile = aggregatedDataProfileRepository
            .findByNameAndVersion(form.name, form.version)
            ?: throw AggregatedDataProfileNotFound(form.name, form.version)

        // For non-active versions, we need to ensure routes are available for testing
        val needsTemporaryRoutes = !aggregatedDataProfile.isActive
        if (needsTemporaryRoutes) {
            ensureRoutesAvailable(aggregatedDataProfile)
        }

        try {
            // ... existing test logic (producerTemplate.send, etc.) ...

            return ModelAndView(...)  // return test results
        } finally {
            // Suspend routes after testing if they were loaded temporarily
            if (needsTemporaryRoutes) {
                suspendRoutes(aggregatedDataProfile)
            }
        }
    }

    /**
     * Ensures routes are available for testing.
     * - If routes don't exist: register them
     * - If routes exist but are stopped: resume them
     * - If routes are already running: do nothing
     */
    private fun ensureRoutesAvailable(aggregatedDataProfile: AggregatedDataProfile) {
        val routeId = "aggregated_data_profile_${aggregatedDataProfile.id}_root"
        val existingRoute = camelContext.getRoute(routeId)

        if (existingRoute == null) {
            // Routes not registered yet - add them
            aggregatedDataProfileService.addRoutes(aggregatedDataProfile)
        } else {
            // Routes exist - check if they need to be resumed
            val routeController = camelContext.routeController
            val status = routeController.getRouteStatus(routeId)
            if (status.isStopped || status.isSuspended) {
                // Resume all routes in the ADP's group
                val groupName = "adp_${aggregatedDataProfile.id}"
                camelContext.getRoutesByGroup(groupName).forEach { route ->
                    routeController.resumeRoute(route.id)
                }
            }
        }
    }

    /**
     * Suspends (stops) routes after testing.
     * Routes remain registered but inactive, avoiding re-registration overhead on next test.
     */
    private fun suspendRoutes(aggregatedDataProfile: AggregatedDataProfile) {
        val groupName = "adp_${aggregatedDataProfile.id}"
        val routeController = camelContext.routeController
        camelContext.getRoutesByGroup(groupName).forEach { route ->
            routeController.suspendRoute(route.id)
        }
    }
}
```

**Key behaviors:**
1. **Active versions**: Routes are already loaded and running at startup, no action needed
2. **Non-active versions (first test)**: Routes are registered and started
3. **Non-active versions (subsequent tests)**: Routes are resumed from suspended state (faster than re-registering)
4. **After test**: Routes are suspended but remain registered in CamelContext
5. **Never activates**: The `isActive` flag is never modified - this is purely for testing
6. **Clean up on error**: The `finally` block ensures routes are suspended even if the test fails

**Camel 4.x Route States:**
- `Started` - Route is running and processing exchanges
- `Stopped` - Route is not running, can be started/resumed
- `Suspended` - Route is paused, can be resumed (preserves inflight exchanges)

**Benefits of suspend/resume over remove/add:**
- Faster: No route parsing and registration overhead on subsequent tests
- Safer: Route definitions remain validated and ready
- Memory efficient: Suspended routes use minimal resources
- Consistent: Routes maintain their configuration across test runs

This approach is preferred over API version override because:
- Testing is only needed in the admin UI, not the production API
- Avoids complexity of version parameter in REST endpoints
- Routes are loaded on-demand and suspended after use
- The currently viewed version in the UI naturally becomes the test target
- Subsequent tests of the same version are faster (resume vs re-register)

## Phase 7: Admin UI Changes

### 7.1 Update Page Header Fragment

**File**: `src/main/resources/templates/layout-internal.html`

Create new page header with version dropdown:
```html
<th:block th:fragment="page-header-versioned (title, versions, currentVersion, entityId, entityType)">
    <div class="page-header-versioned">
        <div class="page-header-title-row">
            <h2 class="cds--type-heading-03" th:text="${title}"></h2>
            <div class="page-header-actions">
                <cds-dropdown
                    th:id="|version-dropdown-${entityId}|"
                    label="Version"
                    th:value="${currentVersion}"
                    hx-trigger="cds-dropdown-selected"
                    th:hx-get="@{|/admin/${entityType}s/by-version|}"
                    hx-target="#view-panel"
                    hx-push-url="true">
                    <cds-dropdown-item
                        th:each="v : ${versions}"
                        th:value="${v.id}"
                        th:text="|${v.version}${v.isActive ? ' (Active)' : ''}|">
                    </cds-dropdown-item>
                </cds-dropdown>
                <cds-button
                    kind="tertiary"
                    size="sm"
                    th:hx-get="@{|/admin/${entityType}s/${entityId}/versions/create|}"
                    hx-target="#modal-container">
                    New Version
                </cds-button>
                <cds-button
                    th:if="${!isCurrentVersionActive}"
                    kind="primary"
                    size="sm"
                    th:hx-post="@{|/admin/${entityType}s/${entityId}/activate|}"
                    hx-target="#view-panel">
                    Activate
                </cds-button>
            </div>
        </div>
    </div>
</th:block>
```

### 7.2 Create Version Modal Template

**File**: `src/main/resources/templates/fragments/internal/version-modal.html`

```html
<th:block th:fragment="create-version-modal (entityType, entityId, currentVersion)">
    <cds-modal id="create-version-modal" open>
        <cds-modal-header>
            <cds-modal-close-button></cds-modal-close-button>
            <cds-modal-heading>Create New Version</cds-modal-heading>
        </cds-modal-header>
        <cds-modal-body>
            <form
                th:hx-post="@{|/admin/${entityType}s/${entityId}/versions|}"
                hx-target="#modal-container"
                hx-swap="innerHTML">
                <cds-text-input
                    name="version"
                    label="Version"
                    placeholder="e.g., 1.1.0"
                    helper-text="Use semantic versioning (major.minor.patch)"
                    th:value="${form?.version}"
                    th:invalid="${errors?.hasFieldErrors('version')}"
                    required>
                </cds-text-input>
                <!-- Validation error display -->
                <div
                    class="cds--form-requirement"
                    th:if="${errors?.getFieldError('version') != null}"
                    th:text="${errors?.getFieldError('version')?.defaultMessage}">
                </div>
                <p class="cds--label" style="margin-top: var(--cds-spacing-05);">
                    Current version: <span th:text="${currentVersion}"></span>
                </p>
            </form>
        </cds-modal-body>
        <cds-modal-footer>
            <cds-modal-footer-button kind="secondary" data-modal-close>Cancel</cds-modal-footer-button>
            <cds-modal-footer-button kind="primary" type="submit">Create</cds-modal-footer-button>
        </cds-modal-footer>
    </cds-modal>
</th:block>
```

### 7.3 Update ADP Detail Page

**File**: `src/main/resources/templates/fragments/internal/aggregated-data-profile/detail-page.html`

Replace page header:
```html
<th:block th:insert="~{layout-internal :: page-header-versioned(
    title=${aggregatedDataProfile.name},
    versions=${versions},
    currentVersion=${aggregatedDataProfile.version},
    entityId=${aggregatedDataProfile.id},
    entityType='aggregated-data-profile',
    isCurrentVersionActive=${aggregatedDataProfile.isActive}
)}"/>
```

### 7.4 Update Connector Detail Page

**File**: `src/main/resources/templates/fragments/internal/connector/detailsPageConnector.html`

Replace page header with versioned variant (same pattern as ADP).

### 7.5 Create Version Form Class

**File**: `src/main/kotlin/com/ritense/iko/mvc/model/CreateVersionForm.kt` (new file)

A reusable form class for creating new versions, used by both ADP and Connector controllers:

```kotlin
package com.ritense.iko.mvc.model

import com.ritense.iko.mvc.model.validation.ValidSemver
import jakarta.validation.constraints.NotBlank

data class CreateVersionForm(
    @field:NotBlank(message = "Please provide a version.")
    @field:ValidSemver
    val version: String,
)
```

### 7.6 Update Controllers

**File**: `src/main/kotlin/com/ritense/iko/mvc/controller/AggregatedDataProfileController.kt`

Add new endpoints:

```kotlin
// Get version list for dropdown
@GetMapping("/aggregated-data-profiles/{id}/versions")
fun getVersions(@PathVariable id: UUID): List<AggregatedDataProfileVersionProjection> {
    val profile = aggregatedDataProfileRepository.findById(id).orElseThrow()
    return aggregatedDataProfileRepository.findVersionsByName(profile.name)
}

// Navigate to specific version
@GetMapping("/aggregated-data-profiles/by-version")
fun detailsByVersion(
    @RequestParam id: UUID,
    @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
): ModelAndView {
    return details(id, isHxRequest)
}

// Show create version modal
@GetMapping("/aggregated-data-profiles/{id}/versions/create")
fun createVersionModal(@PathVariable id: UUID): ModelAndView {
    val profile = aggregatedDataProfileRepository.findById(id).orElseThrow()
    return ModelAndView("fragments/internal/version-modal :: create-version-modal").apply {
        addObject("entityType", "aggregated-data-profile")
        addObject("entityId", id)
        addObject("currentVersion", profile.version)
        addObject("form", CreateVersionForm(""))
    }
}

// Create new version
@PostMapping("/aggregated-data-profiles/{id}/versions")
fun createVersion(
    @PathVariable id: UUID,
    @Valid @ModelAttribute form: CreateVersionForm,
    bindingResult: BindingResult,
    @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    httpServletResponse: HttpServletResponse,
): ModelAndView {
    val profile = aggregatedDataProfileRepository.findById(id).orElseThrow()

    if (bindingResult.hasErrors()) {
        return ModelAndView("fragments/internal/version-modal :: create-version-modal").apply {
            addObject("entityType", "aggregated-data-profile")
            addObject("entityId", id)
            addObject("currentVersion", profile.version)
            addObject("form", form)
            addObject("errors", bindingResult)
        }
    }

    val newProfile = aggregatedDataProfileService.createNewVersion(id, form.version)

    httpServletResponse.setHeader("HX-Trigger", "close-modal")
    httpServletResponse.setHeader("HX-Push-Url", "/admin/aggregated-data-profiles/${newProfile.id}")

    return details(newProfile.id, isHxRequest)
}

// Activate version
@PostMapping("/aggregated-data-profiles/{id}/activate")
fun activateVersion(
    @PathVariable id: UUID,
    @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
): ModelAndView {
    aggregatedDataProfileService.activateVersion(id)
    return details(id, isHxRequest)
}
```

**File**: `src/main/kotlin/com/ritense/iko/mvc/controller/ConnectorController.kt`

Add similar endpoints for Connector versioning (same pattern as ADP):

```kotlin
// Show create version modal
@GetMapping("/connectors/{id}/versions/create")
fun createVersionModal(@PathVariable id: UUID): ModelAndView {
    val connector = connectorRepository.findById(id).orElseThrow()
    return ModelAndView("fragments/internal/version-modal :: create-version-modal").apply {
        addObject("entityType", "connector")
        addObject("entityId", id)
        addObject("currentVersion", connector.version)
        addObject("form", CreateVersionForm(""))
    }
}

// Create new version
@PostMapping("/connectors/{id}/versions")
fun createVersion(
    @PathVariable id: UUID,
    @Valid @ModelAttribute form: CreateVersionForm,
    bindingResult: BindingResult,
    @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    httpServletResponse: HttpServletResponse,
): ModelAndView {
    val connector = connectorRepository.findById(id).orElseThrow()

    if (bindingResult.hasErrors()) {
        return ModelAndView("fragments/internal/version-modal :: create-version-modal").apply {
            addObject("entityType", "connector")
            addObject("entityId", id)
            addObject("currentVersion", connector.version)
            addObject("form", form)
            addObject("errors", bindingResult)
        }
    }

    val newConnector = connectorService.createNewVersion(id, form.version)

    httpServletResponse.setHeader("HX-Trigger", "close-modal")
    httpServletResponse.setHeader("HX-Push-Url", "/admin/connectors/${newConnector.id}")

    return details(newConnector.id, isHxRequest)
}

// Activate version
@PostMapping("/connectors/{id}/activate")
fun activateVersion(
    @PathVariable id: UUID,
    @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
): ModelAndView {
    connectorService.activateVersion(id)
    return details(id, isHxRequest)
}
```

Update `details()` method to include versions list:
```kotlin
fun details(...): ModelAndView {
    // ... existing code ...

    val versions = aggregatedDataProfileRepository.findVersionsByName(aggregatedDataProfile.name)

    return ModelAndView(...).apply {
        // ... existing objects ...
        addObject("versions", versions)
        addObject("isCurrentVersionActive", aggregatedDataProfile.isActive)
    }
}
```

**Remove** `reloadRoutes()` calls from create/edit methods. Routes should only be loaded via `activateVersion()`.

Similar changes for `ConnectorController.kt`.

## Phase 8: Remove Automatic Route Loading on Edit

### 8.1 Update AggregatedDataProfileController

Remove these lines from `edit()` method (around line 285):
```kotlin
// REMOVE: aggregatedDataProfileService.reloadRoutes(aggregatedDataProfile)
```

Remove from `createRelation()` (around line 339):
```kotlin
// REMOVE: aggregatedDataProfileService.reloadRoutes(aggregatedDataProfile)
```

Remove from `editRelation()` (around line 405):
```kotlin
// REMOVE: aggregatedDataProfileService.reloadRoutes(aggregatedDataProfile)
```

Remove from `deleteRelation()` (around line 444):
```kotlin
// REMOVE: aggregatedDataProfileService.reloadRoutes(aggregatedDataProfile)
```

### 8.2 Update Create Flow

When creating a NEW ADP (not a version), it should start as inactive:
```kotlin
@PostMapping("/aggregated-data-profiles")
fun create(...): ModelAndView {
    // ... validation ...

    val aggregatedDataProfile = AggregatedDataProfile(
        // ... fields ...
        version = "1.0.0",
        isActive = false  // Start inactive
    )
    aggregatedDataProfileRepository.save(aggregatedDataProfile)

    // Do NOT call reloadRoutes() - user must explicitly activate

    return details(aggregatedDataProfile.id, isHxRequest)
}
```

## Phase 9: CSS Updates

**File**: `src/main/resources/static/css/style.css`

Add styles for versioned page header:
```css
.page-header-versioned {
    display: flex;
    flex-direction: column;
    gap: var(--cds-spacing-03);
    margin-bottom: var(--cds-spacing-05);
}

.page-header-title-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--cds-spacing-03);
}

.page-header-actions {
    display: flex;
    align-items: center;
    gap: var(--cds-spacing-03);
}

.page-header-actions cds-dropdown {
    min-width: 150px;
}
```

## Implementation Order

1. **Phase 1**: Fix route removal bug (critical, independent)
2. **Phase 2.0**: Disable ADP name changes (can be done independently, prepares for versioning)
3. **Phase 2.1**: Database migration (foundation for all other changes)
4. **Phase 3**: Domain model changes (depends on Phase 2.1)
5. **Phase 4**: Repository changes (depends on Phase 3)
6. **Phase 5**: Service layer changes (depends on Phase 4)
7. **Phase 6**: TestController lazy loading (depends on Phase 5)
8. **Phase 7**: Admin UI (depends on Phase 5)
9. **Phase 8**: Remove auto-reload (depends on Phase 5)
10. **Phase 9**: CSS (depends on Phase 7)

## Testing Considerations

1. **Unit tests** for version creation logic (deep copy, ID remapping)
2. **Integration tests** for:
   - Creating new versions
   - Activating versions
   - Lazy loading via TestController for non-active versions
   - Only one active version per name constraint
3. **UI tests** for version dropdown and modal
4. **Migration test** with existing data

## Decisions

1. **Semver validation**: No validation that new versions are greater than existing versions. Any valid semver is allowed.
2. **Version deletion**: Individual versions cannot be deleted. Only the entire entity (all versions) can be deleted.
3. **Cascade on version delete**: N/A - individual version deletion is not supported.
4. **Max versions**: No limit on number of versions.
5. **Audit trail**: Use debug-level logging for every lifecycle change of a Connector/ADP (creation, activation, deactivation, route suspend/resume).

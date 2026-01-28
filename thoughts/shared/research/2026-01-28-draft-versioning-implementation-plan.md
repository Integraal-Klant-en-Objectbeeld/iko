---
date: 2026-01-28T15:45:00Z
researcher: Claude
git_commit: b99beb5272268dcc5063e61ded4ca31b91b7d7d6
branch: main
repository: iko
topic: "Implementation Plan: Draft Versioning System"
tags: [implementation-plan, versioning, aggregated-data-profile, connector, camel, semver]
status: complete
last_updated: 2026-01-28
last_updated_by: Claude
---

# Implementation Plan: Draft Versioning System

## Overview

This plan describes how to implement a draft/versioning system for AggregatedDataProfiles and Connectors where:
- Editing does not affect currently running routes until explicitly activated
- Versions use semantic versioning (semver)
- Only one version is active at a time per entity
- A query parameter can override the active version for testing
- Relations and ConnectorInstances are duplicated per version (not shared)

## Strategy

**Option 1: Version field on domain entity** - Add `version: String` (semver format "1.0.0") and `isActive: Boolean` to ADP and Connector entities. Each version is a separate row in the database with the same logical "name" but different version.

## Phase 1: Bug Fixes

### 1.1 Fix Route Removal Bug in AggregatedDataProfileService

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/service/AggregatedDataProfileService.kt`

**Current Bug (lines 51-59)**:
```kotlin
fun removeRoutes(aggregatedDataProfile: AggregatedDataProfile) {
    removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_direct")  // WRONG
    removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")
    aggregatedDataProfile.relations.forEach { relation ->
        removeRoute("relation_${relation.id}_direct")  // WRONG
        removeRoute("relation_${relation.id}_multicast")
    }
}
```

**Routes NOT being removed**:
- `aggregated_data_profile_{id}_root` (created at AggregatedDataProfileRouteBuilder.kt:69)
- `aggregated_data_profile_{id}_endpoint_transform` (created at line 105)
- `relation_{id}_root` (created at line 157)
- `relation_{id}_map` (created at line 184)
- `relation_{id}_array` (created at line 197)
- `relation_{id}_loop` (created at line 220)

**Fix**: Update `removeRoutes()` to remove all actual route IDs:

```kotlin
fun removeRoutes(aggregatedDataProfile: AggregatedDataProfile) {
    removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_root")
    removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_endpoint_transform")
    removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")

    aggregatedDataProfile.relations.forEach { relation ->
        removeRoute("relation_${relation.id}_root")
        removeRoute("relation_${relation.id}_map")
        removeRoute("relation_${relation.id}_array")
        removeRoute("relation_${relation.id}_loop")
        removeRoute("relation_${relation.id}_multicast")
    }
}
```

## Phase 2: Database Schema Changes

### 2.1 Migration: Add Version Columns

**File**: `src/main/resources/db/migration/V2026.01.28.1__add_versioning.sql`

```sql
-- Add version and is_active columns to aggregated_data_profile
ALTER TABLE aggregated_data_profile ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE aggregated_data_profile ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Add version and is_active columns to connector
ALTER TABLE connector ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE connector ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Add version column to relation (for identifying which ADP version it belongs to)
-- Relations are duplicated per version, not shared
ALTER TABLE relation ADD COLUMN adp_version VARCHAR(50) NOT NULL DEFAULT '1.0.0';

-- Add version column to connector_instance (for identifying which Connector version it belongs to)
ALTER TABLE connector_instance ADD COLUMN connector_version VARCHAR(50) NOT NULL DEFAULT '1.0.0';

-- Drop existing unique constraint on name (ADP)
ALTER TABLE aggregated_data_profile DROP CONSTRAINT IF EXISTS aggregated_data_profile_name_key;

-- Create new unique constraint on name + version combination
ALTER TABLE aggregated_data_profile ADD CONSTRAINT aggregated_data_profile_name_version_unique
    UNIQUE (name, version);

-- Drop existing unique constraint on tag (Connector)
ALTER TABLE connector DROP CONSTRAINT IF EXISTS connector_tag_key;

-- Create new unique constraint on tag + version combination
ALTER TABLE connector ADD CONSTRAINT connector_tag_version_unique
    UNIQUE (tag, version);

-- Only one version can be active per name (ADP)
CREATE UNIQUE INDEX idx_adp_name_active ON aggregated_data_profile (name)
    WHERE is_active = TRUE;

-- Only one version can be active per tag (Connector)
CREATE UNIQUE INDEX idx_connector_tag_active ON connector (tag)
    WHERE is_active = TRUE;
```

## Phase 3: Domain Model Changes

### 3.1 Update AggregatedDataProfile Entity

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt`

Add fields:
```kotlin
@Column(name = "version", nullable = false)
val version: String = "1.0.0",

@Column(name = "is_active", nullable = false)
var isActive: Boolean = true,
```

Add validation for semver format in constructor or via custom validator.

Add method to create a new version:
```kotlin
fun createNewVersion(newVersion: String): AggregatedDataProfile {
    // Validate semver format
    require(newVersion.matches(Regex("""^\d+\.\d+\.\d+$"""))) {
        "Version must be in semver format (e.g., 1.0.0)"
    }

    // Create new ADP with same data but new ID, version, and inactive
    return AggregatedDataProfile(
        id = UUID.randomUUID(),
        name = this.name,
        version = newVersion,
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

### 3.2 Update Relation Entity

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

### 3.3 Update Connector Entity

**File**: `src/main/kotlin/com/ritense/iko/connectors/domain/Connector.kt`

Add fields:
```kotlin
@Column(name = "version", nullable = false)
val version: String = "1.0.0",

@Column(name = "is_active", nullable = false)
var isActive: Boolean = true,
```

Add method to create new version:
```kotlin
fun createNewVersion(newVersion: String): Connector {
    require(newVersion.matches(Regex("""^\d+\.\d+\.\d+$"""))) {
        "Version must be in semver format (e.g., 1.0.0)"
    }

    return Connector(
        id = UUID.randomUUID(),
        name = this.name,
        tag = this.tag,
        version = newVersion,
        isActive = false,
        connectorCode = this.connectorCode
    )
}
```

### 3.4 Update ConnectorInstance Entity

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

## Phase 4: Repository Changes

### 4.1 Update AggregatedDataProfileRepository

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

### 4.2 Update ConnectorRepository

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

## Phase 5: Service Layer Changes

### 5.1 Update AggregatedDataProfileService

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

    // Find and deactivate currently active version
    val currentActive = aggregatedDataProfileRepository.findByNameAndIsActiveTrue(profileToActivate.name)
    if (currentActive != null && currentActive.id != id) {
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
```kotlin
@Transactional
fun createNewVersion(sourceId: UUID, newVersion: String): AggregatedDataProfile {
    val source = aggregatedDataProfileRepository.findById(sourceId)
        .orElseThrow { AggregatedDataProfileNotFound(sourceId) }

    // Check version doesn't already exist
    if (aggregatedDataProfileRepository.findByNameAndVersion(source.name, newVersion) != null) {
        throw VersionAlreadyExists(source.name, newVersion)
    }

    // Create new ADP
    val newAdp = source.createNewVersion(newVersion)
    aggregatedDataProfileRepository.save(newAdp)

    // Deep copy relations with ID mapping for parent-child relationships
    val oldToNewRelationIds = mutableMapOf<UUID, UUID>()
    val newRelations = source.relations.map { oldRelation ->
        val newRelation = oldRelation.copyForNewVersion(newAdp.id)
        oldToNewRelationIds[oldRelation.id] = newRelation.id
        newRelation
    }

    // Remap sourceId references
    newRelations.forEach { newRelation ->
        val originalRelation = source.relations.find {
            oldToNewRelationIds[it.id] == newRelation.id
        }
        if (originalRelation?.sourceId != null && originalRelation.sourceId != source.id) {
            newRelation.sourceId = oldToNewRelationIds[originalRelation.sourceId]
        } else if (originalRelation?.sourceId == source.id) {
            newRelation.sourceId = newAdp.id
        }
    }

    newAdp.relations.addAll(newRelations)
    return aggregatedDataProfileRepository.save(newAdp)
}
```

**Remove reloadRoutes() calls from edit methods** - Editing should not reload routes. Only activateVersion() loads routes.

### 5.2 Create ConnectorService

**File**: `src/main/kotlin/com/ritense/iko/connectors/service/ConnectorService.kt` (new file)

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

    @EventListener(ApplicationReadyEvent::class)
    fun loadAllConnectorsAtStartup(event: ApplicationReadyEvent) {
        connectorRepository.findAllByIsActiveTrue().forEach { connector ->
            loadConnectorRoutes(connector)
        }
    }

    fun loadConnectorRoutes(connector: Connector) {
        val resource = ResourceHelper.fromBytes("${connector.tag}.yaml", connector.connectorCode.toByteArray())
        PluginHelper.getRoutesLoader(camelContext).loadRoutes(resource)
    }

    fun removeConnectorRoutes(connector: Connector) {
        // Parse YAML to find route IDs and remove them
        // This is more complex as connector YAML can define multiple routes
    }

    @Transactional
    fun activateVersion(id: UUID) {
        val connectorToActivate = connectorRepository.findById(id)
            .orElseThrow { ConnectorNotFound(id) }

        // Find and deactivate current active
        val currentActive = connectorRepository.findByTagAndIsActiveTrue(connectorToActivate.tag)
        if (currentActive != null && currentActive.id != id) {
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

## Phase 6: API Changes for Version Override

### 6.1 Update AggregatedDataProfileRoute

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/AggregatedDataProfileRoute.kt`

Add version query parameter:
```kotlin
private val versionParam = RestParamConfigurationDefinition()
    .name("version")
    .type(RestParamType.query)
    .description("Override version (for testing)")
    .required(false)

rest("/aggregated-data-profiles")
    .get("/{adp_profileName}")
    .routeId("get-aggregated-data-profile-rest-entrypoint")
    .param(profileNamePathParam)
    .param(versionParam)
    .param(idParam)
    .param(containerParamsParam)
    .to("direct:aggregated-data-profile-container-params")
```

Update profile lookup (lines 96-104):
```kotlin
from("direct:aggregated-data-profile-rest-continuation")
    // ... existing variable setup ...
    .process { exchange ->
        val profileName = exchange.getVariable("profile", String::class.java)
        val versionOverride = exchange.getIn().getHeader("version", String::class.java)

        val aggregatedDataProfile = if (versionOverride != null) {
            // Use specific version if query param provided
            aggregatedDataProfileRepository.findByNameAndVersion(profileName, versionOverride)
                ?: throw AggregatedDataProfileNotFound(profileName, versionOverride)
        } else {
            // Use active version
            aggregatedDataProfileRepository.findByNameAndIsActiveTrue(profileName)
                ?: throw AggregatedDataProfileNotFound(profileName)
        }

        exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile.id)
    }
    .toD("direct:aggregated_data_profile_\${variable.aggregatedDataProfileId}")
```

**Important**: Non-active versions won't have routes loaded. Two options:
1. **Lazy loading**: Load route on-demand when version override requested
2. **Always load**: Load all versions' routes at startup (memory impact)

**Recommendation**: Lazy loading with caching:
```kotlin
.process { exchange ->
    val aggregatedDataProfile = // ... lookup as above

    // If not active, ensure route is loaded
    if (!aggregatedDataProfile.isActive) {
        val routeId = "aggregated_data_profile_${aggregatedDataProfile.id}_root"
        if (camelContext.getRoute(routeId) == null) {
            camelContext.addRoutes(AggregatedDataProfileRouteBuilder(aggregatedDataProfile, ...))
        }
    }

    exchange.setVariable("aggregatedDataProfileId", aggregatedDataProfile.id)
}
```

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
                hx-target="#view-panel">
                <cds-text-input
                    name="version"
                    label="Version"
                    placeholder="e.g., 1.1.0"
                    helper-text="Use semantic versioning (major.minor.patch)"
                    required>
                </cds-text-input>
                <p class="cds--label">Current version: <span th:text="${currentVersion}"></span></p>
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

### 7.5 Update Controllers

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
    }
}

// Create new version
@PostMapping("/aggregated-data-profiles/{id}/versions")
fun createVersion(
    @PathVariable id: UUID,
    @RequestParam version: String,
    @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
    httpServletResponse: HttpServletResponse,
): ModelAndView {
    val newProfile = aggregatedDataProfileService.createNewVersion(id, version)

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
2. **Phase 2**: Database migration (foundation for all other changes)
3. **Phase 3**: Domain model changes (depends on Phase 2)
4. **Phase 4**: Repository changes (depends on Phase 3)
5. **Phase 5**: Service layer changes (depends on Phase 4)
6. **Phase 6**: API version override (depends on Phase 5)
7. **Phase 7**: Admin UI (depends on Phase 5)
8. **Phase 8**: Remove auto-reload (depends on Phase 5)
9. **Phase 9**: CSS (depends on Phase 7)

## Testing Considerations

1. **Unit tests** for version creation logic (deep copy, ID remapping)
2. **Integration tests** for:
   - Creating new versions
   - Activating versions
   - Version override query parameter
   - Only one active version per name constraint
3. **UI tests** for version dropdown and modal
4. **Migration test** with existing data

## Open Decisions

1. **Semver validation**: Should we validate that new versions are greater than existing versions?
2. **Version deletion**: Can versions be deleted? What happens to relations/instances?
3. **Cascade on version delete**: When deleting a version, should its relations be deleted?
4. **Max versions**: Should there be a limit on number of versions?
5. **Audit trail**: Should we track when versions are activated/deactivated?

---
date: 2026-01-28T15:23:43Z
researcher: Claude
git_commit: b99beb5272268dcc5063e61ded4ca31b91b7d7d6
branch: main
repository: iko
topic: "Draft Versioning System for ADP and Connector"
tags: [research, codebase, aggregated-data-profile, connector, camel, versioning, drafts]
status: complete
last_updated: 2026-01-28
last_updated_by: Claude
---

# Research: Draft Versioning System for ADP and Connector

**Date**: 2026-01-28T15:23:43Z
**Researcher**: Claude
**Git Commit**: b99beb5272268dcc5063e61ded4ca31b91b7d7d6
**Branch**: main
**Repository**: iko

## Research Question

How can we implement a draft system for AggregatedDataProfiles and Connectors where:
- Working on an ADP or Connector does not affect the currently running routes
- Versions are tracked with semantic versioning (semver)
- The admin UI includes a version dropdown on detail pages
- Only one version is active at a time
- Routes are only loaded into Camel when a version is made active

## Summary

The current architecture stores AggregatedDataProfile and Connector entities without versioning. Each entity directly corresponds to active Camel routes that are loaded at application startup. The route IDs use entity UUIDs, and route lifecycle is managed through `reloadRoutes()` which immediately removes old routes and adds new ones on any edit.

Key findings:
1. **No existing versioning** - Neither ADP nor Connector entities have version fields
2. **Immediate route activation** - All edits immediately reload routes via `reloadRoutes()`
3. **Route IDs use entity UUIDs** - Pattern: `aggregated_data_profile_{id}_root`, `relation_{id}_root`
4. **Route descriptions are available** - Currently used for debugging info, could hold version metadata
5. **Admin UI uses tabbed detail pages** - Page header area available for version dropdown
6. **Cascade relationships exist** - ADP → Relations, Connector → Instances/Endpoints/Roles

## Detailed Findings

### Current Domain Model: AggregatedDataProfile

**Entity Location**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt:35-72`

**Current Fields:**
- `id: UUID` - Primary key
- `name: String` - Unique profile name
- `connectorInstanceId: UUID` - Foreign key to connector instance
- `connectorEndpointId: UUID` - Foreign key to endpoint
- `endpointTransform: EndpointTransform` - JQ expression (embedded)
- `relations: MutableList<Relation>` - Child relations (one-to-many, cascade ALL)
- `resultTransform: Transform` - JQ expression (embedded)
- `roles: Roles` - Access control (embedded)
- `aggregatedDataProfileCacheSetting` - Cache config (embedded)

**Database Table**: `aggregated_data_profile`
- No version column exists
- No timestamp columns (created_at, updated_at)
- Unique constraint on `name`

**Repository Methods** (`AggregatedDataProfileRepository.kt:28-69`):
- `findByName(name: String)` - Lookup by exact name
- `findAllBy(pageable)` - List projection (id, name only)
- `findAllByName(name, pageable)` - Search by name pattern

### Current Domain Model: Connector

**Entity Location**: `src/main/kotlin/com/ritense/iko/connectors/domain/Connector.kt:25-36`

**Current Fields:**
- `id: UUID` - Primary key
- `name: String` - Human-readable name
- `tag: String` - Unique identifier used as YAML filename
- `connectorCode: String` - YAML route definition

**Database Table**: `connector`
- No version column exists
- No timestamp columns
- `tag` used as unique business identifier

**Related Entities:**
- `ConnectorInstance` - Deployed configurations with encrypted credentials
- `ConnectorEndpoint` - Named operations within a connector
- `ConnectorEndpointRole` - RBAC mappings

### Route ID Patterns

**ADP Routes** (built by `AggregatedDataProfileRouteBuilder.kt:68-140`):
- `aggregated_data_profile_{id}_root` - Entry point
- `aggregated_data_profile_{id}_endpoint_transform` - Transform processing
- `aggregated_data_profile_{id}_multicast` - Relation aggregation

**Relation Routes** (built recursively, lines 156-265):
- `relation_{id}_root` - Relation entry
- `relation_{id}_map` - Map-type endpoint mapping
- `relation_{id}_array` - Array-type endpoint mapping
- `relation_{id}_loop` - Iteration route
- `relation_{id}_multicast` - Child aggregation

**Connector Routes** (loaded from YAML at startup):
- Pattern: `direct:iko:connector:{connector.tag}` (defined in YAML)

### Route Description Usage

Route descriptions are set via `.routeDescription()` in `AggregatedDataProfileRouteBuilder.kt`:
- Line 71: `"[ADP Root]"`
- Line 108: `"[ADP Endpoint Transform]"`
- Line 159: `"[${aggregatedDataProfile.name}] <-- [${currentRelation.propertyName}]"`
- Line 186: `"Endpoint mapping (Map): [${currentRelation.propertyName}]"`
- Line 199: `"Endpoint mapping (List): [${currentRelation.propertyName}]"`
- Line 222: `"[${currentRelation.propertyName}] --> Endpoint"`

These descriptions are currently used for debugging/tracing but could be extended to include version information.

### Route Lifecycle Management

**Service Location**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/service/AggregatedDataProfileService.kt`

**Current Operations:**

1. **Startup Loading** (lines 37-49):
```kotlin
@EventListener(ApplicationReadyEvent::class)
fun loadAllAggregatedDataProfilesAtStartup(event: ApplicationReadyEvent) {
    aggregatedDataProfileRepository.findAll().forEach { aggregatedDataProfile ->
        camelContext.addRoutes(AggregatedDataProfileRouteBuilder(...))
    }
}
```

2. **Route Removal** (lines 51-59):
```kotlin
fun removeRoutes(aggregatedDataProfile: AggregatedDataProfile) {
    removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_direct")
    removeRoute("aggregated_data_profile_${aggregatedDataProfile.id}_multicast")
    aggregatedDataProfile.relations.forEach { relation ->
        removeRoute("relation_${relation.id}_direct")
        removeRoute("relation_${relation.id}_multicast")
    }
}
```

3. **Route Addition** (lines 61-71):
```kotlin
fun addRoutes(aggregatedDataProfile: AggregatedDataProfile) {
    camelContext.addRoutes(AggregatedDataProfileRouteBuilder(...))
}
```

4. **Route Reload** (lines 73-76):
```kotlin
fun reloadRoutes(aggregatedDataProfile: AggregatedDataProfile) {
    removeRoutes(aggregatedDataProfile)
    addRoutes(aggregatedDataProfile)
}
```

**Note**: The route IDs used in `removeRoutes()` (`_direct`, `_multicast`) do not match the actual created route IDs (`_root`, `_endpoint_transform`, `_map`, `_array`, `_loop`, `_multicast`). This appears to be a bug where not all routes are being removed during reload.

### Connector Route Loading

**Location**: `src/main/kotlin/com/ritense/iko/connectors/autoconfiguration/ConnectorConfiguration.kt:78-98`

```kotlin
@EventListener(ApplicationReadyEvent::class)
fun loadAllConnectorsAtStartup(event: ApplicationReadyEvent) {
    connectorRepository.findAll().forEach {
        val resource = ResourceHelper.fromBytes("${it.tag}.yaml", it.connectorCode.toByteArray())
        PluginHelper.getRoutesLoader(camelContext).loadRoutes(resource)
    }
}
```

**Key observation**: When editing a connector's YAML code, it is validated by loading into CamelContext (`ConnectorController.kt:162-169`), but old routes are NOT removed. Both old and new routes coexist until application restart.

### Admin UI Structure

**ADP Detail Page** (`templates/fragments/internal/aggregated-data-profile/detail-page.html`):
- Lines 26-28: Fragment wrapper `layout:fragment="view-panel-content"`
- Lines 31-32: Breadcrumb navigation
- Lines 34-35: Page header with `aggregatedDataProfile.name` as title
- Lines 36-55: Three-tab layout (General, Relations, Preview)

**Page Header Pattern** (`templates/layout-internal.html:319-323`):
```html
<th:block th:fragment="page-header">
    <h2 class="cds--type-heading-03" th:text="${title}"></h2>
</th:block>
```

Currently a simple heading with title. No dropdown or version selector exists.

**Connector Detail Page** (`templates/fragments/internal/connector/detailsPageConnector.html`):
- Line 30: Page header with `pageName='Connector'`, title, and subtext (tag)
- Lines 34-59: Three-tab layout (Instances, Connector Code, Endpoints)

### Controller CRUD Operations

**ADP Controller** (`src/main/kotlin/com/ritense/iko/mvc/controller/AggregatedDataProfileController.kt`):
- Create (lines 226-259): Creates ADP, saves, calls `reloadRoutes()`
- Edit (lines 261-293): Updates ADP via `handle()`, calls `reloadRoutes()`, saves
- Delete (lines 462-478): Calls `removeRoutes()`, deletes entity

**Key observation**: Every create/edit immediately activates routes via `reloadRoutes()`.

**Connector Controller** (`src/main/kotlin/com/ritense/iko/mvc/controller/ConnectorController.kt`):
- Create (lines 227-279): Validates YAML by loading, saves if valid
- Edit (lines 154-191): Validates YAML by loading, saves if valid
- Delete (lines 193-218): Manually cascades deletes, removes connector

### Database Migrations

Latest migrations in `src/main/resources/db/migration/`:
- `V2026.01.20.1__rename_adp_role_to_roles.sql` - Most recent schema change
- No version-related columns exist in any migration

### Existing Patterns

**HTMX Response Headers** (used throughout controllers):
- `HX-Trigger: close-modal` - Closes modal dialogs
- `HX-Push-Url: /path` - Updates browser URL
- `HX-Retarget: #selector` - Changes target element
- `HX-Reswap: innerHTML|outerHTML` - Changes swap strategy

**Cascade Delete Pattern** (from `ConnectorController.kt:201-212`):
```kotlin
endpoints.forEach { connectorEndpointRepository.delete(it) }
val roles = connectorEndpointRoleRepository.findAllByConnectorInstance(instance)
roles.forEach { connectorEndpointRoleRepository.delete(it) }
connectorInstanceRepository.delete(instance)
connectorRepository.delete(connector)
```

Manual cascade delete due to JPA relationship structure.

## Code References

### Domain Entities
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt:35-153`
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/Relation.kt:27-48`
- `src/main/kotlin/com/ritense/iko/connectors/domain/Connector.kt:25-36`
- `src/main/kotlin/com/ritense/iko/connectors/domain/ConnectorInstance.kt:33-54`

### Route Building
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/AggregatedDataProfileRouteBuilder.kt:40-267`
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/service/AggregatedDataProfileService.kt:29-81`

### Route Loading
- `src/main/kotlin/com/ritense/iko/connectors/autoconfiguration/ConnectorConfiguration.kt:78-98`

### Admin UI Templates
- `src/main/resources/templates/fragments/internal/aggregated-data-profile/detail-page.html`
- `src/main/resources/templates/fragments/internal/connector/detailsPageConnector.html`
- `src/main/resources/templates/layout-internal.html:319-323`

### Controllers
- `src/main/kotlin/com/ritense/iko/mvc/controller/AggregatedDataProfileController.kt:67-490`
- `src/main/kotlin/com/ritense/iko/mvc/controller/ConnectorController.kt:77-218`

## Architecture Documentation

### Current Data Flow

```
[Database Entity] → [RouteBuilder] → [CamelContext]
       ↑                                    ↓
   [Edit Form] ← [Controller] → [reloadRoutes()] → [Active Route]
```

Every edit immediately affects the running system.

### Route Identification Strategy

Current pattern uses entity UUID in route IDs:
- ADP: `aggregated_data_profile_{UUID}_root`
- Relation: `relation_{UUID}_root`
- Connector: `direct:iko:connector:{tag}` (from YAML)

Version could be appended to create unique route IDs per version:
- Option A: `aggregated_data_profile_{UUID}_v{major}.{minor}.{patch}_root`
- Option B: `aggregated_data_profile_{UUID}_{versionUUID}_root`

### Domain Model Relationships

```
AggregatedDataProfile
├── Relations (1:N, cascade ALL)
├── ConnectorInstance (N:1)
└── ConnectorEndpoint (N:1)

Connector
├── ConnectorInstance (1:N)
├── ConnectorEndpoint (1:N)
└── connectorCode (YAML string)

ConnectorInstance
├── Connector (N:1)
├── ConnectorEndpointRole (1:N)
└── config (Map<String, String>, encrypted)
```

## Open Questions

1. **Relation Versioning**: Should relations be versioned independently or together with their parent ADP?
   - Current structure: Relations are owned by ADP with cascade delete
   - Option: Copy all relations when creating new ADP version

2. **ConnectorInstance Versioning**: Should connector instances be versioned with their connector?
   - Current structure: Instances reference connector but are independent
   - Instance config changes don't affect connector YAML

3. **Route Cleanup Bug**: The `removeRoutes()` method references `_direct` suffix but routes use `_root`, `_map`, `_array`, `_loop` suffixes. Should this be fixed as part of versioning work?

4. **Active Version Storage**: Where to store which version is active?
   - Option A: Boolean `isActive` flag on each version record
   - Option B: Separate table mapping entity ID to active version ID
   - Option C: Parent entity stores active version ID

5. **Migration Strategy**: How to handle existing entities when adding versioning?
   - Option: Create v1.0.0 for all existing entities as part of migration

## Related Research

No prior research documents found in `thoughts/shared/research/`.

---
date: 2026-01-28T16:00:00Z
researcher: Claude (with Tom Bokma)
git_commit: 3c3d89f
branch: feature/draft-system
repository: iko
topic: "Draft Versioning System - Session Summary"
tags: [session-summary, versioning, aggregated-data-profile, connector, camel, semver, drafts]
status: complete
last_updated: 2026-01-28
last_updated_by: Claude
---

# Draft Versioning System - Session Summary

This document summarizes the research and planning session for implementing a draft/versioning system for AggregatedDataProfiles and Connectors.

## Problem Statement

Currently, editing an ADP or Connector immediately affects the running Camel routes. The team wants:
- Working on an ADP or Connector should **not** affect currently running routes
- Introduce versioning with **semver** (e.g., 1.0.0, 1.1.0)
- Only **one version active** at a time per entity
- Admin UI needs a **version dropdown** in the page header with ability to create new versions
- A **query parameter** to override version for testing non-active versions

## Decisions Made

| Question | Decision |
|----------|----------|
| Versioning strategy | Option 1: Add `version` and `isActive` fields to domain entities |
| Relations versioning | Relations are **duplicated** per ADP version (not shared) |
| ConnectorInstance versioning | Instances are **duplicated** per Connector version (not shared) |
| Active version storage | `isActive` boolean on each version row + partial unique index |
| Testing non-active versions | Query parameter `?version=X.Y.Z` overrides active version |
| Migration for existing data | Set all existing entities to version `1.0.0` and `isActive=true` |
| Bug fixes | Fix route removal bug (wrong suffixes in `removeRoutes()`) |

## Research Documents Created

1. **`2026-01-28-draft-versioning-system.md`** - Initial research documenting:
   - Current ADP and Connector domain models
   - Route ID patterns and lifecycle management
   - Admin UI structure (templates, controllers, HTMX patterns)
   - Bug found: `removeRoutes()` uses wrong suffixes (`_direct` instead of `_root`, `_map`, etc.)

2. **`2026-01-28-draft-versioning-implementation-plan.md`** - Detailed implementation plan with:
   - 9 phases covering database, domain, service, API, and UI changes
   - Code snippets for all major changes
   - Flyway migration script
   - Implementation order and dependencies

## Implementation Phases Overview

| Phase | Description | Dependencies |
|-------|-------------|--------------|
| 1 | Implement route groups (ADP + Connector) | None |
| 2 | Database migration (add version/isActive columns) | None |
| 3 | Domain model changes | Phase 2 |
| 4 | Repository changes | Phase 3 |
| 5 | Service layer (activate, createNewVersion) | Phase 4 |
| 6 | API version override query param | Phase 5 |
| 7 | Admin UI (dropdown, modal, activate button) | Phase 5 |
| 8 | Remove auto-reload on edit | Phase 5 |
| 9 | CSS for new page header | Phase 7 |

## Key Technical Details

### Database Changes
```sql
-- New columns on aggregated_data_profile and connector
ALTER TABLE aggregated_data_profile ADD COLUMN version VARCHAR(50) NOT NULL DEFAULT '1.0.0';
ALTER TABLE aggregated_data_profile ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Unique constraint: only one active version per name
CREATE UNIQUE INDEX idx_adp_name_active ON aggregated_data_profile (name) WHERE is_active = TRUE;
```

### Route Group Fix (replaces explicit route removal)

**Problem**: Current `removeRoutes()` tries to remove routes with wrong suffixes (`_direct` instead of `_root`, `_map`, etc.), leaving orphan routes in Camel.

**Solution**: Use Camel's **route groups** feature instead of tracking individual route IDs.

| Entity Type | Group Name Pattern | Example |
|-------------|-------------------|---------|
| AggregatedDataProfile | `adp_{adp.id}` | `adp_550e8400-...` |
| Connector | `connector_{connector.id}` | `connector_660f9500-...` |

**Changes required**:

1. **AggregatedDataProfileRouteBuilder**: Add `.group("adp_${aggregatedDataProfile.id}")` to ALL routes (root, endpoint_transform, multicast, and all relation routes)

2. **AggregatedDataProfileService.removeRoutes()**: Simplify to:
   ```kotlin
   fun removeRoutes(aggregatedDataProfile: AggregatedDataProfile) {
       val groupName = "adp_${aggregatedDataProfile.id}"
       camelContext.getRoutesByGroup(groupName).forEach { route ->
           camelContext.routeController.stopRoute(route.id)
           camelContext.removeRoute(route.id)
       }
   }
   ```

3. **ConnectorService** (new):
   - Consolidates duplicate route loading from `ConnectorConfiguration` and `ConnectorController`
   - Uses Camel's `RoutesLoader.findRoutesBuilders()` to parse YAML into `RouteDefinition` objects
   - Sets `routeDef.group("connector_{id}")` on each RouteDefinition before loading
   - No YAML string manipulation - uses Camel's native API

**Benefits**:
- Simple removal: One call finds all related routes
- Future-proof: Adding new route types doesn't require updating removal code
- Same pattern for ADP and Connector

### Admin UI Header
New page header fragment with:
- Version dropdown (all versions, marks active)
- "New Version" button → modal for semver input
- "Activate" button (shown only for non-active versions)

## Open Questions

These need team input before implementation:

1. **Semver ordering**: Should new versions be validated to be greater than existing versions?
2. **Version deletion**: Can versions be deleted? What about their duplicated relations/instances?
3. **Max versions**: Should there be a limit on versions per entity?
4. **Audit trail**: Track who activated which version and when?

## Files to Modify

### Domain
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt`
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/Relation.kt`
- `src/main/kotlin/com/ritense/iko/connectors/domain/Connector.kt`
- `src/main/kotlin/com/ritense/iko/connectors/domain/ConnectorInstance.kt`

### Services
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/service/AggregatedDataProfileService.kt`
- `src/main/kotlin/com/ritense/iko/connectors/service/ConnectorService.kt` (new)

### Repositories
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/repository/AggregatedDataProfileRepository.kt`
- `src/main/kotlin/com/ritense/iko/connectors/repository/ConnectorRepository.kt`

### Controllers
- `src/main/kotlin/com/ritense/iko/mvc/controller/AggregatedDataProfileController.kt`
- `src/main/kotlin/com/ritense/iko/mvc/controller/ConnectorController.kt`

### API/Camel
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/AggregatedDataProfileRoute.kt`

### Templates
- `src/main/resources/templates/layout-internal.html`
- `src/main/resources/templates/fragments/internal/aggregated-data-profile/detail-page.html`
- `src/main/resources/templates/fragments/internal/connector/detailsPageConnector.html`
- `src/main/resources/templates/fragments/internal/version-modal.html` (new)

### Database
- `src/main/resources/db/migration/V2026.01.28.1__add_versioning.sql` (new)

### CSS
- `src/main/resources/static/css/style.css`

## Next Steps

1. Review this plan with the team
2. Decide on open questions
3. Start implementation (recommended order: Phase 1 → 2 → 3 → 4 → 5 → 6/7/8 in parallel → 9)
4. Write tests for version creation, activation, and query parameter override

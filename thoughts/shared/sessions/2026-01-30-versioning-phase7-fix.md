---
date: 2026-01-30T12:00:00Z
researcher: Claude
branch: feature/draft-system
repository: iko
topic: "Session: Fix versioning fragment parameter passing"
tags: [session, versioning, thymeleaf, fragment-parameters]
status: completed
---

# Session Summary: Fix Versioning Fragment Parameter Passing

## Context

During implementation of the draft versioning system, Phase 7 (Admin UI) had an issue where the `th:replace` invocations in the ADP and Connector detail pages were not passing the required parameters to the versioned page header fragment.

## Problem

The versioned page header fragment (`fragments/internal/versioning/page-header-versioned.html`) expects these parameters:
- `title` - Entity name
- `entityId` - UUID of the entity
- `entityType` - `'aggregated-data-profile'` or `'connector'`
- `currentVersionId` - UUID for dropdown selection
- `versions` - List of version projections
- `isActiveVersion` - Boolean

Both detail pages were calling the fragment without parameters:
```html
<div th:replace="~{fragments/internal/versioning/page-header-versioned :: page-header-versioned}"></div>
```

## Solution

Updated the implementation plan (Phase 7.3 and 7.4) to show the correct syntax for passing parameters from the existing template scope:

**ADP detail-page.html:**
```html
<div th:replace="~{fragments/internal/versioning/page-header-versioned :: page-header-versioned (
    title=${aggregatedDataProfile.name},
    entityId=${aggregatedDataProfile.id},
    entityType='aggregated-data-profile',
    currentVersionId=${aggregatedDataProfile.id},
    versions=${versions},
    isActiveVersion=${aggregatedDataProfile.isActive}
)}"></div>
```

**Connector detailsPageConnector.html:**
```html
<div th:replace="~{fragments/internal/versioning/page-header-versioned :: page-header-versioned (
    title=${connector.name},
    entityId=${connector.id},
    entityType='connector',
    currentVersionId=${connector.id},
    versions=${versions},
    isActiveVersion=${connector.isActive}
)}"></div>
```

## Files Updated

- `thoughts/shared/research/2026-01-28-draft-versioning-implementation-plan.md`:
  - Updated Phase 7.3 with correct ADP th:replace syntax
  - Updated Phase 7.4 with correct Connector th:replace syntax
  - Updated progress table description for Phase 7
  - Updated Key Files Modified section
  - Updated `last_updated_note` metadata

## Next Steps

Implement the fixes in the actual template files:
1. `src/main/resources/templates/fragments/internal/aggregated-data-profile/detail-page.html`
2. `src/main/resources/templates/fragments/internal/connector/detailsPageConnector.html`

## Key Insight

The controllers (`AggregatedDataProfileController.details()` and `ConnectorController.details()`) already provide the necessary data in the model (`aggregatedDataProfile`/`connector` and `versions`). The fix is purely in the Thymeleaf template - the parameters just need to be explicitly passed to the fragment from the existing scope variables.

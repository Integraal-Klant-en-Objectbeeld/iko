---
date: 2026-02-20T00:00:00+01:00
researcher: Maarten van Toor
git_commit: 1234ea31888a3191449c2f01c6c3914e99065d9b
branch: spike/155-drop-down-schema-helper
repository: iko
topic: "JSON Schema generation from ADP transformation expressions"
tags: [research, codebase, aggregated-data-profile, transform, jq, json-schema, caching, openapi, connector]
status: complete
last_updated: 2026-02-20
last_updated_by: Maarten van Toor
last_updated_note: "Added follow-up research on OpenAPI spec storage and proposed generation pipeline"
---

# Research: JSON Schema Generation from ADP Transformation Expressions

**Date**: 2026-02-20
**Researcher**: Maarten van Toor
**Git Commit**: `1234ea31888a3191449c2f01c6c3914e99065d9b`
**Branch**: `spike/155-drop-down-schema-helper`
**Repository**: iko

## Research Question

For an AggregatedDataProfile (ADP) I want to generate a JSON Schema based on the configured
transformation which can be served over an API endpoint so it can be used by consumers to determine
which data paths are available for the ADP. This schema only needs to be generated when
transformations change and can be stored for reuse.

The generation approach: use the OpenAPI specification that each connector typically references to
generate mock/example data, execute the ADP's `resultTransform` JQ expression against that mock
data, and derive a JSON Schema from the output shape.

## Summary

The transformation configuration on an ADP lives in two JQ expression strings stored as plain TEXT
columns in PostgreSQL. These expressions are validated at construction time using
`JsonQuery.compile()` (syntax check only) but are **never statically analysed** to infer their
output shape anywhere in the codebase. Runtime execution is handled entirely by the `camel-jq`
Camel component and does not produce schema metadata.

Connectors use the Apache Camel `camel-rest-openapi` component. The OpenAPI spec location
(`specificationUri`) is stored as an **encrypted entry** in `ConnectorInstance.config` (the
per-instance key-value map). The `ConnectorEndpoint.operation` string is used as the OpenAPI
`operationId` in the Camel route URI (`rest-openapi:{specificationUri}#{operation}?host=...`).
This means the response schema for a given connector endpoint can be retrieved by:
(1) decrypting the `specificationUri` from the instance config, (2) loading the OpenAPI spec,
and (3) resolving the operation by its `operationId`.

There are **no existing JSON API endpoints** for ADP; all current MVC handlers are admin UI
endpoints that return Thymeleaf fragments. The actual data delivery endpoint
(`/aggregated-data-profiles/{profileName}`) is served from the `/aggregated-data-profiles/**`
security filter chain via the Camel routing layer, not from the MVC controllers.

There is **no existing schema storage column** in the database. Adding schema generation therefore
requires a new DB column, a new API endpoint, and a schema computation pipeline.

---

## Detailed Findings

### 1. Where Transformation Expressions Live

#### AggregatedDataProfile entity
`src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt:59-71`

| Field | Embeddable class | DB column | Purpose |
|---|---|---|---|
| `endpointTransform` | `EndpointTransform` | `endpoint_transform TEXT` | JQ expression that produces HTTP headers for the primary connector call |
| `resultTransform` | `Transform` | `transform TEXT` | JQ expression that shapes the connector response into the final output callers see |

#### Relation entity
`src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/Relation.kt:38-45`

| Field | Embeddable class | DB column | Purpose |
|---|---|---|---|
| `endpointTransform` | `RelationEndpointTransform` | `source_to_endpoint_mapping TEXT` | JQ expression mapping parent body into HTTP headers for the child connector call |
| `resultTransform` | `Transform` | `transform TEXT` | JQ expression shaping the child response into the value stored under `propertyName` |

All three embeddable classes (`Transform`, `EndpointTransform`, `RelationEndpointTransform`) are
structurally identical: a single `expression: String` field with `JsonQuery.compile()` called in
`init`.

---

### 2. JQ Validation — What the Library Can and Cannot Do

#### Current validation (compile-only)
`src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/Transform.kt:30-41`

```kotlin
companion object {
    fun validate(expression: String) {
        try {
            JsonQuery.compile(expression, Versions.JQ_1_6)
        } catch (e: JsonQueryException) {
            throw IllegalArgumentException("Invalid expression: $expression", e)
        }
    }
}
```

`JsonQuery.compile()` parses and validates JQ syntax. The returned `JsonQuery` object is discarded;
nothing inspects the compiled AST.

The same call appears in `ValidTransformValidator` (MVC form validation layer,
`src/main/kotlin/com/ritense/iko/mvc/model/validation/ValidTransformValidator.kt:30-43`) and in
the test-only `JQTest.kt`.

#### What `net.thisptr.jackson.jq` exposes at runtime
`src/test/kotlin/com/ritense/iko/JQTest.kt:31-62`

```kotlin
val scope = Scope.newEmptyScope()
BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, scope)
val query = JsonQuery.compile(jqExpr, Versions.JQ_1_6)
val out = mutableListOf<JsonNode>()
query.apply(scope, input) { out.add(it) }   // callback-based, returns JsonNode results
```

The library provides `query.apply(scope, input, callback)` for execution. It does **not** provide
any output-schema introspection or type-inference API. The only way to determine the output shape
of a JQ expression is to execute it against real (or representative sample) input data.

#### Static analysis limitation
JQ is a Turing-complete expression language. Many constructs (`if`/`else`, `try`/`catch`,
recursive descent `..`, `path()`, user-defined functions) make static output-shape inference
impractical in the general case. The library does not expose an AST visitor API.

---

### 3. How the resultTransform Output Reaches Callers

The data flow through Camel routes
(`src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/AggregatedDataProfileRouteBuilder.kt`):

```
HTTP GET /aggregated-data-profiles/{profileName}
    → AggregatedDataProfileTemplatesRouteBuilder — look up active ADP by name
    → direct:aggregated_data_profile_{id}
        → endpointTransform JQ → HTTP headers for connector call
        → cache check (Redis) — if hit, short-circuit and return cached body
        → connector call (external HTTP via Camel connector)
        → IF relations exist:
              enrich with PairAggregator → body becomes {left: <adp_result>, right: {propertyName: relationResult}}
              (each relation is itself: endpointTransform → connector call → resultTransform)
        → resultTransform JQ → shapes final body
        → cache put (Redis, TTL = cacheSettings.timeToLive ms)
        → marshal().json() → HTTP response
```

The `resultTransform` at the ADP root receives:
- When **no relations**: the raw connector HTTP response body (JSON)
- When **relations present**: `{"left": <adpRawBody>, "right": {"<propertyName>": <relationResult>, ...}}`
  from `PairAggregator` (`src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/PairAggregator.kt:37-41`)

The output of `resultTransform` is what the JSON Schema needs to describe.

---

### 4. Existing API Endpoints — No Schema Endpoint Exists

`src/main/kotlin/com/ritense/iko/mvc/controller/AggregatedDataProfileController.kt`

All current endpoints are under `/admin/**` (admin UI, Thymeleaf/HTMX, OAuth2 session auth).
None return JSON schema or structural metadata. The only "metadata-like" endpoints are dropdown
loaders for the admin forms:
- `GET /admin/aggregated-data-profiles/create/endpoints?connectorInstanceId={uuid}` — returns
  endpoint list for a connector instance (HTML fragment)

There is no `/aggregated-data-profiles/{name}/schema` or equivalent endpoint anywhere.

The data delivery endpoint is outside the MVC layer: it is the Camel REST route
`/aggregated-data-profiles/{profileName}` served from the `/aggregated-data-profiles/**` security
filter chain (JWT bearer token auth).

---

### 5. Caching Infrastructure Available for Schema Storage

#### Redis (runtime cache)
`src/main/kotlin/com/ritense/iko/cache/service/CacheService.kt`
`src/main/kotlin/com/ritense/iko/cache/processor/CacheProcessor.kt`

The existing Redis cache stores the final **response body** string per ADP/Relation, keyed by:
```
"<entity-id>:" + SHA256(<id> + <endpointTransform.expression> + <endpointTransformResult> + <resultTransform.expression>)
```

The cache key already includes both transform expressions. When transforms change, a new ADP
version is created (versioning system, `is_active` flag), so previously cached response bodies
for the old version become unreachable by new keys.

The `CacheService` has:
- `set(key, value)` / `set(key, value, ttl)` — store string
- `get(key)` — retrieve string
- `evictByPrefix(id)` — delete all keys starting with `"<id>"` (used by cache eviction UI)
- `isCached(id)` — checks `KEYS "<id>:*"` pattern

#### PostgreSQL (persistent config storage)
Current schema has no column for storing a generated schema alongside the ADP. The `aggregated_data_profile`
table columns are all configuration fields. A new migration would be needed to add a
`jsonschema TEXT` (or `JSONB`) column.

---

### 6. Versioning and Change Detection

`src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt:152-165`

```kotlin
fun createNewVersion(newVersion: Version): AggregatedDataProfile {
    return AggregatedDataProfile(
        id = UUID.randomUUID(),
        name = name,
        version = newVersion,
        isActive = false,
        ...
    )
}
```

- `(name, version)` is a unique constraint.
- Only one version per name can have `is_active = true` (partial unique index).
- `POST /admin/aggregated-data-profiles/{id}/versions` creates a new inactive version; `POST /admin/aggregated-data-profiles/{id}/activate` activates it.
- Editing transforms on an existing ADP (`PUT /admin/aggregated-data-profiles`, line 309) does NOT
  reload routes — changes only take effect when the version is activated.
- Relations also trigger route reloads when changed, but only on the active version (controllers
  call `aggregatedDataProfileService.reloadRoute()` only when `aggregatedDataProfile.isActive`).

This versioning model means:
- A schema is tied to a specific `(name, version)` pair (i.e., a specific `AggregatedDataProfile.id`).
- Schema is effectively invalidated whenever a new version is activated (new ADP entity, new `id`).
- Within a version, transforms can be changed via edit form — schema would need invalidation on save.

---

### 7. DB Schema — What Currently Exists vs. What's Missing

Current `aggregated_data_profile` effective columns (from Flyway migrations):
```
id                    UUID PK
name                  VARCHAR(255)
roles                 VARCHAR(255)
version               VARCHAR(50) NOT NULL DEFAULT '1.0.0'
is_active             BOOLEAN NOT NULL DEFAULT TRUE
connector_instance_id UUID FK → connector_instance(id)
connector_endpoint_id UUID FK → connector_endpoint(id)
endpoint_transform    TEXT DEFAULT '.'
transform             TEXT          ← this is resultTransform
cache_enabled         BOOLEAN NOT NULL DEFAULT FALSE
cache_ttl             INTEGER NOT NULL DEFAULT 0
```

No `jsonschema` or equivalent column exists. A new Flyway migration would be needed to add one.

---

### 8. endpointTransformContext — Input Shape Available to endpointTransform

`src/main/kotlin/com/ritense/iko/aggregateddataprofile/processor/ContainerParamsProcessor.kt:47-63`

The input context for `endpointTransform` expressions (and the `source` injection for relations)
is predictable in structure:

**ADP root `endpointTransform` input:**
```json
{
  "idParam": "<string or null>",
  "sortParams": { "<containerId>": { /* Pageable */ } },
  "filterParams": { "<containerId>": { "<key>": "<value>" } }
}
```

**Relation `endpointTransform` input (same as above + parent body):**
```json
{
  "idParam": "...",
  "sortParams": { ... },
  "filterParams": { ... },
  "source": <parent connector response body>
}
```

The `source` field's structure depends on the parent connector's response, not on ADP
configuration — it's not known at schema-generation time without a sample run.

---

---

## Follow-up Research 2026-02-20 — OpenAPI Spec Storage and Schema Generation Pipeline

### 9. OpenAPI Spec Storage in ConnectorInstance

The key discovery is that the OpenAPI specification is stored **per instance**, not per connector
template. Camel's `rest-openapi` component is what consumes it.

#### Camel route YAML pattern (from test seed data)
`src/test/resources/db/migration-test/V2026.01.08.1__adp_test_seed_data.sql:14,56-57`

```yaml
rest-openapi:${variable.configProperties.specificationUri}#${variable.operation}?host=...
```

The Camel URI structure is: `rest-openapi:<specURI>#<operationId>?host=...`

- `${variable.configProperties.specificationUri}` — resolved from `ConnectorInstance.config["specificationUri"]`
- `${variable.operation}` — resolved from the Camel route variable `operation`, which is set from
  `ConnectorEndpoint.operation` during route execution

#### Where `specificationUri` is stored
`src/main/kotlin/com/ritense/iko/connectors/domain/ConnectorInstance.kt:48`

```kotlin
@Convert(attributeName = "value", converter = AesGcmStringAttributeConverter::class)
var config: Map<String, String>
```

The `config` map key `specificationUri` holds the spec location. Values are AES-GCM encrypted in
the `connector_instance_config` table. The key `specificationUri` is a convention, not enforced by
the domain model — it is only meaningful when the `connectorCode` YAML uses the `rest-openapi`
Camel component URI scheme.

#### Possible `specificationUri` values
From the test seed data and connector documentation:
- `classpath:pet-api.yaml` — spec bundled on the classpath (test fixtures only)
- Presumably: `http://...` or `https://...` — remote spec URL for production connectors

Production connector documentation confirms all six documented connectors use `specificationUri`
pointing to the external system's OpenAPI URL:
- `doc/connectors/haalcentraal-brp.md:7,15,73`
- `doc/connectors/openklant.md:7,16,53`
- `doc/connectors/opendocumenten.md:7,17,70`
- `doc/connectors/objectenapi.md:7,16,66`
- `doc/connectors/openzaak.md:7,19,101`
- `doc/connectors/bag.md:7,16,64`

#### `ConnectorEndpoint.operation` = OpenAPI `operationId`
`src/main/kotlin/com/ritense/iko/connectors/domain/ConnectorEndpoint.kt:36-38`

```kotlin
@Column(name = "operation")
var operation: String
```

When the connector's `connectorCode` YAML uses `rest-openapi:...#${variable.operation}`, this
string is treated by the Camel `rest-openapi` component as the OpenAPI `operationId`. This is the
handle to look up the specific operation's request/response schema in the OpenAPI spec document.

#### No OpenAPI spec on the Connector entity itself
`src/main/kotlin/com/ritense/iko/connectors/domain/Connector.kt:32-57`

The `Connector` entity has only: `id`, `name`, `tag`, `version`, `isActive`, `connectorCode`
(YAML text). There is no `specificationUri` or OpenAPI-related field. The spec location lives
exclusively in `ConnectorInstance.config`.

---

### 10. Camel OpenAPI Libraries Already in the Build

`build.gradle.kts:94,98`

```kotlin
implementation(libs.camel.openapi.java.starter)   // camel-openapi-java-starter
implementation(libs.camel.rest.openapi)            // camel-rest-openapi
```

`gradle/libs.versions.toml:36,40`

```toml
camel-openapi-java-starter = { group = "org.apache.camel.springboot", name = "camel-openapi-java-starter" }
camel-rest-openapi = { group = "org.apache.camel", name = "camel-rest-openapi" }
```

Both libraries are already on the classpath. `camel-rest-openapi` exposes the parsed OpenAPI model
that it uses internally to build HTTP requests — this model can in principle be accessed
programmatically to read response schemas without making HTTP calls.

There are no `springdoc`, `springfox`, or standalone Swagger UI dependencies.

---

### 11. Proposed Schema Generation Pipeline

Given the above, the pipeline for generating a JSON Schema for an ADP is:

```
AggregatedDataProfile (id, connectorInstanceId, connectorEndpointId, resultTransform, relations)
    │
    ├─ 1. Load ConnectorInstance by connectorInstanceId
    │       → decrypt config map → read specificationUri
    │
    ├─ 2. Load ConnectorEndpoint by connectorEndpointId
    │       → read operation (= OpenAPI operationId)
    │
    ├─ 3. Fetch + parse OpenAPI spec from specificationUri
    │       → find operation by operationId
    │       → extract 200-response JSON schema
    │
    ├─ 4. Generate mock JSON from OpenAPI response schema
    │       (example values / synthetic defaults)
    │
    ├─ 5. For each Relation (recursively):
    │       → repeat steps 1–4 for its connectorInstance + connectorEndpoint
    │       → generate relation mock
    │       → apply relation resultTransform JQ to relation mock
    │       → collect as {propertyName: relationResult, ...} (MapAggregator equivalent)
    │
    ├─ 6. If relations exist: compose {left: <adpMock>, right: <relationsMap>}
    │       (PairAggregator equivalent)
    │       else: use adpMock directly
    │
    ├─ 7. Execute ADP resultTransform JQ against the composed input
    │       → use JsonQuery.compile() + query.apply(scope, input, callback)
    │       → needs: Scope + BuiltinFunctionLoader (pattern from JQTest.kt:31-42)
    │
    ├─ 8. Infer JSON Schema from the JQ output JsonNode
    │       (walk the node tree, map JSON types to JSON Schema types)
    │
    └─ 9. Store generated schema in aggregated_data_profile.jsonschema column (new)
```

#### Key execution code pattern (from JQTest.kt)
```kotlin
val scope = Scope.newEmptyScope()
BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, scope)
val query = JsonQuery.compile(resultTransform.expression, Versions.JQ_1_6)
val out = mutableListOf<JsonNode>()
query.apply(scope, mockInput) { out.add(it) }
// out.first() is the shaped output → derive JSON Schema from its structure
```

#### Complication: specificationUri is instance-specific, not connector-level

The same `Connector` (template) may have multiple `ConnectorInstance` records each with a
different `specificationUri` (e.g., pointing to different API server environments). An ADP binds
to a specific `ConnectorInstance`, so the spec resolution is always deterministic: one ADP →
one instance → one spec URI.

#### Complication: specificationUri may require network access

Production connectors reference external OpenAPI URLs. Schema generation needs to load these
specs, which means:
- The application must have network access to the external system at schema generation time
- HTTP specs may require auth headers (some government APIs serve the spec unauthenticated,
  but this is not guaranteed)
- A `classpath:` spec (as in tests) requires the spec file to be on the application classpath

#### Complication: relation `endpointTransform` uses parent body as `"source"`

The relation's `endpointTransform` JQ expression receives the parent connector's response as
`"source"` in the `endpointTransformContext`. This transform's output determines the HTTP headers
for the relation connector call — but for schema generation this doesn't matter because the mock
data is taken directly from the OpenAPI response schema (step 4), bypassing the actual connector
call entirely.

---

### 12. Schema Invalidation Points

A stored schema (once generated) needs invalidation when:

1. **New version activated** — `POST /admin/aggregated-data-profiles/{id}/activate` creates a
   new `AggregatedDataProfile` entity with a new `id`. The old entity's schema remains stored
   but becomes irrelevant (inactive version). The new entity's schema column starts as `null`.

2. **Transform edited within same version** — `PUT /admin/aggregated-data-profiles` (line 286 of
   `AggregatedDataProfileController.kt`) calls `aggregatedDataProfile.handle(form)` which replaces
   `endpointTransform` and `resultTransform`. The schema column on that same entity would need
   to be cleared/set to `null` on this edit. Same applies to relation edits via
   `PUT /admin/relations` when the ADP is not active (active versions don't accept relation edits
   without route reload; inactive ones can be freely edited).

3. **ConnectorInstance spec changes** — If `specificationUri` is updated in an instance config,
   the ADP's schema (derived from that spec) is stale. Currently there is no link from
   `connector_instance_config` changes back to ADPs using that instance.

---

## Code References

- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt:59-71` — transform field declarations
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/Transform.kt:26-42` — `Transform` embeddable + compile validation
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/EndpointTransform.kt:26-42` — `EndpointTransform` embeddable
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/RelationEndpointTransform.kt:26-42` — `RelationEndpointTransform` embeddable
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/Relation.kt:38-45` — relation transform fields
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/AggregatedDataProfileRouteBuilder.kt:61-106` — transform execution in Camel routes
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/AggregatedDataProfileRouteBuilder.kt:156-225` — relation route transforms (including array vs object branching)
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/PairAggregator.kt:37-41` — `{left, right}` structure produced when relations exist
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/MapAggregator.kt:22-44` — relation results keyed by `propertyName`
- `src/main/kotlin/com/ritense/iko/mvc/model/validation/ValidTransformValidator.kt:25-43` — JQ compile validation in form layer
- `src/main/kotlin/com/ritense/iko/mvc/controller/AggregatedDataProfileController.kt:68-106` — all current ADP endpoints (admin UI only)
- `src/main/kotlin/com/ritense/iko/cache/processor/CacheProcessor.kt:36-68` — cache check/put with expression-based key
- `src/main/kotlin/com/ritense/iko/cache/service/CacheService.kt:54-95` — Redis operations
- `src/main/kotlin/com/ritense/iko/aggregateddataprofile/processor/ContainerParamsProcessor.kt:47-63` — `endpointTransformContext` shape
- `src/test/kotlin/com/ritense/iko/JQTest.kt:31-62` — only place `Scope`, `BuiltinFunctionLoader`, and `query.apply()` are used

---

## Architecture Documentation

### Transform expression storage pattern

Both ADP and Relation follow the same pattern: JQ expressions are stored as plain TEXT in
PostgreSQL, wrapped in an `@Embeddable` value object that validates syntax at construction
time. The compiled `JsonQuery` object is not cached or reused — each validation call discards
the compiled result. At Camel route build time, expressions are re-compiled implicitly by the
`camel-jq` component.

### No static output-shape inference exists

The `net.thisptr.jackson.jq` library does not expose schema inference. The codebase has zero
usages of the compiled `JsonQuery` AST beyond compile-as-validation. Any schema generation
approach must either: (a) execute the transform against representative sample data, or (b)
implement a partial JQ-to-JSON-Schema translator for the subset of expressions used in practice.

### Versioning as the natural invalidation boundary

The ADP versioning system (`is_active`, unique `(name, version)` constraint) already acts as a
natural schema invalidation boundary: each activated version gets a new `id`. A stored schema
could be keyed by `AggregatedDataProfile.id` and considered valid for the lifetime of that
version.

### Admin UI vs. API separation

The admin UI (`/admin/**`) and the data API (`/aggregated-data-profiles/**`, `/endpoints/**`)
are served by different security filter chains (OAuth2/OIDC session vs. JWT bearer). A new
schema endpoint would be placed under the API filter chain (JWT) to align with consumer access
patterns, mirroring the existing data endpoint structure.

---

## Decisions

1. **OpenAPI spec accessibility**: Specs are assumed to be publicly accessible without
   authentication when `specificationUri` is an HTTP/HTTPS URL. No auth header injection is
   needed for spec fetching.

2. **Trigger for generation**: Schema generation is triggered asynchronously after a save action
   (ADP edit or relation change) to avoid blocking the user. The `jsonschema` column will be
   `null` until generation completes. Consumers requesting the schema endpoint before generation
   finishes will receive a `404` or an empty/pending response.

3. **Mock data breadth**: The mock connector response must be as broad as possible — all fields
   from the OpenAPI response schema are included in the mock, both required and optional. For
   `oneOf`/`anyOf`, all variants are merged. For `$ref`, references are fully resolved. The goal
   is to produce a mock that exposes every possible data path so the `resultTransform` JQ can
   reference them and the derived schema captures the full output structure.

4. **Schema storage column type**: `TEXT`. The schema is stored and served as-is; no server-side
   JSON path querying is needed.

5. **Invalidation trigger**: Schema generation is triggered whenever `resultTransform` changes,
   regardless of whether the ADP version is active or not. When an inactive version is later
   activated, the already-generated schema for that version is used as-is — no regeneration on
   activation. Other active/inactive versions of the same ADP name are never touched.

6. **Invalidation when ConnectorInstance config changes**: Out of scope. In practice, a change to
   `specificationUri` (a different external API) means the ADP itself needs to be updated to
   reflect new data paths, which will naturally trigger schema regeneration via the transform edit
   flow.

7. **API endpoint location**: Consumer-facing JWT endpoint only as the primary deliverable
   (`/aggregated-data-profiles/{name}/schema`). Additionally, the generated schema should be
   viewable (read-only) on the ADP connector detail page in the admin UI.

8. **Relation array output wrapping**: When a relation's `endpointTransform` would produce an
   `ArrayNode` at runtime (triggering batch mode), the mock generation wraps the single-element
   `resultTransform` output in an array before placing it under `propertyName` in the `right`
   side of the composed mock. This ensures the ADP's `resultTransform` JQ receives the same
   `{left, right: {propertyName: [...]}}` structure it sees at runtime, producing a correct
   schema for the final output.

   To detect array mode during mock generation without executing the relation's `endpointTransform`
   against real data: the mock generator must evaluate the relation's `endpointTransform` JQ
   against a mock `endpointTransformContext` (with `source` populated from the parent mock) and
   check whether the result is an `ArrayNode` or `ObjectNode`. The same branching logic from
   `AggregatedDataProfileRouteBuilder.kt:179-188` applies.

9. **Non-OpenAPI connectors**: If the `ConnectorInstance` config does not contain a
   `specificationUri` key, or if the connector is not of the `camel-rest-openapi` type, schema
   generation is **skipped silently** for that ADP. The `jsonschema` column remains `null` and
   no error is raised. This handles connectors that integrate via other mechanisms (e.g., custom
   Camel DSL without `rest-openapi`).

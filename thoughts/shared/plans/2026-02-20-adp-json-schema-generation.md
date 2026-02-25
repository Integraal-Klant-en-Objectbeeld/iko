---
date: 2026-02-20T00:00:00+01:00
author: Claude (claude-sonnet-4-6)
research: thoughts/shared/research/2026-02-20-adp-json-schema-generation.md
branch: spike/155-drop-down-schema-helper
---

# ADP JSON Schema Generation — Implementation Plan

## Overview

For each `AggregatedDataProfile` (ADP), generate and persist a JSON Schema that describes the
shape of the `resultTransform` output. The schema is derived by:

1. Loading the OpenAPI specification referenced by the connector instance
2. Generating a maximally broad mock JSON payload from the response schema
3. Recursively composing mock data for all nested relations
4. Executing the ADP's `resultTransform` JQ expression against the composed mock
5. Inferring a JSON Schema from the resulting `JsonNode`

The schema is generated synchronously during saves that affect the transform output shape, so any
errors are immediately reflected to the caller. The schema is exposed via a consumer-facing
JWT-secured API endpoint and a read-only admin UI panel.

---

## Current State Analysis

- `AggregatedDataProfile` stores `resultTransform` (TEXT) and `endpointTransform` (TEXT) in
  PostgreSQL; no schema column exists yet.
- JQ is Turing-complete — static inference is impossible; execution against representative input
  is the only viable approach.
- `swagger-parser` v3 (`io.swagger.parser.v3:swagger-parser:2.1.37`) is already on the classpath
  transitively from `camel-rest-openapi`.
- `ConnectorInstance.config` is an AES-GCM-encrypted `Map<String, String>` where `specificationUri`
  holds the OpenAPI spec location (classpath or HTTP URL). JPA decrypts it on read.
- Schema generation runs synchronously within the save transaction so errors propagate to the
  caller. No async infrastructure is needed.
- The Camel REST route for data delivery is at exactly 2 path segments
  (`/aggregated-data-profiles/{adp_profileName}`). A Spring MVC controller at
  `/aggregated-data-profiles/{name}/schema` (3 segments) does not conflict and is covered by the
  existing JWT security filter chain (`SecurityConfig.kt:74-76`).
- Route builder (`AggregatedDataProfileRouteBuilder.kt`) is recursive: both ADPs and relations
  receive `{left: connectorResponse, right: {propName: childResult, ...}}` as `resultTransform`
  input when children exist, or the raw connector response when no children exist — at every level
  of the hierarchy.

---

## Desired End State

When implemented, an operator can:
1. Edit and save an ADP (transforms or relations). The JSON Schema is immediately regenerated and
   any errors are shown to the operator.
2. A consumer can `GET /aggregated-data-profiles/{name}/schema` (JWT auth) to retrieve the schema.
3. The admin can click "Regenerate Schema" to re-trigger generation manually (e.g. after a spec URL
   became reachable again).

### Verification
- `GET /aggregated-data-profiles/{name}/schema` with a valid JWT returns a valid JSON Schema
  object after saving the ADP.
- The schema accurately reflects the shape of a live call to
  `GET /aggregated-data-profiles/{name}`.
- Editing `resultTransform` clears the old schema and regenerates it synchronously.
- Relation changes (add/edit/delete) do **not** trigger schema regeneration.
- Creating a new version carries over the existing schema from the source ADP.

---

## What We Are NOT Doing

- No schema diff/versioning history beyond the per-entity-id column.
- No auth header injection for OpenAPI spec fetching (specs assumed publicly accessible; decision
  from research doc).
- No schema invalidation when `ConnectorInstance.specificationUri` changes independently of the
  ADP (out of scope per research decision 6).
- No partial JQ-to-JSON-Schema static analysis — runtime execution only.
- No OpenAPI spec caching (loading is synchronous within the save request; may be added later).
- **No schema generation for non-OpenAPI connectors**: if `specificationUri` is absent from
  `ConnectorInstance.config` (i.e. the connector does not use `camel-rest-openapi`), schema
  generation is silently skipped and `jsonschema` remains `null`. This is not treated as an error.

---

## Implementation Approach

Six ordered phases. Each phase produces a testable increment.

```
Phase 1: DB + Domain          → new column as constructor param
Phase 2: Version Carry-over   → pass jsonschema through createNewVersion constructor
Phase 3: Schema Core          → mock generator, JQ executor, schema inferrer, orchestrator service
Phase 4: Sync Trigger         → call schema service directly from controller after saves
Phase 5: Consumer API         → JWT-secured GET endpoint
Phase 6: Admin UI             → Schema tab, read-only Monaco editor, Regenerate button
```

---

## Phase 1: Database Column + Domain Model

### Overview
Add the `jsonschema TEXT` column and the corresponding constructor parameter to
`AggregatedDataProfile`.

### Changes Required

#### 1.1 Flyway Migration

**File**: `src/main/resources/db/migration/V2026.02.20.1__add_adp_jsonschema.sql`

```sql
ALTER TABLE aggregated_data_profile
    ADD COLUMN jsonschema TEXT;
```

#### 1.2 `AggregatedDataProfile` entity

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt`

Add `jsonschema` as a constructor parameter with default `null` (after `aggregatedDataProfileCacheSetting`):

```kotlin
@Column(name = "jsonschema")
var jsonschema: String? = null,
```

### Success Criteria

#### Automated Verification
- [ ] Migration applies cleanly: `./gradlew flywayMigrate` (or via `./gradlew bootRun` on a clean
  DB)
- [x] `AggregatedDataProfile` compiles with the new field: `./gradlew compileKotlin`
- [x] Existing unit tests still pass: `./gradlew test`
- [x] Spotless formatting: `./gradlew spotlessApply && ./gradlew spotlessCheck`

#### Manual Verification
- [ ] Editing an ADP in the admin UI and saving does not throw errors.

---

## Phase 2: Version Carry-over

### Overview
Adapt `createNewVersion()` to pass `jsonschema` through the constructor so a new version inherits
the schema from its source ADP.

### Changes Required

#### 2.1 `AggregatedDataProfile.createNewVersion()`

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/domain/AggregatedDataProfile.kt`

Add `jsonschema = this.jsonschema` to the `AggregatedDataProfile(...)` constructor call in
`createNewVersion()`:

```kotlin
fun createNewVersion(newVersion: String): AggregatedDataProfile = AggregatedDataProfile(
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
    relations = mutableListOf(),
    jsonschema = this.jsonschema,
)
```

### Success Criteria

#### Automated Verification
- [ ] `./gradlew compileKotlin`
- [ ] Existing unit tests still pass: `./gradlew test`
- [ ] `./gradlew spotlessApply && ./gradlew spotlessCheck`

#### Manual Verification
- [ ] Creating a new version of an ADP carries over the `jsonschema` value from the source.

---

## Phase 3: Schema Generation Core

### Overview
Implement three focused classes and one orchestrating service in a new
`aggregateddataprofile/schema/` package:

| Class | Responsibility |
|---|---|
| `OpenApiMockGenerator` | Load OpenAPI spec, walk response schema, produce a maximally broad `JsonNode` |
| `JsonSchemaInferrer` | Walk a `JsonNode` tree, produce a JSON Schema string |
| `AdpSchemaService` | Orchestrate mock generation (recursive), JQ execution, schema inference, persistence. Called synchronously from the controller so errors propagate. |

#### 3.1 `OpenApiMockGenerator`

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/schema/OpenApiMockGenerator.kt`

```kotlin
// Copyright (C) 2026 Ritense BV, the Netherlands.
// Licensed under EUPL, Version 1.2 (the "License");
// ...

package com.ritense.iko.aggregateddataprofile.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.*
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.parser.OpenAPIV3Parser
import org.springframework.stereotype.Component

@Component
internal class OpenApiMockGenerator {

    private val mapper = ObjectMapper()

    fun loadSpec(specUri: String): OpenAPI =
        when {
            specUri.startsWith("classpath:") -> {
                val path = specUri.removePrefix("classpath:")
                val content = javaClass.classLoader.getResourceAsStream(path)
                    ?.bufferedReader()?.readText()
                    ?: error("Classpath resource not found: $path")
                OpenAPIV3Parser().readContents(content).openAPI
                    ?: error("Failed to parse OpenAPI spec from classpath: $path")
            }
            else -> OpenAPIV3Parser().read(specUri)
                ?: error("Failed to fetch or parse OpenAPI spec from: $specUri")
        }

    /**
     * Finds the response schema for the given operationId (HTTP 200 / first success code)
     * and generates a maximally broad mock JsonNode that includes all properties
     * (required and optional) recursively.
     */
    fun generateMock(openApi: OpenAPI, operationId: String): JsonNode {
        val schema = findResponseSchema(openApi, operationId)
            ?: return NullNode.instance
        return generateNode(schema, openApi, depth = 0)
    }

    private fun findResponseSchema(openApi: OpenAPI, operationId: String): Schema<*>? {
        for (pathItem in openApi.paths?.values ?: emptyList()) {
            val operation = listOfNotNull(
                pathItem.get, pathItem.post, pathItem.put,
                pathItem.patch, pathItem.delete
            ).firstOrNull { it.operationId == operationId } ?: continue

            val response = operation.responses
                ?.entries
                ?.firstOrNull { (code, _) -> code == "200" || code.startsWith("2") }
                ?.value ?: continue

            return response.content
                ?.entries
                ?.firstOrNull { (mediaType, _) -> mediaType.contains("json") }
                ?.value?.schema
        }
        return null
    }

    private fun generateNode(schema: Schema<*>, openApi: OpenAPI, depth: Int): JsonNode {
        if (depth > 20) return NullNode.instance

        // Resolve $ref
        schema.`$ref`?.let { ref ->
            val refName = ref.substringAfterLast("/")
            val resolved = openApi.components?.schemas?.get(refName) ?: return NullNode.instance
            return generateNode(resolved, openApi, depth + 1)
        }

        // Merge allOf / anyOf / oneOf into a single object
        val composites = listOfNotNull(schema.allOf, schema.anyOf, schema.oneOf).flatten()
        if (composites.isNotEmpty()) {
            val merged = mapper.createObjectNode()
            composites.forEach { sub ->
                val subNode = generateNode(sub, openApi, depth + 1)
                if (subNode.isObject) merged.setAll<ObjectNode>(subNode as ObjectNode)
            }
            // Also include any direct properties on this schema node
            if (!schema.properties.isNullOrEmpty()) {
                val direct = generateByType(schema, openApi, depth)
                if (direct.isObject) merged.setAll<ObjectNode>(direct as ObjectNode)
            }
            return if (merged.size() > 0) merged else NullNode.instance
        }

        return generateByType(schema, openApi, depth)
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateByType(schema: Schema<*>, openApi: OpenAPI, depth: Int): JsonNode =
        when (schema.type) {
            "object" -> {
                val obj = mapper.createObjectNode()
                schema.properties?.forEach { (key, propSchema) ->
                    obj.set<JsonNode>(key, generateNode(propSchema as Schema<*>, openApi, depth + 1))
                }
                obj
            }
            "array" -> {
                val arr = mapper.createArrayNode()
                schema.items?.let { arr.add(generateNode(it as Schema<*>, openApi, depth + 1)) }
                arr
            }
            "string"  -> TextNode.valueOf(schema.example?.toString() ?: schema.enum?.firstOrNull()?.toString() ?: "example")
            "integer" -> IntNode.valueOf((schema.example as? Int) ?: 0)
            "number"  -> DoubleNode.valueOf((schema.example as? Number)?.toDouble() ?: 0.0)
            "boolean" -> BooleanNode.FALSE
            null      -> {
                // No explicit type: generate object if properties exist, else null
                if (!schema.properties.isNullOrEmpty()) generateByType(
                    schema.apply { type = "object" }, openApi, depth
                ) else NullNode.instance
            }
            else -> NullNode.instance
        }
}
```

#### 3.2 `JsonSchemaInferrer`

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/schema/JsonSchemaInferrer.kt`

```kotlin
// Copyright (C) 2026 Ritense BV, the Netherlands.
// ...

package com.ritense.iko.aggregateddataprofile.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Component

@Component
internal class JsonSchemaInferrer {

    private val mapper = ObjectMapper()

    /** Walks a JsonNode and returns a JSON Schema 2020-12 string. */
    fun infer(root: JsonNode): String {
        val schema = inferShape(root)
        schema.put("\$schema", "https://json-schema.org/draft/2020-12/schema")
        return mapper.writeValueAsString(schema)
    }

    private fun inferShape(node: JsonNode): ObjectNode {
        val schema = mapper.createObjectNode()
        return when {
            node.isObject -> {
                schema.put("type", "object")
                if (node.size() > 0) {
                    val props = mapper.createObjectNode()
                    node.fields().forEach { (key, value) ->
                        props.set<ObjectNode>(key, inferShape(value))
                    }
                    schema.set<ObjectNode>("properties", props)
                }
                schema
            }
            node.isArray -> {
                schema.put("type", "array")
                if (node.size() > 0) schema.set<ObjectNode>("items", inferShape(node.first()))
                schema
            }
            node.isTextual  -> schema.apply { put("type", "string") }
            node.isInt      -> schema.apply { put("type", "integer") }
            node.isNumber   -> schema.apply { put("type", "number") }
            node.isBoolean  -> schema.apply { put("type", "boolean") }
            else            -> schema.apply { put("type", "null") }
        }
    }
}
```

#### 3.3 `AdpSchemaService`

**File**: `src/main/kotlin/com/ritense/iko/aggregateddataprofile/schema/AdpSchemaService.kt`

The service mirrors the `buildRelationRoute` / ADP route builder logic but operates on in-memory
mock data instead of live Camel exchanges.

```kotlin
// Copyright (C) 2026 Ritense BV, the Netherlands.
// ...

package com.ritense.iko.aggregateddataprofile.schema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ritense.iko.aggregateddataprofile.domain.AggregatedDataProfile
import com.ritense.iko.aggregateddataprofile.domain.Relation
import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import com.ritense.iko.connectors.repository.ConnectorEndpointRepository
import com.ritense.iko.connectors.repository.ConnectorInstanceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import net.thisptr.jackson.jq.BuiltinFunctionLoader
import net.thisptr.jackson.jq.JsonQuery
import net.thisptr.jackson.jq.Scope
import net.thisptr.jackson.jq.Versions
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
internal class AdpSchemaService(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
    private val connectorInstanceRepository: ConnectorInstanceRepository,
    private val connectorEndpointRepository: ConnectorEndpointRepository,
    private val openApiMockGenerator: OpenApiMockGenerator,
    private val jsonSchemaInferrer: JsonSchemaInferrer,
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val mapper = ObjectMapper()
    }

    /**
     * Synchronous entry point — generates and persists the schema for the given ADP.
     * Called directly from the controller after saves. Exceptions propagate to the caller
     * so errors can be reflected in the HTTP response.
     */
    @Transactional
    fun generateAndSave(adpId: UUID) {
        val adp = aggregatedDataProfileRepository.findById(adpId)
            .orElseThrow { NoSuchElementException("ADP not found: $adpId") }
        val schema = generateSchema(adp)
        if (schema == null) {
            logger.info { "Schema generation skipped for ADP '${adp.name}' (${adp.id}): connector has no specificationUri" }
            return
        }
        adp.jsonschema = schema
        aggregatedDataProfileRepository.save(adp)
        logger.info { "Schema generated for ADP '${adp.name}' (${adp.id})" }
    }

    /**
     * Returns null when schema generation is not applicable for this ADP (i.e. one or more
     * connector instances in the ADP hierarchy do not have `specificationUri` in their config).
     * Callers should leave `jsonschema` as null in that case.
     *
     * Mirrors AggregatedDataProfileRouteBuilder:
     *   - Generates a mock from the ADP's connector OpenAPI spec
     *   - Recursively generates mocks for all relations (same PairAggregator / MapAggregator logic)
     *   - Executes the ADP's resultTransform JQ against the composed input
     *   - Infers a JSON Schema from the output
     */
    fun generateSchema(adp: AggregatedDataProfile): String? {
        if (!isSchemaGenerationSupported(adp)) return null
        val adpMock = generateConnectorMock(adp.connectorInstanceId, adp.connectorEndpointId)
        val composedInput = composeInput(adpMock, adp.level1Relations(), adp)
        val result = applyTransform(adp.resultTransform.expression, composedInput)
        return jsonSchemaInferrer.infer(result)
    }

    /**
     * Returns true when every ConnectorInstance referenced by the ADP and all its relations
     * has a `specificationUri` key in its config (i.e. uses camel-rest-openapi).
     * This is the single decision point for whether schema generation is applicable.
     */
    private fun isSchemaGenerationSupported(adp: AggregatedDataProfile): Boolean {
        val allInstanceIds = buildList {
            add(adp.connectorInstanceId)
            adp.relations.forEach { add(it.connectorInstanceId) }
        }
        return allInstanceIds.all { instanceId ->
            connectorInstanceRepository.findById(instanceId)
                .map { it.config.containsKey("specificationUri") }
                .orElse(false)
        }
    }

    /**
     * Recursively generates mock + applies resultTransform for a Relation node.
     * Mirrors buildRelationRoute(). Precondition: isSchemaGenerationSupported() returned true.
     */
    private fun generateRelationResult(relation: Relation): JsonNode {
        val connectorMock = generateConnectorMock(relation.connectorInstanceId, relation.connectorEndpointId)
        val children = relation.aggregatedDataProfile.relationsOf(relation.id)
        val composedInput = composeInput(connectorMock, children, relation.aggregatedDataProfile)
        return applyTransform(relation.resultTransform.expression, composedInput)
    }

    /**
     * Composes the resultTransform input for an entity (ADP or Relation).
     *
     * When there are no children: returns connectorMock directly (raw connector response).
     * When children exist: builds {left: connectorMock, right: {propName: childResult, ...}}
     *   mirroring PairAggregator + MapAggregator behaviour.
     *
     * Precondition: isSchemaGenerationSupported() returned true.
     *
     * @param connectorMock  The mock JSON from the entity's own connector
     * @param children       Direct child relations of this entity
     * @param adp            The owning ADP (needed to resolve further nested children)
     */
    private fun composeInput(
        connectorMock: JsonNode,
        children: List<Relation>,
        adp: AggregatedDataProfile,
    ): JsonNode {
        if (children.isEmpty()) return connectorMock

        val rightMap = mapper.createObjectNode()
        for (child in children) {
            val childResult = generateRelationResult(child)
            val isArrayMode = detectArrayMode(child.endpointTransform.expression, connectorMock)
            if (isArrayMode) {
                rightMap.set<ArrayNode>(child.propertyName, mapper.createArrayNode().add(childResult))
            } else {
                rightMap.set<JsonNode>(child.propertyName, childResult)
            }
        }

        return mapper.createObjectNode()
            .set<ObjectNode>("left", connectorMock).also {
                (it as ObjectNode).set<ObjectNode>("right", rightMap)
            }
    }

    /**
     * Precondition: isSchemaGenerationSupported() returned true, so specificationUri is present.
     */
    private fun generateConnectorMock(connectorInstanceId: UUID, connectorEndpointId: UUID): JsonNode {
        val instance = connectorInstanceRepository.findById(connectorInstanceId)
            .orElseThrow { NoSuchElementException("ConnectorInstance not found: $connectorInstanceId") }
        val endpoint = connectorEndpointRepository.findById(connectorEndpointId)
            .orElseThrow { NoSuchElementException("ConnectorEndpoint not found: $connectorEndpointId") }
        val specUri = checkNotNull(instance.config["specificationUri"]) {
            "specificationUri missing from ConnectorInstance ${instance.id}"
        }
        val openApi = openApiMockGenerator.loadSpec(specUri)
        return openApiMockGenerator.generateMock(openApi, endpoint.operation)
    }

    /**
     * Evaluates the endpointTransform JQ against a mock context with `source` = parentMock.
     * Returns true if the result is an ArrayNode (→ batch/array mode).
     * Mirrors the `when { ex -> ex.getVariable(...).isArray }` branch in the route builder.
     */
    private fun detectArrayMode(endpointTransformExpression: String, parentMock: JsonNode): Boolean {
        val mockContext = mapper.createObjectNode().apply {
            put("idParam", "")
            set<ObjectNode>("sortParams", mapper.createObjectNode())
            set<ObjectNode>("filterParams", mapper.createObjectNode())
            set<JsonNode>("source", parentMock)
        }
        return try {
            val result = applyTransform(endpointTransformExpression, mockContext)
            result.isArray
        } catch (e: Exception) {
            false // assume non-array when transform cannot be evaluated against mock context
        }
    }

    private fun applyTransform(expression: String, input: JsonNode): JsonNode {
        val scope = Scope.newEmptyScope()
        BuiltinFunctionLoader.getInstance().loadFunctions(Versions.JQ_1_6, scope)
        val query = JsonQuery.compile(expression, Versions.JQ_1_6)
        val out = mutableListOf<JsonNode>()
        query.apply(scope, input) { out.add(it) }
        return out.firstOrNull() ?: NullNode.instance
    }
}
```

### Success Criteria

#### Automated Verification
- [ ] `./gradlew compileKotlin` — all new classes compile
- [ ] Unit tests for `OpenApiMockGenerator` with the existing `pet-api.yaml` fixture:
  `./gradlew test --tests "*.OpenApiMockGeneratorTest"`
- [ ] Unit tests for `JsonSchemaInferrer`:
  `./gradlew test --tests "*.JsonSchemaInferrerTest"`
- [ ] Unit tests for `AdpSchemaService.generateSchema()` using `MockWebServer` for the spec URL:
  `./gradlew test --tests "*.AdpSchemaServiceTest"`
- [ ] `./gradlew spotlessApply && ./gradlew spotlessCheck`

#### Manual Verification
- [ ] (Deferred to Phase 4 integration) Schema column is populated after save.

---

## Phase 4: Synchronous Trigger Integration

### Overview
Call `AdpSchemaService.generateAndSave()` directly from the controller after ADP edits that change
the `resultTransform`. Because the call is synchronous, any errors (unreachable spec URL, parse
failures, JQ errors) propagate to the controller and can be reflected in the HTTP response.
Relation creates/edits/deletes do **not** trigger schema regeneration.

### Changes Required

#### 4.1 `AggregatedDataProfileController` — inject service and call after saves

**File**: `src/main/kotlin/com/ritense/iko/mvc/controller/AggregatedDataProfileController.kt`

Add `AdpSchemaService` to the constructor parameter list:

```kotlin
internal class AggregatedDataProfileController(
    ...existing parameters...,
    private val adpSchemaService: AdpSchemaService,
)
```

Capture the previous `resultTransform` before calling `handle()`, then only regenerate when it
actually changed:

```kotlin
val previousResultTransform = aggregatedDataProfile.resultTransform.expression
aggregatedDataProfile.handle(form)
aggregatedDataProfileRepository.save(aggregatedDataProfile)
// ... existing reloadRoute logic ...
if (aggregatedDataProfile.resultTransform.expression != previousResultTransform) {
    adpSchemaService.generateAndSave(aggregatedDataProfile.id)
}
```

> **Note**: Schema generation is **not** triggered after `createRelation()`, `editRelation()`,
> or `deleteRelation()`. Only `resultTransform` changes affect the schema.
>
> Schema generation runs synchronously within the same request. If the OpenAPI spec is unreachable
> or a JQ expression fails, the exception propagates to the controller's error handling.

#### 4.2 Admin "Regenerate Schema" endpoint

Add to `AggregatedDataProfileController`:

```kotlin
@PostMapping("/aggregated-data-profiles/{id}/schema/regenerate")
fun regenerateSchema(
    @PathVariable id: UUID,
    @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false,
): ModelAndView {
    adpSchemaService.generateAndSave(id)
    return details(id, isHxRequest)
}
```

### Success Criteria

#### Automated Verification
- [ ] `./gradlew compileKotlin`
- [ ] `./gradlew test` — existing tests still pass
- [ ] `./gradlew spotlessApply && ./gradlew spotlessCheck`

#### Manual Verification
- [ ] Edit an ADP's `resultTransform`, save. The detail page immediately shows the generated
  schema (check via admin "Schema" tab once Phase 6 is done, or verify in the DB directly).
- [ ] Edit an ADP without changing `resultTransform` (e.g. change roles). Confirm schema is
  **not** regenerated (stays the same, no extra latency).
- [ ] Add, edit, or delete a relation. Confirm schema is **not** regenerated.
- [ ] Save an ADP whose `specificationUri` is unreachable. Confirm the error is reflected to
  the caller (e.g. error notification in the admin UI).

**Implementation Note**: After completing this phase and all automated verification passes,
confirm manually that the synchronous trigger works before proceeding to Phase 5.

---

## Phase 5: Consumer API Endpoint

### Overview
Expose the stored schema via a new Spring MVC `@RestController` under the existing JWT-secured
`/aggregated-data-profiles/**` path.

### Changes Required

#### 5.1 `AggregatedDataProfileSchemaController`

**File**: `src/main/kotlin/com/ritense/iko/mvc/controller/AggregatedDataProfileSchemaController.kt`

```kotlin
// Copyright (C) 2026 Ritense BV, the Netherlands.
// ...

package com.ritense.iko.mvc.controller

import com.ritense.iko.aggregateddataprofile.repository.AggregatedDataProfileRepository
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/aggregated-data-profiles")
internal class AggregatedDataProfileSchemaController(
    private val aggregatedDataProfileRepository: AggregatedDataProfileRepository,
) {

    @GetMapping("/{name}/schema")
    @Transactional(readOnly = true)
    fun getSchema(@PathVariable name: String): ResponseEntity<String> {
        val adp = aggregatedDataProfileRepository.findByNameAndIsActiveTrue(name)
            ?: return ResponseEntity.notFound().build()
        val schema = adp.jsonschema
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(schema)
    }
}
```

> **Security**: `/aggregated-data-profiles/{name}/schema` is a 3-segment path. It falls under
> the API JWT filter chain (pattern `/aggregated-data-profiles/**`). No security config changes
> are needed.

### Success Criteria

#### Automated Verification
- [ ] `./gradlew compileKotlin`
- [ ] Integration test: `GET /aggregated-data-profiles/{name}/schema` with a valid JWT returns
  the schema when one exists, 404 otherwise.
  `./gradlew integrationTest --tests "*.AggregatedDataProfileSchemaControllerIT"`
- [ ] `./gradlew spotlessApply && ./gradlew spotlessCheck`

#### Manual Verification
- [ ] `curl -H "Authorization: Bearer <token>" http://localhost:8080/aggregated-data-profiles/{name}/schema`
  returns a JSON Schema object for a profile that has been saved with a reachable spec URL.
- [ ] Same call returns 404 for an unknown profile name.
- [ ] Same call returns 404 for a profile whose schema is null (not yet generated).
- [ ] The endpoint requires a valid JWT — unauthenticated request returns 401.

**Implementation Note**: Pause here for manual confirmation before proceeding to Phase 6.

---

## Phase 6: Admin UI — Schema Tab

### Overview
Add a "Schema" tab to the ADP detail page with a read-only Monaco editor and a "Regenerate Schema"
button.

### Changes Required

#### 6.1 New Thymeleaf fragment: `schema-panel.html`

**File**: `src/main/resources/templates/fragments/internal/aggregated-data-profile/schema-panel.html`

```html
<!--
 Copyright (C) 2026 Ritense BV, the Netherlands.
 ...
 -->
<div th:fragment="schema-panel" id="panel-schema">
    <cds-tile>
        <span class="tile-header">JSON Schema</span>
        <cds-content>
            <div th:if="${aggregatedDataProfile.jsonschema != null}">
                <!-- Read-only Monaco editor for schema display -->
                <textarea id="schemaContent" hidden th:text="${aggregatedDataProfile.jsonschema}"></textarea>
                <div
                    id="schema-editor"
                    data-monaco
                    data-language="json"
                    data-textarea="#schemaContent"
                    th:data-initial="${aggregatedDataProfile.jsonschema}"
                    data-theme="lightgray-theme"
                    data-readonly="true"
                    class="monaco-editor-style"
                ></div>
            </div>
            <cds-inline-notification
                th:if="${aggregatedDataProfile.jsonschema == null}"
                kind="info"
                title="Schema not yet generated"
                subtitle="Save the profile or click Regenerate to generate the schema."
                hide-close-button
            ></cds-inline-notification>
            <cds-button
                kind="secondary"
                size="sm"
                th:attr="hx-post=@{'/admin/aggregated-data-profiles/' + ${aggregatedDataProfile.id} + '/schema/regenerate'}"
                hx-target="#panel-schema"
                hx-swap="outerHTML"
                style="margin-top: var(--cds-spacing-5)"
            >
                Regenerate Schema
            </cds-button>
        </cds-content>
    </cds-tile>
</div>
```

> **Note**: The `data-readonly="true"` attribute requires a corresponding check in
> `js/monaco-init.js` to set `readOnly: true` on the Monaco editor options when this attribute
> is present. Add this handling to the existing Monaco initialisation logic.

#### 6.2 Add "Schema" tab to `detail-page.html`

**File**: `src/main/resources/templates/fragments/internal/aggregated-data-profile/detail-page.html`

Add a new `<cds-tab>` after the existing "Preview" tab:

```html
<cds-tab
    id="tab-schema"
    target="panel-schema"
    value="schema"
    >Schema</cds-tab
>
```

Add the corresponding panel after `#panel-preview`:

```html
<div
    id="panel-schema"
    role="tabpanel"
    aria-labelledby="tab-schema"
>
    <div
        th:replace="~{fragments/internal/aggregated-data-profile/schema-panel :: schema-panel}"
    ></div>
</div>
```

#### 6.3 Pass `aggregatedDataProfile` to model in `details()` controller method

Verify (or ensure) that `aggregatedDataProfile` is already included in the `ModelAndView` model
for the detail page — it is, as the existing "General" panel already reads `${aggregatedDataProfile.jsonschema}`
will be available without further controller changes.

#### 6.4 Read-only Monaco support in `monaco-init.js`

**File**: `src/main/resources/static/js/monaco-init.js`

In the Monaco editor initialisation loop (where `data-monaco` elements are processed), add
read-only support:

```js
const isReadOnly = el.dataset.readonly === 'true';
const editor = monaco.editor.create(el, {
    value: el.dataset.initial || '',
    language: el.dataset.language || 'plaintext',
    theme: el.dataset.theme || 'vs',
    readOnly: isReadOnly,
    // ... existing options
});
```

### Success Criteria

#### Automated Verification
- [ ] `./gradlew compileKotlin` — no Kotlin errors
- [ ] `./gradlew spotlessApply && ./gradlew spotlessCheck`

#### Manual Verification
- [ ] Navigate to an ADP detail page. A "Schema" tab is visible.
- [ ] Before schema generation: the "Schema not yet generated" notification is shown; the
  "Regenerate Schema" button is present.
- [ ] Click "Regenerate Schema". The panel refreshes and shows the generated JSON Schema in
  the read-only Monaco editor (JSON syntax highlighting, not editable).
- [ ] After editing transforms and saving: the Schema tab immediately shows the newly generated
  schema (no delay, no refresh needed).
- [ ] The Monaco editor on the Schema tab cannot be edited (read-only).

---

## Testing Strategy

### Unit Tests

| Class | Test file | Key scenarios |
|---|---|---|
| `OpenApiMockGenerator` | `OpenApiMockGeneratorTest.kt` | Load `classpath:pet-api.yaml`, find operation, verify all properties present; handle missing operation (returns NullNode); handle `$ref`; handle `allOf` |
| `JsonSchemaInferrer` | `JsonSchemaInferrerTest.kt` | Object node → `{type:object, properties:{...}}`; array → `{type:array, items:...}`; primitives; nested objects |
| `AdpSchemaService` | `AdpSchemaServiceTest.kt` | No relations: resultTransform applied to connector mock; with relations: `{left,right}` composed; detect array mode; generation failure throws exception |

### Integration Tests

| Test | File | Scenario |
|---|---|---|
| `AggregatedDataProfileSchemaControllerIT` | — | GET schema returns 404 when null; returns schema JSON with 200 after generation; requires JWT |

### Manual Testing Steps

1. Start the stack: `docker compose up -d && ./gradlew bootRun`
2. Create a connector instance pointing to a running system with a valid `specificationUri`.
3. Create an ADP using that connector instance, with a non-trivial `resultTransform` (e.g. `{id: .id, name: .name}`).
4. Save the ADP. The detail page immediately shows the generated schema under the "Schema" tab.
5. Edit the `resultTransform`. Save. The schema is immediately regenerated.
6. `curl -H "Authorization: Bearer <token>" http://localhost:8080/aggregated-data-profiles/{name}/schema` — confirms the consumer endpoint works.
7. Test with a `specificationUri` pointing to an unreachable URL — confirm error is logged and the app continues working.

---

## Performance Considerations

- Schema generation runs synchronously within the save request. This adds latency to saves
  (mainly from fetching the OpenAPI spec over HTTP). For most specs this is sub-second, but
  unreachable or slow spec URLs will cause the save request to block until timeout. This is
  intentional — errors must propagate to the caller.
- If spec-fetch latency becomes a problem in practice, OpenAPI spec caching can be added later
  without changing the synchronous contract.
- JQ execution uses `Scope.newEmptyScope()` + `BuiltinFunctionLoader` per call; these are not
  thread-safe but are constructed fresh each invocation as per the existing `JQTest.kt` pattern.

---

## Migration Notes

- The migration adds a nullable `jsonschema` column. All existing rows will have `NULL` after
  migration — no data backfill needed.
- After deployment, schemas will be generated the next time each active ADP is edited and saved,
  or when an admin clicks "Regenerate Schema". A one-time bulk generation is not included in scope.

---

## References

- Research: `thoughts/shared/research/2026-02-20-adp-json-schema-generation.md`
- JQ execution pattern: `src/test/kotlin/com/ritense/iko/JQTest.kt:31-62`
- Route builder (PairAggregator/MapAggregator recursion):
  `src/main/kotlin/com/ritense/iko/aggregateddataprofile/camel/AggregatedDataProfileRouteBuilder.kt:92-100,253-258`
- Array-mode detection: `AggregatedDataProfileRouteBuilder.kt:179-188`
- Controller save flows: `AggregatedDataProfileController.kt:286-318` (edit), `361-368` (createRelation)
- Flyway convention: latest migration `V2026.01.29.1__add_versioning.sql`
- Security filter chains: `SecurityConfig.kt:67-90`

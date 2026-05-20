# OpenAPI specifications

Self-contained OpenAPI documents committed to the repository so that connector
instances can reference them at runtime via `file:` URIs.

This directory is mounted read-only into the `iko-application` container at
`/openapi-specs` (see `docker-compose.yaml`). A connector instance can then set
its `apiSpecificationUrl` to `file:/openapi-specs/<filename>.yaml`.

## Files

### `haalcentraal-brp-personen.yaml`

- Source: <https://raw.githubusercontent.com/BRP-API/Haal-Centraal-BRP-bevragen/refs/heads/master/specificatie/resolved/openapi.yaml>
- Upstream project: <https://github.com/BRP-API/Haal-Centraal-BRP-bevragen>
- Variant: `specificatie/resolved/openapi.yaml` — the pre-bundled single-file
  build published by the upstream project. The unresolved
  `specificatie/openapi.yaml` is a `$ref` tree and cannot be loaded
  standalone.
- Downloaded on: 2026-05-20

## Updating a spec

Replace the file in place from the upstream source URL listed above and commit
the result. Record the new download date in this README.

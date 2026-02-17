# Aggregated Data Profiles (ADP)

An Aggregated Data Profile defines how data from one or more external systems is fetched, combined, and transformed into a single response.

## Properties

| Property | Description |
|---|---|
| **Name** | Unique identifier for the profile. Used in API paths (`/aggregated-data-profiles/{name}`). |
| **Connector Instance** | The connector instance to use for the primary data source. |
| **Connector Endpoint** | The endpoint to call on the primary connector. |
| **Endpoint Transform** | Optional JQ expression to transform the request context before calling the primary endpoint. Maps incoming request parameters to endpoint parameters. |
| **Result Transform** | Optional JQ expression to transform the final aggregated result before returning it to the caller. |
| **Roles** | Comma-separated list of roles required to access this profile via the API (e.g., `ROLE_USER,ROLE_ADMIN`). |
| **Cache Settings** | Whether caching is enabled and TTL in milliseconds. |

## Relations

Relations are additional data sources that enrich the profile's primary data. Each relation calls a separate endpoint and adds its result to the response under a named property.

Relations can be nested: a relation can have its own child relations, forming a tree structure. See [relaties.md](../relaties.md) for how the response structure works.

### Relation Properties

| Property | Description |
|---|---|
| **Property Name** | The key under which the relation's result appears in the output JSON. |
| **Connector Instance** | Connector instance for this relation's data source. |
| **Connector Endpoint** | Endpoint to call. |
| **Endpoint Transform** | JQ expression mapping the parent response to endpoint parameters (source endpoint mapping). |
| **Result Transform** | JQ expression to transform the relation's result. |
| **Cache Settings** | Per-relation cache configuration. |

### Source endpoint mapping

A source endpoint mapping is a `jq` expression that maps the previous call to an endpoint to new endpoint parameters. So for example if we have a call to `BRP` and we get a person's information back, and if we want to do a call to `BAG` we need to map the `verblijfplaats.verblijfadres.postcode` and `verblijfplaats.verblijfadres.huisnummer` from `BRP` to the parameters `huisnummer` and `postcode` of `BAG`. We can do this by creating a `source endpoint mapping` `jq` expression below:

```
{
    postcode: .verblijfplaats.verblijfadres.postcode,
    huisnummer: .verblijfplaats.verblijfadres.huisnummer
}
```

### Array of source endpoint mappings

You can also create an array in the `source endpoint mapping` with help of the `jq` expression. IKO will then run the relation for each element in the array of the `source endpoint mapping`. The below example will cause the relation to be executed 3 times, once for each element in the array, and the relation will thus also return an array of results.

```
[
    {
        postcode: .verblijfplaats.verblijfadres.postcode,
        huisnummer: .verblijfplaats.verblijfadres.huisnummer
    },
    {
        postcode: .verblijfplaats.verblijfadres.postcode,
        huisnummer: .verblijfplaats.verblijfadres.huisnummer
    },
    {
        postcode: .verblijfplaats.verblijfadres.postcode,
        huisnummer: .verblijfplaats.verblijfadres.huisnummer
    }
]
```

## Execution Flow

When a profile is requested via `GET /aggregated-data-profiles/{name}?id={externalId}`:

1. The JWT token is validated and roles are checked against the profile's required roles.
2. The primary endpoint is called via its Camel route.
3. The endpoint response is optionally transformed via the endpoint transform (JQ).
4. For each top-level relation:
   a. Parent data is mapped to endpoint parameters via the relation's endpoint transform.
   b. The relation's endpoint is called.
   c. The relation's result is optionally transformed.
   d. If the endpoint transform produces an array, the relation executes once per element (batch mode).
   e. Child relations are processed recursively.
5. Parent and relation data are combined into `left`/`right` structure (see [relaties.md](../relaties.md)).
6. The final result is optionally transformed via the profile's result transform.
7. The result is cached in Redis if caching is enabled.

## Debug / Testing

Profiles can be tested via the admin UI debug panel (`POST /admin/aggregated-data-profiles/debug`). This enables Camel's BacklogTracer and returns both the JSON result and a trace of all Camel route events, which is useful for diagnosing transformation and routing issues.

## Managing Profiles

Profiles are managed through the admin UI at `/admin/aggregated-data-profiles`. The UI provides:

- List view with pagination and search/filter
- Create/edit forms for profile properties
- Relation management (add, edit, delete with cascade)
- Cache eviction buttons
- Debug panel with trace output
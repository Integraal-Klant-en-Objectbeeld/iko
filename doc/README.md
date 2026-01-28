# Documentation

## Architecture & Design
- [Architecture Overview](./architecture.md) -- High-level system architecture, layers, and data flow
- [Domain Model](./domain-model.md) -- Entity descriptions, relationships, and database tables

## Core Concepts
- [Aggregated Data Profiles (ADP)](./profiles/README.md) -- Profile configuration, properties, and execution flow
- [Connectors](./connectors/README.md) -- Connector templates, instances, endpoints, and YAML routes
- [Relaties](./relaties.md) -- How relations combine data into left/right response structure

## Technical Reference
- [API Endpoints](./api-endpoints.md) -- Complete HTTP endpoint reference (admin UI + public API + actuator)
- [Security](./security.md) -- Authentication, authorization, filter chains, and encryption at rest
- [Caching](./caching.md) -- Redis caching layer, configuration, and eviction

## Connector Reference
- [BAG](./connectors/bag.md) -- Basisregistratie Adressen en Gebouwen
- [BRP](./connectors/haalcentraal-brp.md) -- Haal Centraal BRP (persons)
- [ObjectenAPI](./connectors/objectenapi.md) -- Objects API
- [OpenDocumenten](./connectors/opendocumenten.md) -- Document management
- [OpenKlant](./connectors/openklant.md) -- Customer contacts
- [OpenZaak](./connectors/openzaak.md) -- Case management
- [Demo](./connectors/demo.md) -- Demo connector with mock data

## Endpoint Reference
- [Personen](./endpoints/personen.md) -- Person lookup endpoints
- [Zaken](./endpoints/zaken.md) -- Case lookup endpoints

## Data Sources
- [BRP](./sources/brp.md) -- BRP primary data source
- [OpenZaak](./sources/openzaak.md) -- OpenZaak primary data source

## Other
- [Release Notes](./release-notes.md)
# Documentation

Some documentation about sources and searches.

- A **primary source** is a source where a **primary search** can be executed upon. 
- A **primary search** is a search that only returns a single entity and is addressable by an **identification**. And is 
    considered the entrypoint of any profile within IKO.
- An **identification** is a single value.

## Sources

- [BRP](./sources/brp.md)
- [OpenZaak](./sources/openzaak.md)

## Searches

Searches are what allows you to retrieve information from a source. They are predefined ways of accessing that data. Searches 
in this documentation are written as `<searchId>(<searchParameters>)`. To use a search in a relation just set the Search ID 
as the respective `searchId` and map the parameters.

- [Personen](./searches/personen.md)
- [Zaken](./searches/zaken.md)

## Add custom source and search with YAML

TODO

## Relations 

## JQ

## Authentication / OIDC

# 2025-10-DD - Version 0.4.x

## Breaking changes

### Added `property name` to `relations`

Results of relations are now no longer joined together on the same depth, but instead are added to a object with propert name as key. This causes relations data to be one property deeper then they currently are.

If your relation currently has a transform that looks like `{ "name": . }`. You can change the property name in the relation edit menu to `name` and change the transform to `.`.

### Changed `Endpoint parameter mapping` to a jq expression

Endpoint parameter mapping in a relation is now a jq expression instead of a map of key value pairs, where the value is a jq expression. This means that the `{ "a": ".b" }` mapping is now written as `{ a: .b }`. You will need to update your existing relations.

## Changes


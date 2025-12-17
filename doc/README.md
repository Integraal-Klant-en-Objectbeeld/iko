# Documentation

- [Release notes](./release-notes.md)

## General
- [Aggregated Data Profiles (ADP)](./profiles/README.md)
- [Connectors](./connectors/README.md)

## Technical
- [Relaties](./relaties.md)
- [Security](./security.md)

### Open Telemetry

IKO supports open telemetry. Add the folowing environment variables to enable it.

```
JAVA_TOOL_OPTIONS=-javaagent:/app/opentelemetry-javaagent.jar
OTEL_SERVICE_NAME=iko
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=none
OTEL_LOGS_EXPORTER=none
... Other ENVS
```

# OpenKlant Connector Module

This module provides integration with the OpenKlant 2.0 API for the IKO project. It has been implemented as an isolated module to improve maintainability and separation of concerns.

## Configuration

The connector can be configured in the application.yml file:

```yaml
iko:
  connectors:
    openklant:
      enabled: false  # Set to true to enable the connector
      host: # Set via environment variable
      specificationUri: "https://raw.githubusercontent.com/maykinmedia/open-klant/2.10.0/src/openklant/components/klantinteracties/openapi.yaml"
      token: # Set via environment variable
```

## Testing the Connector

To test the connector:

1. Set the required environment variables:
   ```
   IKO_CONNECTORS_OPENKLANT_ENABLED=true
   IKO_CONNECTORS_OPENKLANT_HOST=<OpenKlant API host URL>
   IKO_CONNECTORS_OPENKLANT_TOKEN=<Authentication token>
   ```

2. Start the application:
   ```
   ./gradlew bootRun
   ```

3. Test the endpoints:
   - List actoren: `GET /endpoints/openklant/actoren`
   - Get actor by ID: `GET /endpoints/openklant/actoren/{id}`
   - List klantcontacten: `GET /endpoints/openklant/klantcontacten`
   - Get klantcontact by ID: `GET /endpoints/openklant/klantcontacten/{id}`
   - And other endpoints as defined in the OpenKlantPublicEndpoints class

## Troubleshooting

If the connector is not working:

1. Verify that the connector is enabled in the configuration
2. Check that the host URL and token are correctly set
3. Look for errors in the application logs
4. Verify that the OpenAPI specification URL is accessible

## Development

To extend the connector:

1. Add new endpoint classes in the `endpoints` package
2. Register the endpoints in the `OpenKlantConfig` class
3. Expose the endpoints in the `OpenKlantPublicEndpoints` class
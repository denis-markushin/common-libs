# Security starter

Provides a basic Spring Security configuration for services.
The module exposes an autoconfiguration with common settings for Swagger, Actuator and GraphQL endpoints.
In non-production profiles it also enables a filter that reads user
roles from the `X-Roles` header.

## Usage

1. Add dependency:
   ```kotlin
   implementation("io.github.denis-markushin:security-starter:x.x.x")
   ```
2. Send requests with the `X-Roles` header during local development:
   ```
   X-Roles: admin,user
   ```
   The filter will populate the security context with the provided authorities.

# Security starter

Spring Security auto-configuration shared across services. It owns a single
`SecurityFilterChain` with sensible defaults (CSRF disabled, stateless sessions,
Swagger / Actuator / GraphQL / internal endpoints public, everything else
authenticated) and offers pluggable authentication mechanisms.

## What you get

- **Base filter chain** (`BaseSecurityAutoConfiguration`) — CSRF off, `STATELESS`
  sessions, `permitAll` for `/swagger-ui/**`, `/v3/api-docs/**`,
  `/swagger-ui.html`, `/actuator/**`, `/graphql`, `/internal/**`, and
  `authenticated` for any other request. `@EnableMethodSecurity` is on.
- **JWT authentication** (`JwtAutoConfiguration`) — activates automatically when a
  `JwtDecoder` is present (i.e. when the application configures
  `spring.security.oauth2.resourceserver.jwt.*`). Extracts authorities from a
  configurable claim and renders auth failures as JSON.
- **X-Roles header auth** (`XRolesAutoConfiguration`) — non-production only; reads
  roles from the `X-Roles` header for local development.

## Usage

Add the dependency:
```kotlin
implementation("io.github.denis-markushin:security-starter:x.x.x")
```
The base chain and X-Roles filter work out of the box. No extra configuration is
required for local development.

### Enabling JWT authentication

JWT auth turns on as soon as the standard resource-server decoder is configured —
there is no custom flag. Point the resource server at your issuer:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak/realms/<realm>   # or jwk-set-uri / public-key-location
```
When a `JwtDecoder` bean exists, the starter wires `oauth2ResourceServer` with:

- a converter that reads roles from the configured claim, trims them, and
  upper-cases them into authorities;
- a JSON authentication entry point (see below).

#### Roles claim

Roles are read from a single claim, flattened recursively (any string leaf under
the claim becomes an authority). Default claim is `realm_access`, which resolves
Keycloak's `realm_access -> { roles: [...] }` shape:
```yaml
dema:
  security:
    jwt:
      roles-claim: realm_access   # default
```

#### Authentication entry point

Authentication failures (invalid / expired bearer token) are rendered as JSON
instead of an empty `401`:

- requests whose URI ends with `/graphql` → HTTP `200` with a GraphQL-shaped error
  body `{"errors":[{"message":"Unauthorized","extensions":{"code":"UNAUTHENTICATED"}}]}`,
  so a federation gateway can parse the body instead of choking on an empty 401;
- any other request → HTTP `401` with `{"message":"Unauthorized"}`.

Both responses use `application/json`.

### X-Roles header (local development)

In non-production profiles, send roles via a header instead of a token:
```
X-Roles: admin,user
```
The filter populates the security context with the provided authorities.

## Extending the chain

Each authentication mechanism contributes one `HttpSecurityCustomizer` bean:
```kotlin
fun interface HttpSecurityCustomizer {
    fun customize(http: HttpSecurity)
}
```
`BaseSecurityAutoConfiguration` injects all `HttpSecurityCustomizer` beans and
applies them to the shared `HttpSecurity` after the base rules and before the
chain is built, so an implementation may use either the raw Java API or the
Kotlin `http { }` DSL. Provide your own bean to add a custom authentication
mechanism; the built-in `JwtAuthCustomizer` is one such implementation.

To override a built-in piece, declare your own bean — the converter, the entry
point, and the JWT customizer are all `@ConditionalOnMissingBean`.

## Configuration reference

| Property                     | Default        | Description                                  |
|------------------------------|----------------|----------------------------------------------|
| `dema.security.jwt.roles-claim` | `realm_access` | JWT claim holding user roles (flattened).    |

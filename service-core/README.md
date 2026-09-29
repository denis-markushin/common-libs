# Service core

Spring Boot defaults shared across services: Jetty, validation, Jackson
Kotlin, SpringDoc, Actuator, and HTTP exchange logging with Logbook.

## HTTP exchange logging

Logbook is on by default for requests under `/api/**` and writes JSON at
`TRACE` (service-core raises `org.zalando.logbook` to `TRACE` for you). The
defaults live in `logbook.properties`; any `logbook.*` property you set wins.

Only metadata is logged: method, URI, status, headers, and duration. Bodies
are left out (`logbook.strategy=without-body`), since a payment or fiscal
callback under `/api` carries data that must not reach the log.

> **Behavior change.** Earlier versions logged every request and response
> body under `/api/**` (`logbook.strategy=body-only-if-status-at-least` with
> `logbook.minimum-status=200`). A service that relies on body logs must opt
> in as shown below.

### Logging bodies

To log bodies for every logged path, restore the previous strategy and
narrow the logged paths with predicates:
```yaml
logbook:
  strategy: body-only-if-status-at-least
  predicate:
    exclude:
      - path: /api/payments/**
```
`logbook.minimum-status` (default here `200`) sets the lowest status whose
bodies are logged. An excluded path is not logged at all.

To log bodies only for chosen paths while the rest keep metadata only,
declare your own `Strategy` bean; it replaces the property-driven one:
```kotlin
@Bean
fun logbookStrategy(): Strategy = object : Strategy {
    private val body = requestTo<HttpRequest>("/api/orders/**")

    override fun process(request: HttpRequest): HttpRequest =
        if (body.test(request)) request.withBody() else request.withoutBody()

    override fun process(request: HttpRequest, response: HttpResponse): HttpResponse =
        if (body.test(request)) response.withBody() else response.withoutBody()
}
```
`requestTo` comes from `org.zalando.logbook.core.Conditions`.

Set `logbook.enabled=false` to turn exchange logging off entirely.

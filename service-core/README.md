# Service core

- [HTTP exchange logging](#http-exchange-logging)
  - [Logging bodies](#logging-bodies)
- [Actuator](#actuator)
  - [Exposing more endpoints](#exposing-more-endpoints)
- [Time zone](#time-zone)
- [Defaults](#defaults)

Spring Boot defaults shared across services: Jetty, validation, Jackson
Kotlin, SpringDoc, Actuator, HTTP exchange logging with Logbook, and a UTC
JVM default time zone.

## HTTP exchange logging

Logbook is on by default for requests under `/api/**` and writes JSON at
`TRACE` (service-core raises `org.zalando.logbook` to `TRACE` for you). The
defaults live in `org/dema/servicecore/logbook.properties`; any `logbook.*`
property you set wins (see [Defaults](#defaults)).

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

## Actuator

Only `health` and `prometheus` are exposed over HTTP. They are what the
platform relies on: health checks call `/actuator/health`, and Prometheus
scrapes `/actuator/prometheus`.

service-core brings the Prometheus registry (`micrometer-registry-prometheus`),
so `/actuator/prometheus` serves every Micrometer meter of the service. Set
`management.prometheus.metrics.export.enabled=false` to stop the export.

Every meter carries an `application` tag with the service's
`spring.application.name` (`unknown` when the service sets none), so
dashboards and alerts tell services apart.

Every other endpoint stays off by default. `env`, `configprops` and
`threaddump` reveal configuration, a heap dump holds every secret the
application has loaded, and `loggers` accepts writes that change log levels at
runtime. With `security-starter` on the classpath, the health and Prometheus
paths are the only actuator paths served without authentication.

> **Behavior change.** Earlier versions exposed every endpoint
> (`management.endpoints.web.exposure.include=*`). A service that calls
> anything other than `health` or `prometheus` must opt in as shown below.

### Exposing more endpoints

List the full set; the property replaces the default, so keep `health` and
`prometheus` in it:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info
```
An endpoint exposed this way still requires authentication under
`security-starter`; add it to `dema.security.permit-all` to make it public.
`heapdump` also has default access `none` in Spring Boot 4, so it needs
`management.endpoint.heapdump.access` as well.

## Time zone

The JVM default time zone is set to UTC when the application starts, on
`ApplicationStartingEvent`, before the environment, logging or any bean is
set up. This covers `main`, `bootRun`, containers and `@SpringBootTest`.

Columns are `timestamptz` and code works with `LocalDateTime` through jOOQ
converters; both the converters and the pgjdbc session time zone follow the
JVM default, so any other zone silently shifts stored times. There is no
property to turn this off.

> **Behavior change.** Earlier versions left the JVM default untouched. A
> service that set `TimeZone.setDefault` itself can drop that call.

## Defaults

service-core's defaults live next to its classes, in
`org/dema/servicecore/actuator.properties`, `service-core.properties` and
`logbook.properties`. An `EnvironmentPostProcessor` adds them before any
configuration class is parsed, so every condition sees them, at the lowest
precedence: any property the service sets in `application.yml`, the config
server, environment variables or test properties wins. Values passed to
`SpringApplication.setDefaultProperties` and files a service loads with
`@PropertySource` rank below them.

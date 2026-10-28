# Common Libs

[![Build](https://github.com/denis-markushin/common-libs/actions/workflows/gradle-ci.yml/badge.svg)](https://github.com/denis-markushin/common-libs/actions/workflows/gradle-ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.denis-markushin/bom)](https://central.sonatype.com/artifact/io.github.denis-markushin/bom)
[![License](https://img.shields.io/github/license/denis-markushin/common-libs)](LICENSE)
[![JDK](https://img.shields.io/badge/JDK-21%2B-blue)](#requirements)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)](#requirements)

- [Modules](#modules)
- [Quick start](#quick-start)
- [Requirements](#requirements)
- [Cutting a release](#cutting-a-release)
- [License](#license)

Spring Boot starters and Kotlin libraries shared across microservices, published to
[Maven Central](https://central.sonatype.com/artifact/io.github.denis-markushin/bom) under the
`io.github.denis-markushin` group.

## Modules

| Artifact                                                                 | Description                                                                            |
|--------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| `bom`                                                                    | Aligns Spring Boot, Spring Cloud, Netflix DGS and third-party library versions         |
| [`catalog`](catalog/README.md)                                           | Gradle version catalog of every module here, at one release version                    |
| [`service-core`](service-core/README.md)                                 | Spring Boot defaults: Jetty, Jackson Kotlin, SpringDoc, Actuator, Logbook, UTC zone    |
| [`security-starter`](security-starter/README.md)                         | Single `SecurityFilterChain` with JWT and pluggable authentication                     |
| [`jooq-starter`](jooq-starter/README.md)                                 | `AbstractRepository` with CRUD and upsert, pagination, timestamp listener              |
| [`graphql-dgs-starter`](graphql-dgs-starter/README.md)                   | Netflix DGS on top of jOOQ: Relay cursor pagination and sorting                        |
| [`common-scalars-starter`](common-scalars-starter/README.md)             | GraphQL scalars for `LocalDateTime`, Zulu time and Base64                              |
| [`outbox-starter`](outbox-starter/README.md)                             | Transactional outbox from a PostgreSQL table to Kafka                                  |
| [`minio-starter`](minio-starter/README.md)                               | Object storage over one MinIO bucket, wired from configuration                         |
| [`jooq-liquibase-testcontainer`](jooq-liquibase-testcontainer/README.md) | Testcontainers JDBC provider that applies Liquibase migrations to a PostgreSQL container before jOOQ codegen |
| [`test-common`](test-common/README.md)                                   | assertk extensions plus Spring and Testcontainers test scaffolding                     |

## Quick start

Every module lives on Maven Central. Import `bom` for the third-party versions the modules were built against,
then add the modules you need:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.denis-markushin:bom:2.9.2"))
    implementation("io.github.denis-markushin:service-core:2.9.2")
    implementation("io.github.denis-markushin:jooq-starter:2.9.2")
    testImplementation("io.github.denis-markushin:test-common:2.9.2")
}
```

To keep the modules themselves on one version, import the [version catalog](catalog/README.md) instead of repeating it.
Each module's README lists what it configures and which properties it exposes.

## Requirements

- Java 21+
- Spring Boot 4.x
- Kotlin is recommended; the libraries are written in it and use Kotlin-friendly APIs

## Cutting a release

Releases are cut from the GitHub **Actions** tab with the manual **Release** workflow, which asks for a release type:

- **MAJOR** / **MINOR** — run from the `main` branch.
- **PATCH** — run from a `release/*` branch.

The workflow runs the vercraft `makeRelease` task, which creates the release branch and tag and pushes them. The tag push
triggers `publish.yml`, which signs the modules and publishes them to Maven Central.

## License

[Apache License 2.0](LICENSE)

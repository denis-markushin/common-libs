# jooq-liquibase-testcontainer

- [Usage](#usage)
- [How it works](#how-it-works)
- [Limits](#limits)

A Testcontainers JDBC provider for jOOQ code generation: `jdbc:tc:liquibasepostgres:<tag>:///<db>?changelog=<path>`
starts a throwaway `postgres:<tag>` container, applies the Liquibase changelog to it and hands the
connection to the generator. No jOOQ dependency, so it works with whatever jOOQ version runs the
generator.

## Usage

1. Apply the official jOOQ plugin:
   ```kotlin
   plugins {
       id("org.jooq.jooq-codegen-gradle")
   }
   ```
2. Add the library to the generator classpath:
   ```kotlin
   dependencies {
       jooqCodegen("io.github.denis-markushin:jooq-liquibase-testcontainer:x.x.x")
   }
   ```
3. Point the generator at the URL and at jOOQ's plain PostgreSQL database:
   ```kotlin
   jooq {
       configuration {
           jdbc {
               driver = "org.testcontainers.jdbc.ContainerDatabaseDriver"
               url = "jdbc:tc:liquibasepostgres:17.5-alpine:///test-db?changelog=$projectDir/src/main/resources/liquibase/changelog-master.yml"
           }
           generator {
               database {
                   name = "org.jooq.meta.postgres.PostgresDatabase"
                   inputSchema = "public"
                   excludes = "databasechangelog|databasechangeloglock"
               }
           }
       }
   }
   ```

The `io.github.denis-markushin.jooq-codegen` convention plugin does all of this for you.

## How it works

Testcontainers' JDBC driver resolves the `liquibasepostgres` token through `ServiceLoader` to
`LiquibasePostgresContainerProvider`, which builds a `PostgreSQLContainer` subclass from the
image tag and the `changelog` parameter. Right after the container starts, the subclass runs
`Liquibase.update()` on it; sibling files referenced through `include` resolve relative to the
changelog's directory. The `changelog` parameter never reaches the PostgreSQL driver. jOOQ then
introspects the migrated schema through its own `PostgresDatabase`.

## Limits

The changelog path travels in the JDBC URL as-is: Testcontainers splits the query string on `&`
and `=` and does not URL-decode, so a path containing either character cannot be used.

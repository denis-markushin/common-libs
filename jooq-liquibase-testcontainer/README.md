# jooq-liquibase-testcontainer

Supplies a jOOQ `Database` implementation that applies Liquibase migrations in a Testcontainers PostgreSQL instance.

## Usage

1. Apply the plugin:
   ```kotlin
   plugins {
       id("org.jooq.jooq-codegen-gradle")
   }
   ```
2. Add dependency for code generation:
   ```kotlin
   jooqGenerator("org.dema:jooq-liquibase-testcontainer:x.x.x")
   ```
3. Configure the database:
    ```kotlin
    jooq {
      configuration {
          logging = org.jooq.meta.jaxb.Logging.DEBUG

          jdbc {
              driver = "org.testcontainers.jdbc.ContainerDatabaseDriver"
              url = "jdbc:tc:postgresql:17.5-alpine:///test-db"
          }

          generator {
              database {
                  name = "org.dema.jooq.liquibase.LiquibasePostgresTcDatabase"
                  includes = ".*"
                  excludes = "databasechangelog|databasechangeloglock"
                  inputSchema = "public"
                  properties {
                      property {
                          key = "liquibaseChangelogFile"
                          value = "${projectDir}/src/main/resources/liquibase/changelog-master.yml"
                      }
                  }
              }
          }
      }
    }
    ```
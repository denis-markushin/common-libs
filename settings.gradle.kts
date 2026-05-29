include(
    "bom",
    "jooq-utils",
    "security-starter",
    "jooq-liquibase-testcontainer",
    "graphql-dgs-starter",
    "test-common",
    "common-scalars-starter",
    "service-core",
    "outbox-starter",
)

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

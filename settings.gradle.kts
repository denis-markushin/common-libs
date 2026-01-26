include(
    "bom",
    "jooq-utils",
    "security-starter",
    "jooq-liquibase-testcontainer",
    "graphql-dgs-starter",
    "common-lib",
    "test-common",
    "common-scalars-starter",
)

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

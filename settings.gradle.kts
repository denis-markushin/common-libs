rootProject.name = "common-libs"

include(
    "bom",
    "jooq-utils",
    "security-starter",
    "jooq-liquibase-testcontainer",
    "graphql-dgs-starter",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

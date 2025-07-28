rootProject.name = "common-libs"

include(
    "bom",
    "jooq-utils",
    "security-starter",
    "jooq-liquibase-testcontainer",
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
rootProject.name = "common-libs"

include("bom", "jooq-utils", "security-starter")

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
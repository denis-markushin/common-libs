plugins {
    id("io.github.denis-markushin.kotlin-library")
}

val bomForResolutionOnly = configurations.create("bomForResolutionOnly")
    .apply {
        isCanBeConsumed = false
        isCanBeResolved = false
        isVisible = false
    }

dependencies {
    bomForResolutionOnly(platform(project(":bom")))
}

configurations.all {
    if (name != bomForResolutionOnly.name) {
        extendsFrom(bomForResolutionOnly)
    }
}

// Disables Gradle's custom module metadata from being published to maven. The
// metadata includes mapped dependencies which are not reasonably consumable by
// other mod developers.
tasks.withType<GenerateModuleMetadata>().configureEach { enabled = false }

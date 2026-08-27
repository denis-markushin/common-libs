# Version catalog

Every library published from this repository, aliased at the version of the
release the catalog itself belongs to. Name one version, get a set that was
built and tested together.

## Use

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("dema") {
            from("io.github.denis-markushin:catalog:2.1.0")
        }
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(dema.service.core)
    implementation(dema.security.starter)
    implementation(dema.bundles.graphql)
    implementation(dema.bundles.jooq)
    testImplementation(dema.test.common)
}
```

Upgrading every dema library is one line in `settings.gradle.kts`.

## Two catalogs, not one

Gradle refuses to combine `from()` with declarations of your own in the same
catalog, so a project keeps its own `gradle/libs.versions.toml` for anything this
repository does not publish. Importing this one under the name `dema` leaves
`libs` free for that, and the prefix says at a glance where a dependency comes
from:

```kotlin
implementation(dema.outbox.starter)   // published here
implementation(libs.bouncycastle)     // yours
```

## What is not in it

**Gradle plugins.** They are released from a different repository on a different
schedule; aliasing them here would tie the two together. Keep the plugin alias in
your own catalog.

**Test bundles.** What a service needs around `test-common` — which
Testcontainers modules, an HTTP stub, an awaitility — differs per service, and a
shared bundle would either be too small to help or force dependencies nobody
asked for. `bundles.graphql` and `bundles.jooq` are here because they genuinely
do not vary.

**Third-party libraries this repository does not build against.** Pinning a
version here would mean vouching for one that nothing in this build ever
exercises. The single exception is `spring-jooq`, which carries no version at all
— Spring's BOM supplies it — and exists only so `bundles.jooq` is complete.

## Maintenance

The library aliases are derived from the subprojects of this build, not from a
hand-written list, so a module added here appears in the catalog without anyone
remembering to add it.

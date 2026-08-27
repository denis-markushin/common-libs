description = "Version catalog of the libraries published from this repository"

plugins {
    `version-catalog`
}

val demaGroup = "io.github.denis-markushin"
val libsVersion = "dema-libs"

/*
 * Aliases every published module at the version of the release the catalog itself
 * belongs to, so a consumer that names one version gets a set that was built and
 * tested together. Derived from the actual subprojects rather than a hand-written
 * list, so a module added to this repository cannot be forgotten here.
 */
catalog {
    versionCatalog {
        version(libsVersion, project.version.toString())
        rootProject.subprojects
            .map { it.name }
            .filterNot { it == project.name }
            .sorted()
            .forEach { library(it, demaGroup, it).versionRef(libsVersion) }

        library("spring-jooq", "org.springframework.boot", "spring-boot-starter-jooq").withoutVersion()

        bundle("graphql", listOf("graphql-dgs-starter", "common-scalars-starter"))
        bundle("jooq", listOf("jooq-starter", "spring-jooq"))
    }
}

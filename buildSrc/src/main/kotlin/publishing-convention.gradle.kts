plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    coordinates(project.group.toString(), project.name, project.version.toString())
    publishToMavenCentral(automaticRelease = true)

    if (providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    pom {
        name.set(project.name)
        description.set(project.description)
        inceptionYear.set("2025")
        url.set("https://github.com/denis-markushin/common-libs/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("denis-markushin")
                name.set("Denis Markushin")
                url.set("https://github.com/denis-markushin/")
            }
        }
        scm {
            url.set("https://github.com/denis-markushin/common-libs/")
            connection.set("scm:git:git://github.com:denis-markushin/common-libs.git")
            developerConnection.set("scm:git:ssh://git@github.com:denis-markushin/common-libs.git")
        }
    }
}

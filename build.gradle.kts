import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = property("group") as String
version = property("version") as String

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spotless) apply true
    alias(libs.plugins.vanniktechMavenPublish) apply false
}

allprojects {
    group = rootProject.group
    version = rootProject.version

    apply {
        plugin(rootProject.libs.plugins.spotless.get().pluginId)
    }

    configure<SpotlessExtension> {
        lineEndings = LineEnding.GIT_ATTRIBUTES_FAST_ALLSAME
        kotlin {
            target("**/*.kt")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            ktlint(libs.versions.ktlint.get()).setEditorConfigPath(rootProject.file(".editorconfig").path)
            toggleOffOn()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(libs.versions.ktlint.get()).setEditorConfigPath(rootProject.file(".editorconfig").path)
            toggleOffOn()
        }
    }
}

subprojects {
    apply { plugin(rootProject.libs.plugins.vanniktechMavenPublish.get().pluginId) }
    configurePublishing()
}

configure(subprojects.filterNot { it == project(":bom") }) {
    apply {
        plugin("java-library")
        plugin(rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    }

    dependencies {
        "testImplementation"(platform(rootProject.libs.junit.dependecies))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    // Spring Boot starters dependencies
    if (name.contains("-starter")) {
        apply(plugin = rootProject.libs.plugins.kotlin.spring.get().pluginId)

        dependencies {
            // Apply the BOM to applicable subprojects.
            "implementation"(platform(project(":bom")))

            // Test libs
            "testImplementation"("org.springframework.boot:spring-boot-starter-test") {
                exclude(group = "org.mockito")
            }
            "testImplementation"("com.ninja-squad:springmockk")
            "testImplementation"("com.willowtreeapps.assertk:assertk-jvm")
            "testImplementation"("io.kotest:kotest-assertions-core")
        }
    }

    configureKotlinCompilation()

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<Jar> {
        manifest {
            attributes("Implementation-Version" to version)
        }
    }
}

fun Project.configurePublishing() {
    extensions.configure<MavenPublishBaseExtension> {
        coordinates(project.group.toString(), project.name, project.version.toString())

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

    tasks.withType<AbstractPublishToMaven> {
        doLast {
            publication.apply {
                logger.lifecycle("Published $groupId:$artifactId:$version artifact")
            }
        }
    }
}

private fun Project.configureKotlinCompilation() {
    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            javaParameters = true
            allWarningsAsErrors = true
        }
    }
}

tasks.assemble {
    dependsOn("spotlessInstallGitPrePushHook")
}

tasks.named("spotlessInstallGitPrePushHook") {
    onlyIf { !file(".git/hooks/pre-push").exists() }
}

tasks.withType<GenerateModuleMetadata>().configureEach { enabled = false }

import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "org.dema"
    version = "1.0.0"

    apply {
        plugin(rootProject.libs.plugins.spotless.get().pluginId)
    }

    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            ktlint(libs.versions.ktlint.get()).setEditorConfigPath(rootProject.file(".editorconfig").path)
            toggleOffOn()
            trimTrailingWhitespace()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint(libs.versions.ktlint.get())
        }
    }
}

(subprojects - project(":bom")).forEach { subproject ->
    subproject.apply {
        plugin(libs.plugins.kotlin.jvm.get().pluginId)
        plugin("java-library")
        plugin("signing")
    }

    if (subproject.name.contains("-starter")) {
        subproject.apply {
            plugin(libs.plugins.kotlin.spring.get().pluginId)
        }

        subproject.dependencies {
            "implementation"("org.springframework.boot:spring-boot-starter-web")
            // Test libs
            "testImplementation"("org.junit.jupiter:junit-jupiter-api")
            "testImplementation"("org.springframework.boot:spring-boot-starter-test") {
                exclude(group = "org.mockito")
            }
            "testImplementation"("com.ninja-squad:springmockk")
            "testImplementation"("com.willowtreeapps.assertk:assertk-jvm")
            "testImplementation"("io.kotest:kotest-assertions-core")
        }
    }

    subproject.configurePublishing()
    subproject.configureJavaCompilation()
    subproject.configureKotlinCompilation()

    subproject.tasks.withType<Test> {
        useJUnitPlatform()
    }

    subproject.tasks.withType<Jar> {
        manifest {
            attributes("Implementation-Version" to subproject.version)
        }
    }
}

fun Project.configurePublishing() {
    apply {
        plugin("maven-publish")
    }

    extensions.configure<PublishingExtension> {
        repositories {
            mavenLocal()
        }
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
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

fun Project.configureJavaCompilation() {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            withSourcesJar()
            withJavadocJar()
        }
    }
}

private fun Project.configureKotlinCompilation() {
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
            freeCompilerArgs =
                listOf(
                    "-Xjsr305=strict",
                    "-Xemit-jvm-type-annotations",
                )
            javaParameters = true
            allWarningsAsErrors = true
        }
    }
}
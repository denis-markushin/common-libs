import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
}

plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    group = "org.dema"
    version = "1.0.0"

    apply {
        plugin(rootProject.libs.plugins.spotless.get().pluginId)
    }

    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            ktlint("1.2.1").setEditorConfigPath(rootProject.file(".editorconfig").path)
            toggleOffOn()
            trimTrailingWhitespace()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.2.1")
        }
    }
}

subprojects {
    apply {
        plugin(rootProject.libs.plugins.kotlin.get().pluginId)
        plugin("java-library")
        plugin("signing")
    }

    configurePublishing()

    dependencies {
        "implementation"(kotlin("stdlib"))
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            withSourcesJar()
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
        }
    }

    tasks.withType<Jar> {
        manifest {
            attributes("Implementation-Version" to project.version)
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
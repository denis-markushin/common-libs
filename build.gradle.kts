plugins {
    kotlin("jvm") apply false
    alias(libs.plugins.vercraft)
    alias(libs.plugins.dema.spotless)
}

subprojects {
    apply(plugin = "publishing-convention")
}

configure(subprojects.filterNot { it == project(":bom") }) {
    apply(plugin = "kotlin-library-convention")

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    // Spring Boot starters dependencies
    if (name.contains("-starter")) {
        apply(plugin = "org.jetbrains.kotlin.plugin.spring")
        apply(plugin = "org.jetbrains.kotlin.kapt")

        dependencies {
            "kapt"("org.springframework.boot:spring-boot-configuration-processor")
            // Test libs
            "testImplementation"("org.springframework.boot:spring-boot-starter-test") {
                exclude(group = "org.mockito")
            }
            "testImplementation"("com.ninja-squad:springmockk")
            "testImplementation"("com.willowtreeapps.assertk:assertk-jvm")
            "testImplementation"("io.kotest:kotest-assertions-core")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<Jar> {
        manifest {
            attributes("Implementation-Version" to version)
        }
    }
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.BIN
}

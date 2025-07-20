description = "Billing Of Materials"

plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(platform(libs.spring.cloud.dependencies))
    api(platform(libs.netflix.dgs.dependencies))

    constraints {
        // Third party libs
        api(libs.jaywayJsonPath)
        // Test
        api(libs.assertk)
        api(libs.kotest.assertions)
        api(libs.mockk)
        api(libs.springmock)
    }
}
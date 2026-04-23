plugins {
    kotlin("kapt")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(libs.logbook)

    api("org.springframework.boot:spring-boot-starter-web") {
        exclude(module = "spring-boot-starter-tomcat")
    }
    api("org.springframework.boot:spring-boot-starter-jetty")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api(libs.kotlin.logging)
    api(libs.spring.doc)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.assertk)

    kapt("org.springframework.boot:spring-boot-configuration-processor")
}

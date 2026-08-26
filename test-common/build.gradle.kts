description = "Common test utils"

dependencies {
    compileOnlyApi("com.willowtreeapps.assertk:assertk-jvm:0.28.1")

    // Consumers already carry these on their test runtime classpath; keeping them
    // compileOnly lets a project use only the assertion helpers without dragging
    // Spring or Testcontainers into a build that has no use for either.
    compileOnly("org.springframework.boot:spring-boot-test")
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework.security:spring-security-core")
    compileOnly("org.springframework.security:spring-security-oauth2-jose")
    compileOnly("org.springframework.security:spring-security-oauth2-resource-server")
    compileOnly("org.springframework.security:spring-security-test")
    compileOnly("org.testcontainers:testcontainers-kafka")
    compileOnly("org.testcontainers:testcontainers-minio")

    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework:spring-context")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation("org.springframework.security:spring-security-oauth2-jose")
    testImplementation("org.springframework.security:spring-security-oauth2-resource-server")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testImplementation("org.testcontainers:testcontainers-minio")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm")
}

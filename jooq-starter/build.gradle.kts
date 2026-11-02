description = "jOOQ Spring Boot starter"

dependencies {
    api("org.jooq:jooq")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jooq")
    compileOnlyApi("org.springframework.data:spring-data-commons")
    compileOnly("org.springframework.security:spring-security-core")
    compileOnly("org.springframework.security:spring-security-oauth2-jose")

    testImplementation("org.springframework.data:spring-data-commons")
    testImplementation("org.springframework.security:spring-security-oauth2-jose")
    testImplementation("org.springframework.security:spring-security-oauth2-resource-server")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.postgresql:postgresql")
}

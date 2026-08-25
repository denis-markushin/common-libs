description = "jOOQ Spring Boot starter"

dependencies {
    api("org.jooq:jooq")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jooq")
    compileOnlyApi("org.springframework.data:spring-data-commons")

    testImplementation("org.springframework.data:spring-data-commons")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.postgresql:postgresql")
}

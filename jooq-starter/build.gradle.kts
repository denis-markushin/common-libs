description = "jOOQ Spring Boot starter"

dependencies {
    api("org.jooq:jooq:3.18.4")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    compileOnlyApi("org.springframework.data:spring-data-commons:3.2.5")

    testImplementation("org.springframework.data:spring-data-commons:3.2.5")
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testImplementation("org.postgresql:postgresql:42.6.2")
}

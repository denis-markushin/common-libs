description = "Transactional outbox starter (Postgres + Kafka)"

dependencies {
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("org.springframework.kafka:spring-kafka")
    api("org.liquibase:liquibase-core")

    implementation(libs.kotlin.logging)
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-kafka")
    implementation("org.springframework.boot:spring-boot-liquibase")
    implementation("tools.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.postgresql:postgresql")
}

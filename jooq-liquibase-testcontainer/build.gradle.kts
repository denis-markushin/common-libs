description = "JOOQ codegen database implementation that runs Liquibase migrations in a Testcontainers PostgreSQL instance"

dependencies {
    implementation(platform(project(":bom")))

    implementation("org.jooq:jooq-meta")
    implementation("org.testcontainers:postgresql")
    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")
}
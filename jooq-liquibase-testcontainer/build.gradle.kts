description = "JOOQ codegen database implementation that runs Liquibase migrations in a Testcontainers PostgreSQL instance"

dependencies {
    implementation("org.jooq:jooq-meta:3.18.4")
    implementation("org.testcontainers:postgresql:1.19.7")
    implementation("org.liquibase:liquibase-core:4.24.0")
    implementation("org.postgresql:postgresql:42.6.2")
}

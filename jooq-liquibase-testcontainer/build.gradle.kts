description = "Testcontainers JDBC provider that applies Liquibase migrations to a PostgreSQL container before jOOQ codegen"

dependencies {
    implementation("org.testcontainers:testcontainers-postgresql")
    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm")
}

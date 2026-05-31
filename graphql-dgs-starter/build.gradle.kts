description = "Spring Boot starter that simplifies building GraphQL APIs with Jooq and DGS Framework"

dependencies {
    implementation(project(":jooq-starter"))
    implementation(libs.kotlin.logging)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    compileOnly("org.springframework.security:spring-security-core")

    api("com.netflix.graphql.dgs:dgs-starter")
    api("com.netflix.graphql.dgs:graphql-dgs-extended-scalars")
    api("com.netflix.graphql.dgs:graphql-dgs-spring-boot-micrometer")
    api("com.netflix.graphql.dgs:graphql-dgs-pagination")

    testImplementation("org.springframework.security:spring-security-core")
}

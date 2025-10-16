description = "Spring Boot starter that simplifies building GraphQL APIs with Jooq and DGS Framework"

dependencies {
    implementation(project(":jooq-utils"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter")

    api(project(":common-scalars-starter"))
    api("com.netflix.graphql.dgs:dgs-starter")
    api("com.netflix.graphql.dgs:graphql-dgs-spring-boot-micrometer")
    api("com.netflix.graphql.dgs:graphql-dgs-pagination")
}

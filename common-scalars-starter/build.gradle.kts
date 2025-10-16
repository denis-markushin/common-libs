description = "Spring Boot starter that simplifies building GraphQL APIs with Jooq and DGS Framework"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("com.graphql-java:graphql-java:24.3")

    api("com.netflix.graphql.dgs:dgs-starter")
    api("com.netflix.graphql.dgs:graphql-dgs-extended-scalars")
}

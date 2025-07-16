description = "Spring Boot Security starter"

dependencies {
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.security:spring-security-oauth2-jose")
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    implementation("com.jayway.jsonpath:json-path")

    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
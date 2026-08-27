description = "Spring Boot MinIO starter"

dependencies {
    api(libs.minio)

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-web")

    compileOnly("jakarta.validation:jakarta.validation-api")
}

dependencies {
    testImplementation("io.mockk:mockk")
}

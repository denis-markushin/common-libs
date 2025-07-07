dependencies {
    implementation(platform(project(":bom")))
    api(libs.jooq)
    api("org.springframework.data:spring-data-commons")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
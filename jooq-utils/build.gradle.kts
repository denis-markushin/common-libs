description = "Jooq utils"

dependencies {
    implementation(platform(project(":bom")))
    api(libs.jooq)
    api("org.springframework.data:spring-data-commons")
}
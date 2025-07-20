description = "Jooq utils"

dependencies {
    implementation(platform(project(":bom")))
    api("org.jooq:jooq")
    api("org.springframework.data:spring-data-commons")
}
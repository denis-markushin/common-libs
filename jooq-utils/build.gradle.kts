description = "Jooq utils"

dependencies {
    compileOnly(platform(project(":bom")))
    testImplementation(platform(project(":bom")))
    api("org.jooq:jooq")
    compileOnlyApi("org.springframework.data:spring-data-commons")
    testImplementation("org.springframework.data:spring-data-commons")
}

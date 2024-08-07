dependencies {
    implementation(platform(rootProject.libs.spring.boot.dependencies))
    api(rootProject.libs.jooq)
    api("org.springframework.data:spring-data-commons")
}
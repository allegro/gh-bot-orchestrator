plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.integration.test)
    alias(libs.plugins.test.logger)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.web)

    implementation(libs.kotlin.logging)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatype)
    implementation(libs.auth0.java.jwt)
    implementation(libs.auth0.jwks.rsa)
    implementation(libs.java.semver)
    implementation(libs.json.path)
    implementation(libs.db.scheduler.spring.boot.starter)
    implementation(libs.postgresql)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.docker.compose)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.flyway.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}

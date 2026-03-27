plugins {
    java
    alias(libs.plugins.axion.release)
    id("conventions.spring-boot")
    id("conventions.dot-env")
}

version = scmVersion.version

springBoot {
    mainClass = "pl.allegro.tech.github.botorchestrator.AppRunnerKt"
}

dependencies {
    implementation(projects.backend)
    implementation(projects.frontend)
}

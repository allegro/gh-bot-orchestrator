plugins {
    application
    `maven-publish`
    id("conventions.dot-env")
}

application {
    mainClass = "pl.allegro.tech.github.botorchestrator.AppRunnerKt"
}

dependencies {
    implementation(projects.backend)
    implementation(projects.frontend)
}

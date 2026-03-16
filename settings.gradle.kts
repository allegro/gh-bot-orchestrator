dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "gh-bot-orchestrator"

include("frontend", "backend")
includeBuild("build-logic")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

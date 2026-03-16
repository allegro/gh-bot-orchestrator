dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "github-workflow-orchestrator"

include("frontend", "backend")
includeBuild("build-logic")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

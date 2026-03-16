rootProject.name = "github-workflow-orchestrator"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("build-logic")

dependencyResolutionManagement {
    repositories {
        maven("https://artifactory.allegrogroup.com/artifactory/group-allegro/")
        mavenCentral()
    }
}

include("frontend", "backend")

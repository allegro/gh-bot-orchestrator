rootProject.name = "build-logic"

plugins {
    id("dev.panuszewski.typesafe-conventions") version "0.10.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

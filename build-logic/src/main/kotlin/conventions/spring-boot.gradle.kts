package conventions

import libs
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    alias(libs.plugins.spring.boot)
}

tasks.named<BootBuildImage>("bootBuildImage") {
    val imageOwner = System.getenv("GITHUB_REPOSITORY_OWNER") ?: "allegro"
    imageName = "ghcr.io/$imageOwner/gh-workflow-orchestrator:${project.version}"

    docker {
        publishRegistry {
            url = "ghcr.io"
            username = System.getenv("GITHUB_ACTOR") ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: ""
        }
    }
}

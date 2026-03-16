package pl.allegro.tech.github.botorchestrator.infra.github

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.github.server")
data class GithubServerProperties(
    val baseUrl: String
)

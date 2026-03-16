package pl.allegro.tech.common.pullrequestmanager.infra.github

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.github.server")
data class GithubServerProperties(
    val baseUrl: String
)

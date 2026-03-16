package pl.allegro.tech.github.botorchestrator.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.github.botorchestrator.domain.DependabotMetadata
import pl.allegro.tech.github.botorchestrator.domain.PullRequest
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.dependabot.VersionUpdate
import java.time.Instant

data class PullRequestEvent(
    @field:JsonProperty("event") val data: PullRequestEventPayload,
    val delivery: String
) {

    data class PullRequestEventPayload(
        val repository: GithubRepo,
        @field:JsonProperty("pull_request") val pullRequest: GithubPullRequest,
        val sender: GithubPrincipal,
        val action: String
    )

    fun toDomain(objectMapper: ObjectMapper): PullRequest = PullRequest(
        repository = Repository(data.repository.owner.login, data.repository.name),
        number = data.pullRequest.number,
        headSha = data.pullRequest.head.sha,
        lastUpdateTimestamp = Instant.parse(data.pullRequest.updatedAt),
        dependabotMetadata = DependabotMetadata(VersionUpdate.from(data)),
        originalEventString = objectMapper.writeValueAsString(data),
        customRepositoryProperties = data.repository.customProperties
    )
}

data class GithubRepo(
    val id: Long,
    val name: String,
    @field:JsonProperty("full_name") val fullName: String,
    val owner: GithubPrincipal,
    val topics: Set<String> = emptySet(),
    @field:JsonProperty("custom_properties") val customProperties: Map<String, Any> = emptyMap()
)

data class GithubPrincipal(val login: String, val id: Long, val type: String? = null)

data class GithubPullRequest(
    val url: String,
    val number: Int,
    val title: String,
    var body: String?,
    val user: GithubPrincipal,
    val head: GithubRef,
    val base: GithubRef,
    val merged: Boolean,
    @field:JsonProperty("updated_at") val updatedAt: String,
    @field:JsonProperty("html_url") val htmlUrl: String
)

data class GithubRef(val ref: String, val sha: String, val repo: GithubRepo)

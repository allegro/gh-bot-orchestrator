package pl.allegro.tech.github.botorchestrator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import pl.allegro.tech.github.botorchestrator.PullRequestEventFixture.pullRequestEvent
import pl.allegro.tech.github.botorchestrator.domain.PullRequest
import pl.allegro.tech.github.botorchestrator.domain.Repository
import java.time.Instant

object PullRequestFixture {

    val updatedAt = Instant.parse("2024-12-17T11:56:39.000Z")

    fun pullRequest(applyModifications: PullRequest.() -> PullRequest = { this }): PullRequest {
        return PullRequest(
            repository = Repository("some-org", "some-repository"),
            number = 234,
            headSha = "909abe27a85c9accde8c1fdb19d0fb234cff3e1e",
            lastUpdateTimestamp = updatedAt,
            dependabotMetadata = null,
            originalEventString = jacksonObjectMapper().writeValueAsString(pullRequestEvent().data),
            customRepositoryProperties = emptyMap()
        ).applyModifications()
    }
}

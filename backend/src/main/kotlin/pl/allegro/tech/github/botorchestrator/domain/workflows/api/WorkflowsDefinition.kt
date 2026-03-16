package pl.allegro.tech.github.botorchestrator.domain.workflows.api

import pl.allegro.tech.github.botorchestrator.domain.PullRequest
import pl.allegro.tech.github.botorchestrator.domain.PullRequestEventMapper
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.dependabot.VersionUpdate
import pl.allegro.tech.github.botorchestrator.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.DependabotMatcher
import pl.allegro.tech.github.botorchestrator.infra.github.PullRequestFileDto
import java.time.Instant
import java.util.*

data class WorkflowDefinition(
    val id: UUID,
    val repository: Repository,
    val enabled: Boolean,
    val ref: String,
    val concurrencyGroup: ConcurrencyGroup,
    val eventMapper: PullRequestEventMapper,
    val maxConcurrentRuns: Int,
    val check: PullRequestCheck,
    val fileName: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val filters: CompoundPullRequestMatcher,
) {

    fun hasDependabotFilters(): Boolean = filters.matchers.any { it is DependabotMatcher }

    fun matches(pullRequest: PullRequest, pullRequestData: Sequence<PullRequestFileDto>): Boolean {
        return filters.match(pullRequest, pullRequestData)
    }

    fun requiresPullRequestFiles(): Boolean {
        return filters.requiresPullRequestFiles()
    }

    sealed interface ConcurrencyGroup {

        fun getType(): ConcurrencyGroupType
        fun generateJobId(pullRequest: PullRequest): String
    }

    data object OneJobPerRepository : ConcurrencyGroup {

        override fun getType(): ConcurrencyGroupType = ConcurrencyGroupType.ONE_JOB_PER_REPOSITORY

        override fun generateJobId(pullRequest: PullRequest): String = pullRequest.repository.id.name
    }

    data object OneJobPerPullRequest : ConcurrencyGroup {

        override fun getType(): ConcurrencyGroupType = ConcurrencyGroupType.ONE_JOB_PER_PULL_REQUEST

        override fun generateJobId(pullRequest: PullRequest): String =
            "${pullRequest.repository.id.name}-${pullRequest.number}"
    }

    data class PullRequestCheck(val name: String, val detailsPage: DetailsPage)

    data class DetailsPage(
        val title: String,
        val summary: String,
        val details: String? = null,
        val customUrl: String? = null
    )

    enum class ConcurrencyGroupType {
        ONE_JOB_PER_REPOSITORY, ONE_JOB_PER_PULL_REQUEST;
    }

    enum class MatchingStrategy {
        ALL, ANY
    }
}

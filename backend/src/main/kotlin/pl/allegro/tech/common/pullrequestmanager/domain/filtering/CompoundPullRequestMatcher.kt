package pl.allegro.tech.common.pullrequestmanager.domain.filtering

import pl.allegro.tech.common.pullrequestmanager.domain.PullRequest
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.MatchingStrategy
import pl.allegro.tech.common.pullrequestmanager.infra.github.PullRequestFileDto

class CompoundPullRequestMatcher(
    internal val matchingStrategy: MatchingStrategy,
    val matchers: Collection<PullRequestMatcher>,
    internal val requiresPullRequestData: Boolean = matchers.filterIsInstance<FilePathMatcher>().count() > 0
) : PullRequestMatcher {

    override fun match(pullRequest: PullRequest, pullRequestFiles: Sequence<PullRequestFileDto>): Boolean {
        return when (matchingStrategy) {
            MatchingStrategy.ALL -> matchers.all { filter -> filter.match(pullRequest, pullRequestFiles) }
            MatchingStrategy.ANY -> matchers.any { filter -> filter.match(pullRequest, pullRequestFiles) }
        }
    }

    override fun requiresPullRequestFiles(): Boolean {
        return requiresPullRequestData
    }

    fun matchingStrategy(): MatchingStrategy = matchingStrategy
}

package pl.allegro.tech.common.pullrequestmanager.domain.filtering

import pl.allegro.tech.common.pullrequestmanager.domain.PullRequest
import pl.allegro.tech.common.pullrequestmanager.infra.github.PullRequestFileDto

sealed interface PullRequestMatcher {

    fun match(pullRequest: PullRequest, pullRequestFiles: Sequence<PullRequestFileDto> = emptySequence()): Boolean
    fun requiresPullRequestFiles(): Boolean = false
}

object MatchEverything : PullRequestMatcher {

    override fun match(pullRequest: PullRequest, pullRequestFiles: Sequence<PullRequestFileDto>): Boolean {
        return true
    }

    override fun requiresPullRequestFiles(): Boolean {
        return true
    }
}

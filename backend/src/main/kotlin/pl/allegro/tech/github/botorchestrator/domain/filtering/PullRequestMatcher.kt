package pl.allegro.tech.github.botorchestrator.domain.filtering

import pl.allegro.tech.github.botorchestrator.domain.PullRequest
import pl.allegro.tech.github.botorchestrator.infra.github.PullRequestFileDto

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

package pl.allegro.tech.github.botorchestrator

import pl.allegro.tech.github.botorchestrator.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.JsonPathMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.PullRequestMatcher
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition

object PullRequestFiltersFixture {

    fun matchAny(modifications: MutableList<PullRequestMatcher>.() -> MutableList<PullRequestMatcher>): CompoundPullRequestMatcher {
        val filters = mutableListOf<PullRequestMatcher>()
            .modifications()

        return CompoundPullRequestMatcher(
            WorkflowDefinition.MatchingStrategy.ANY,
            filters,
        )
    }

    fun matchAll(modifications: MutableList<PullRequestMatcher>.() -> List<PullRequestMatcher>): CompoundPullRequestMatcher {
        val filters = mutableListOf<PullRequestMatcher>()
            .modifications()

        return CompoundPullRequestMatcher(
            WorkflowDefinition.MatchingStrategy.ALL,
            filters,
        )
    }

    fun MutableList<PullRequestMatcher>.jsonPath(path: String, pattern: String): MutableList<PullRequestMatcher> {
        this.add(JsonPathMatcher(path, pattern))
        return this
    }
}

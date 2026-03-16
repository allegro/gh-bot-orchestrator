package pl.allegro.tech.common.pullrequestmanager

import pl.allegro.tech.common.pullrequestmanager.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.JsonPathMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.PullRequestMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition

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

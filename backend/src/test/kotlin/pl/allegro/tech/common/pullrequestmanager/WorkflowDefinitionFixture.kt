package pl.allegro.tech.common.pullrequestmanager

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.property.Arb
import io.kotest.property.arbitrary.ArbitraryBuilderContext
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.single
import io.kotest.property.arbitrary.stringPattern
import pl.allegro.tech.common.pullrequestmanager.domain.PullRequestEventMapper
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.JsonPathMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.MatchEverything
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.PullRequestMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.DetailsPage
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.OneJobPerPullRequest
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.OneJobPerRepository
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.PullRequestCheck
import java.util.*

object WorkflowDefinitionFixture {

    fun randomWorkflowDefinition(applyModifications: WorkflowDefinition.() -> WorkflowDefinition = { this }): WorkflowDefinition =
        arbitrary {
            WorkflowDefinition(
                repository = Repository(anyString(), anyString()),
                ref = anyString(),
                id = UUID.randomUUID(),
                enabled = true,
                concurrencyGroup = Arb.of(listOf(OneJobPerPullRequest, OneJobPerRepository)).bind(),
                filters = CompoundPullRequestMatcher(
                    WorkflowDefinition.MatchingStrategy.ANY,
                    listOf(MatchEverything),
                ),
                maxConcurrentRuns = Arb.int(min = 1, max = 10).bind(),
                eventMapper = PullRequestEventMapper(jacksonObjectMapper(), listOf("$")),
                fileName = anyString(),
                check = PullRequestCheck(anyString(), DetailsPage(anyString(), anyString()))
            ).applyModifications()
        }.single()

}

fun WorkflowDefinition.filters(
    applyModifications: MutableList<PullRequestMatcher>.() -> MutableList<PullRequestMatcher>
): WorkflowDefinition {
    return this.copy(filters = CompoundPullRequestMatcher(WorkflowDefinition.MatchingStrategy.ALL, mutableListOf<PullRequestMatcher>().applyModifications()))
}

fun MutableList<PullRequestMatcher>.jsonPathFilter(path: String, matcher: String): MutableList<PullRequestMatcher> {
    this.add(JsonPathMatcher(path, matcher))
    return this
}

private suspend fun ArbitraryBuilderContext.anyString(): String = Arb.stringPattern("[a-zA-Z]+").bind()

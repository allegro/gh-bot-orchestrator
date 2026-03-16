package pl.allegro.tech.common.pullrequestmanager

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component
import pl.allegro.tech.common.pullrequestmanager.domain.PullRequestEventMapper
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.JsonPathMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.WorkflowDefinitionsEditor
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.WorkflowDefinitionsReader
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.MatchingStrategy.ALL
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinitionToAdd
import java.time.Instant

@Component
class WorkflowDefinitionFixtures(
    private val workflowDefinitionsReader: WorkflowDefinitionsReader,
    private val workflowDefinitionsEditor: WorkflowDefinitionsEditor
) {

    fun randomWorkflow(
        repository: Repository = Repository("some-owner", "some-repository"),
        enabled: Boolean = true,
        ref: String = "main",
        concurrencyGroup: WorkflowDefinition.ConcurrencyGroup = WorkflowDefinition.OneJobPerPullRequest,
        eventMapper: PullRequestEventMapper = PullRequestEventMapper(jacksonObjectMapper(), listOf("$")),
        maxConcurrentRuns: Int = 1,
        check: WorkflowDefinition.PullRequestCheck = WorkflowDefinition.PullRequestCheck(
            name = "some check name",
            detailsPage = WorkflowDefinition.DetailsPage(
                title = "Some title",
                summary = "Some summary",
                details = "Some details"
            )
        ),
        fileName: String = "some-workflow-name.yml",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
        filters: CompoundPullRequestMatcher = CompoundPullRequestMatcher(
            matchingStrategy = ALL,
            matchers = listOf(JsonPathMatcher("$.action", "opened|synchronize"))
        )
    ): WorkflowDefinition {
        val workflowDefinition = WorkflowDefinitionToAdd(
            repository = repository,
            enabled = enabled,
            ref = ref,
            concurrencyGroup = concurrencyGroup,
            eventMapper = eventMapper,
            maxConcurrentRuns = maxConcurrentRuns,
            check = check,
            fileName = fileName,
            createdAt = createdAt,
            updatedAt = updatedAt,
            filters = filters
        )
        val savedId = workflowDefinitionsEditor.saveDefinition(workflowDefinition)
        return workflowDefinitionsReader.getDefinitionFor(savedId)
    }
}

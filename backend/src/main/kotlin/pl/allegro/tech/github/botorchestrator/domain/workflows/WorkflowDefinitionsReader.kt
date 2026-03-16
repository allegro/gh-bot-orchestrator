package pl.allegro.tech.github.botorchestrator.domain.workflows

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import pl.allegro.tech.github.botorchestrator.domain.PullRequestEventMapper
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.DependabotMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.FilePathMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.JsonPathMatcher
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.ConcurrencyGroupType.ONE_JOB_PER_PULL_REQUEST
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.ConcurrencyGroupType.ONE_JOB_PER_REPOSITORY
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.OneJobPerPullRequest
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.OneJobPerRepository
import java.util.UUID

@Component
data class WorkflowDefinitionsReader(
    private val workflowDefinitionRepository: WorkflowDefinitionRepository,
    private val objectMapper: ObjectMapper
) {

    fun getDefinitions(): Collection<WorkflowDefinition> {
        return workflowDefinitionRepository.findAll().map { persistedWorkflowDefinition -> persistedWorkflowDefinition.toDomain() }
    }

    fun getDefinitionsIds(): Set<UUID> {
        return workflowDefinitionRepository.findEnabled()
    }

    fun exists(repository: Repository, ref: String, path: String): Boolean {
        return workflowDefinitionRepository.exists(
            repository.id.name,
            repository.id.owner,
            ref,
            path
        )
    }

    fun getDefinitionFor(id: UUID): WorkflowDefinition {
        val workflowDefinition = workflowDefinitionRepository.findById(id)
        if (workflowDefinition == null) error("Workflow definition with id $id not found")
        return workflowDefinition.toDomain()
    }

    private fun PersistedWorkflowDefinition.toDomain(): WorkflowDefinition = WorkflowDefinition(
        id = this.id!!,
        enabled = this.enabled,
        repository = Repository(owner = this.repository.owner, repo = this.repository.name),
        fileName = this.workflowFileName,
        ref = this.ref,
        concurrencyGroup = when (this.concurrencyGroup) {
            ONE_JOB_PER_REPOSITORY -> OneJobPerRepository
            ONE_JOB_PER_PULL_REQUEST -> OneJobPerPullRequest
        },
        filters = CompoundPullRequestMatcher(
            matchingStrategy = this.filters.matchingStrategy,
            matchers = this.filters.matchers.map {
                when (it) {
                    is PersistedWorkflowDefinition.DependabotMatcher -> DependabotMatcher(
                        artifactRegex = it.artifactRegex.toRegex()
                    )

                    is PersistedWorkflowDefinition.FileMatcher -> FilePathMatcher(
                        files = it.files,
                        statuses = it.statuses
                    )

                    is PersistedWorkflowDefinition.JsonPathMatcher -> JsonPathMatcher(
                        path = it.path,
                        pattern = it.regex
                    )
                }
            },
        ),
        maxConcurrentRuns = this.maxConcurrentRuns,
        eventMapper = PullRequestEventMapper(objectMapper, this.mapping.fields.map(String::trim)),
        check = WorkflowDefinition.PullRequestCheck(
            this.check.name,
            WorkflowDefinition.DetailsPage(
                this.check.detailsPage.title,
                this.check.detailsPage.summary,
                this.check.detailsPage.details,
            ),
        ),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

package pl.allegro.tech.github.botorchestrator.domain.workflows.web

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import pl.allegro.tech.github.botorchestrator.domain.PullRequestEventMapper
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.DependabotMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.FilePathMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.JsonPathMatcher
import pl.allegro.tech.github.botorchestrator.domain.workflows.WorkflowDefinitionsEditor
import pl.allegro.tech.github.botorchestrator.domain.workflows.WorkflowDefinitionsReader
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.ConcurrencyGroupType
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinitionToAdd
import pl.allegro.tech.github.botorchestrator.domain.workflows.web.WorkflowDefinitionRequest.*
import pl.allegro.tech.github.botorchestrator.domain.workflows.web.WorkflowDefinitionResponse.CheckResponse
import pl.allegro.tech.github.botorchestrator.domain.workflows.web.WorkflowDefinitionResponse.DetailsPageResponse
import pl.allegro.tech.github.botorchestrator.domain.workflows.web.WorkflowDefinitionResponse.FiltersResponse
import pl.allegro.tech.github.botorchestrator.domain.workflows.web.WorkflowDefinitionResponse.FiltersResponse.DependabotMatcherResponse
import pl.allegro.tech.github.botorchestrator.domain.workflows.web.WorkflowDefinitionResponse.FiltersResponse.FileMatcherResponse
import pl.allegro.tech.github.botorchestrator.domain.workflows.web.WorkflowDefinitionResponse.FiltersResponse.JsonPathMatcherResponse
import pl.allegro.tech.github.botorchestrator.domain.workflows.web.WorkflowDefinitionResponse.MappingResponse
import pl.allegro.tech.github.botorchestrator.domain.workflows.web.WorkflowDefinitionResponse.RepositoryResponse
import java.util.*

@RestController
@RequestMapping("/api/workflows")
class WorkflowDefinitionsEndpoint(
    private val workflowDefinitionsReader: WorkflowDefinitionsReader,
    private val workflowDefinitionsEditor: WorkflowDefinitionsEditor,
    private val objectMapper: ObjectMapper
) {

    @GetMapping
    fun getWorkflowsDefinitions(): List<WorkflowDefinitionResponse> {
        return workflowDefinitionsReader.getDefinitions().map(::toResponse)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createWorkflowDefinition(@RequestBody request: WorkflowDefinitionRequest): WorkflowDefinitionResponse {
        val workflowDefinitionToAdd = toWorkflowDefinitionToAdd(request)
        val id = workflowDefinitionsEditor.saveDefinition(workflowDefinitionToAdd)
        val created = workflowDefinitionsReader.getDefinitionFor(id)
        return toResponse(created)
    }

    @PutMapping("/{id}")
    fun updateWorkflowDefinition(
        @PathVariable id: String,
        @RequestBody request: WorkflowDefinitionRequest
    ): WorkflowDefinitionResponse {
        val workflowId = UUID.fromString(id)
        val workflowDefinitionToAdd = toWorkflowDefinitionToAdd(request)
        workflowDefinitionsEditor.updateDefinition(workflowId, workflowDefinitionToAdd)
        val updated = workflowDefinitionsReader.getDefinitionFor(workflowId)
        return toResponse(updated)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteWorkflowDefinition(@PathVariable id: String) {
        val workflowId = UUID.fromString(id)
        workflowDefinitionsEditor.deleteDefinition(workflowId)
    }

    private fun toWorkflowDefinitionToAdd(request: WorkflowDefinitionRequest): WorkflowDefinitionToAdd {
        val concurrencyGroup = when (request.concurrencyGroup) {
            ConcurrencyGroupType.ONE_JOB_PER_REPOSITORY -> WorkflowDefinition.OneJobPerRepository
            ConcurrencyGroupType.ONE_JOB_PER_PULL_REQUEST -> WorkflowDefinition.OneJobPerPullRequest
        }

        val matchers = request.filters.matchers.map { matcher ->
            when (matcher) {
                is JsonPathMatcherRequest -> JsonPathMatcher(path = matcher.path, pattern = matcher.regex)
                is FileMatcherRequest -> FilePathMatcher(files = matcher.file, statuses = matcher.statuses)
                is DependabotMatcherRequest -> DependabotMatcher(artifactRegex = matcher.artifactRegex.toRegex())
            }
        }

        return WorkflowDefinitionToAdd(
            repository = Repository(owner = request.repository.owner, repo = request.repository.name),
            enabled = request.enabled,
            ref = request.ref,
            concurrencyGroup = concurrencyGroup,
            eventMapper = PullRequestEventMapper(objectMapper, request.mapping.fields),
            maxConcurrentRuns = request.maxConcurrentRuns,
            check = WorkflowDefinition.PullRequestCheck(
                name = request.check.name,
                detailsPage = WorkflowDefinition.DetailsPage(
                    title = request.check.detailsPage.title,
                    summary = request.check.detailsPage.summary,
                    details = request.check.detailsPage.details
                )
            ),
            fileName = request.workflowFilename,
            filters = CompoundPullRequestMatcher(
                matchingStrategy = request.filters.matchingStrategy,
                matchers = matchers
            )
        )
    }

    private fun toResponse(definition: WorkflowDefinition): WorkflowDefinitionResponse {
        return WorkflowDefinitionResponse(
            id = definition.id.toString(),
            enabled = definition.enabled,
            repository = RepositoryResponse(
                name = definition.repository.id.name,
                owner = definition.repository.id.owner
            ),
            ref = definition.ref,
            maxConcurrentRuns = definition.maxConcurrentRuns,
            concurrencyGroup = definition.concurrencyGroup.getType(),
            filters = FiltersResponse(
                matchingStrategy = definition.filters.matchingStrategy(),
                matchers = definition.filters.matchers.map { matcher ->
                    when (matcher) {
                        is JsonPathMatcher -> JsonPathMatcherResponse(
                            path = matcher.path,
                            regex = matcher.pattern.toString()
                        )

                        is FilePathMatcher -> FileMatcherResponse(
                            file = matcher.files.toString(),
                            statuses = matcher.statuses,
                        )

                        is DependabotMatcher -> DependabotMatcherResponse(
                            artifactRegex = matcher.artifactRegex.toString()
                        )

                        else -> throw IllegalArgumentException("Unknown matcher type: ${matcher::class.simpleName}")
                    }
                }
            ),
            mapping = MappingResponse(
                fields = definition.eventMapper.paths
            ),
            check = CheckResponse(
                name = definition.check.name,
                detailsPage = DetailsPageResponse(
                    title = definition.check.detailsPage.title,
                    summary = definition.check.detailsPage.summary,
                    details = definition.check.detailsPage.details ?: ""
                )
            ),
            workflowFilename = definition.fileName
        )
    }
}

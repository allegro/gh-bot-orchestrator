package pl.allegro.tech.github.botorchestrator.domain.workflows.web

import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.ConcurrencyGroupType
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.MatchingStrategy

data class WorkflowDefinitionResponse(
    val id: String,
    val enabled: Boolean,
    val repository: RepositoryResponse,
    val ref: String,
    val maxConcurrentRuns: Int,
    val concurrencyGroup: ConcurrencyGroupType,
    val filters: FiltersResponse,
    val mapping: MappingResponse,
    val check: CheckResponse,
    val workflowFilename: String
) {

    data class RepositoryResponse(
        val name: String,
        val owner: String
    )

    data class FiltersResponse(
        val matchingStrategy: MatchingStrategy,
        val matchers: List<MatcherResponse>
    ) {

        sealed class MatcherResponse(
            val type: String
        )

        data class JsonPathMatcherResponse(
            val path: String,
            val regex: String
        ) : MatcherResponse("event")

        data class FileMatcherResponse(
            val file: String,
            val statuses: Set<String>
        ) : MatcherResponse("file")

        data class DependabotMatcherResponse(
            val artifactRegex: String
        ) : MatcherResponse("dependabot")
    }

    data class MappingResponse(
        val fields: List<String>
    )

    data class CheckResponse(
        val name: String,
        val detailsPage: DetailsPageResponse
    )

    data class DetailsPageResponse(
        val title: String,
        val summary: String,
        val details: String
    )
}

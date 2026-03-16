package pl.allegro.tech.common.pullrequestmanager.domain.workflows.web

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.ConcurrencyGroupType
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.MatchingStrategy

data class WorkflowDefinitionRequest(
    val enabled: Boolean,
    val repository: RepositoryRequest,
    val ref: String,
    val maxConcurrentRuns: Int,
    val concurrencyGroup: ConcurrencyGroupType,
    val filters: FiltersRequest,
    val mapping: MappingRequest,
    val check: CheckRequest,
    val workflowFilename: String
) {

    data class RepositoryRequest(
        val name: String,
        val owner: String
    )

    data class FiltersRequest(
        val matchingStrategy: MatchingStrategy,
        val matchers: List<MatcherRequest>
    )

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = JsonPathMatcherRequest::class, name = "event"),
        JsonSubTypes.Type(value = FileMatcherRequest::class, name = "file"),
        JsonSubTypes.Type(value = DependabotMatcherRequest::class, name = "dependabot")
    )
    sealed class MatcherRequest

    data class JsonPathMatcherRequest(
        val path: String,
        val regex: String
    ) : MatcherRequest()

    data class FileMatcherRequest(
        val file: String,
        val statuses: Set<String> = emptySet()
    ) : MatcherRequest()

    data class DependabotMatcherRequest(
        val artifactRegex: String
    ) : MatcherRequest()

    data class MappingRequest(
        val fields: List<String>
    )

    data class CheckRequest(
        val name: String,
        val detailsPage: DetailsPageRequest
    )

    data class DetailsPageRequest(
        val title: String,
        val summary: String,
        val details: String? = null
    )
}

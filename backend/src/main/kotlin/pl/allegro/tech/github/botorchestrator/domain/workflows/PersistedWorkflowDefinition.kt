package pl.allegro.tech.github.botorchestrator.domain.workflows

import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.MatchingStrategy
import java.time.Instant
import java.util.*

data class PersistedWorkflowDefinition(
    val id: UUID? = null,
    val enabled: Boolean,
    val repository: Repository,
    val ref: String,
    val concurrencyGroup: WorkflowDefinition.ConcurrencyGroupType,
    val filters: Filters,
    val mapping: Mapping,
    val check: Check,
    val maxConcurrentRuns: Int,
    val workflowFileName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {

    data class Repository(
        val owner: String,
        val name: String
    )

    data class Filters(
        val matchingStrategy: MatchingStrategy,
        val matchers: List<Matcher>,
    )

    sealed interface Matcher

    data class JsonPathMatcher(
        val path: String,
        val regex: String,
    ) : Matcher

    data class FileMatcher(
        val files: String,
        val statuses: Set<String> = emptySet()
    ) : Matcher

    data class DependabotMatcher(
        val artifactRegex: String
    ) : Matcher

    data class Mapping(val fields: List<String>)

    data class Check(
        val name: String,
        val detailsPage: DetailsPage
    ) {

        data class DetailsPage(
            val title: String,
            val summary: String,
            val details: String?
        )
    }
}





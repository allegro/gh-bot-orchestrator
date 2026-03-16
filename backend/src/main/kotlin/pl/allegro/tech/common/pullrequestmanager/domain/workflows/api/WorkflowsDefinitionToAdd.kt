package pl.allegro.tech.common.pullrequestmanager.domain.workflows.api

import pl.allegro.tech.common.pullrequestmanager.domain.PullRequestEventMapper
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.ConcurrencyGroup
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.PullRequestCheck
import java.time.Instant

data class WorkflowDefinitionToAdd(
    val repository: Repository,
    val enabled: Boolean,
    val ref: String,
    val concurrencyGroup: ConcurrencyGroup,
    val eventMapper: PullRequestEventMapper,
    val maxConcurrentRuns: Int,
    val check: PullRequestCheck,
    val fileName: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val filters: CompoundPullRequestMatcher,
)

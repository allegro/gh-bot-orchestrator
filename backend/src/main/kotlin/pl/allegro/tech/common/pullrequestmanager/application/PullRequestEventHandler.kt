package pl.allegro.tech.common.pullrequestmanager.application

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.allegro.tech.common.pullrequestmanager.api.PullRequestEvent
import pl.allegro.tech.common.pullrequestmanager.domain.GithubClient
import pl.allegro.tech.common.pullrequestmanager.domain.PullRequest
import pl.allegro.tech.common.pullrequestmanager.domain.WorkflowDispatcher
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.common.pullrequestmanager.infra.github.PullRequestFileDto

@Component
class PullRequestEventHandler(
    private val githubClient: GithubClient,
    private val workflowDispatcher: WorkflowDispatcher,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun handle(workflowDefinition: WorkflowDefinition, event: PullRequestEvent) {
        val pullRequest = event.toDomain(objectMapper)
        logger.debug { "Pull request: $pullRequest" }
        val pullRequestFiles = fetchPullRequestFilesIfNeeded(workflowDefinition, pullRequest)

        if (workflowDefinition.matches(pullRequest, pullRequestFiles)) {
            workflowDispatcher.dispatch(workflowDefinition, pullRequest)
        } else {
            logger.info { "Event did not match filters" }
        }
    }

    private fun fetchPullRequestFilesIfNeeded(
        config: WorkflowDefinition,
        pullRequest: PullRequest
    ): Sequence<PullRequestFileDto> = when {
        config.requiresPullRequestFiles() -> {
            githubClient.getPullRequestFiles(pullRequest.repository, pullRequest.number)
        }

        else -> emptySequence()
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

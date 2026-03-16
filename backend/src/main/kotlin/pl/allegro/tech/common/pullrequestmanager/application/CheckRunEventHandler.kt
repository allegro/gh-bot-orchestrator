package pl.allegro.tech.common.pullrequestmanager.application

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.common.pullrequestmanager.api.CheckRunEvent
import pl.allegro.tech.common.pullrequestmanager.domain.PullRequest
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.WorkflowDispatcher
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.allegro.tech.common.andamio.metrics.micrometer.Timed
import java.time.Instant

@Component
class CheckRunEventHandler(
    private val workflowDispatcher: WorkflowDispatcher,
    private val objectMapper: ObjectMapper,
) {
    @Timed(name = "check-run-event-handler")
    @Transactional
    fun handle(workflowDefinition: WorkflowDefinition, event: CheckRunEvent, pullRequestNumber: Int) {
        val lastUpdateTimestamp = Instant.parse(
            event.data.checkRun.completedAt
                ?: event.data.checkRun.startedAt
                ?: error("Check run has neither completed_at nor started_at")
        )
        val originalEventString = objectMapper.writeValueAsString(event.data)

        logger.atInfo {
            message = "Processing check run event for pull request"
            payload = mapOf(
                "pullRequestNumber" to pullRequestNumber,
                "repo" to event.data.repository.fullName,
                "checkRunId" to event.data.checkRun.id,
                "workflowId" to workflowDefinition.id
            )
        }

        val pullRequest = PullRequest(
            repository = Repository(event.data.repository.owner.login, event.data.repository.name),
            number = pullRequestNumber,
            headSha = event.data.checkRun.headSha,
            lastUpdateTimestamp = lastUpdateTimestamp,
            dependabotMetadata = null,
            originalEventString = originalEventString,
            customRepositoryProperties = event.data.repository.customProperties
        )

        if (workflowDefinition.matches(pullRequest, emptySequence())) {
            workflowDispatcher.dispatch(workflowDefinition, pullRequest)
        } else {
            logger.info { "Check run event did not match filters for pull request #$pullRequestNumber" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

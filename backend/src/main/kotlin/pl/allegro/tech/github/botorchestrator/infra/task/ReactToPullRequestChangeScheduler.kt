package pl.allegro.tech.github.botorchestrator.infra.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.SchedulerClient.ScheduleOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.allegro.tech.github.botorchestrator.api.PullRequestEvent
import pl.allegro.tech.github.botorchestrator.domain.workflows.WorkflowDefinitionsReader
import java.time.Clock
import java.util.*

@Component
class ReactToPullRequestChangeScheduler(
    private val clock: Clock,
    private val scheduler: SchedulerClient,
    private val workflowDefinitionsReader: WorkflowDefinitionsReader
) {

    // If any task insertion fails, the rethrown exception triggers @Transactional rollback
    // and propagates to the endpoint, resulting in 5xx so that Hermes retries the event.
    // We extend the scope of rollback to Exception.class to be consistent with the catching Exception.
    @Transactional(rollbackFor = [Exception::class])
    fun schedule(event: PullRequestEvent) {
        workflowDefinitionsReader.getDefinitions()
            .forEach { workflow ->
                try {
                    logger.atInfo {
                        message = "Scheduling workflow"
                        payload = mapOf(
                            "repo" to event.data.repository.fullName,
                            "pullRequest" to event.data.pullRequest.htmlUrl,
                            "pullRequestNumber" to event.data.pullRequest.number,
                            "workflowId" to workflow.id,
                            "workflow" to "${workflow.repository.fullName}/${workflow.fileName}@${workflow.ref}"
                        )
                    }

                    scheduler.schedule(
                        HANDLE_PULL_REQUEST
                            .instance("${event.delivery}@${workflow.id}")
                            .data(ReactToPullRequestChangeTaskData(workflow.id, event))
                            .build(),
                        clock.instant(),
                        ScheduleOptions.WHEN_EXISTS_DO_NOTHING
                    )
                } catch (e: Exception) {
                    logger.atError {
                        cause = e
                        message = "Failed to schedule task"
                        payload = mapOf(
                            "repo" to event.data.repository.fullName,
                            "pullRequestNumber" to event.data.pullRequest.number,
                            "pullRequest" to event.data.pullRequest.htmlUrl,
                            "workflowId" to workflow.id,
                            "workflow" to "${workflow.repository.fullName}/${workflow.fileName}@${workflow.ref}"
                        )
                    }
                    throw e
                }
            }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

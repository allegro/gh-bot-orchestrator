package pl.allegro.tech.github.botorchestrator.infra.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.SchedulerClient.ScheduleOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.allegro.tech.github.botorchestrator.api.CheckRunEvent
import pl.allegro.tech.github.botorchestrator.domain.workflows.WorkflowDefinitionsReader
import java.time.Clock
import java.util.UUID

@Component
class ReactToCheckRunScheduler(
    private val clock: Clock,
    private val scheduler: SchedulerClient,
    private val workflowDefinitionsReader: WorkflowDefinitionsReader
) {

    // If any task insertion fails, the rethrown exception triggers @Transactional rollback
    // and propagates to the endpoint, resulting in 5xx so that Hermes retries the event.
    // We extend the scope of rollback to Exception.class to be consistent with the catching Exception.
    @Transactional(rollbackFor = [Exception::class])
    fun schedule(event: CheckRunEvent) {
        val pullRequests = event.data.checkRun.pullRequests
        if (pullRequests.isEmpty()) {
            logger.info { "Check run has no pull requests, skipping" }
            return
        }

        workflowDefinitionsReader.getDefinitions()
            .forEach { workflow ->
                pullRequests.forEach { pullRequest ->
                    try {
                        logger.atInfo {
                            message = "Scheduling workflow"
                            payload = mapOf(
                                "repo" to event.data.repository.fullName,
                                "checkRunId" to event.data.checkRun.id,
                                "workflowId" to workflow.id,
                                "workflow" to "${workflow.repository.fullName}/${workflow.fileName}@${workflow.ref}",
                                "pullRequestNumber" to pullRequest.number
                            )
                        }

                        val delivery = event.delivery ?: event.data.checkRun.id.toString()
                        scheduler.schedule(
                            HANDLE_CHECK_RUN
                                .instance("${delivery}@${workflow.id}@${pullRequest.number}")
                                .data(ReactToCheckRunTaskData(workflow.id, pullRequest.number, event))
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
                                "checkRunId" to event.data.checkRun.id,
                                "workflowId" to workflow.id,
                                "workflow" to "${workflow.repository.fullName}/${workflow.fileName}@${workflow.ref}",
                                "pullRequestNumber" to pullRequest.number
                            )
                        }
                        throw e
                    }
                }
            }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

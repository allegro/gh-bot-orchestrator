package pl.allegro.tech.common.pullrequestmanager.infra.task

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.allegro.tech.common.pullrequestmanager.api.CheckRunEvent
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.WorkflowDefinitionsReader
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskAdder
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskId
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskMetadata
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskParams
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskSpecification
import java.time.Clock
import java.util.UUID

@Component
class ReactToCheckRunScheduler(
    private val clock: Clock,
    private val taskAdder: TaskAdder,
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

                        taskAdder.addTask(taskBasedOn(workflow.id, pullRequest.number, event))
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

    private fun taskBasedOn(workflowId: UUID, pullRequestNumber: Int, event: CheckRunEvent): TaskSpecification<ReactToCheckRunTaskData> {
        val delivery = event.delivery ?: event.data.checkRun.id.toString()
        return TaskSpecification(
            id = TaskId("${delivery}@${workflowId}@${pullRequestNumber}"),
            name = HANDLE_CHECK_RUN,
            params = TaskParams(ReactToCheckRunTaskData(workflowId, pullRequestNumber, event)),
            scheduleAt = clock.instant(),
            meta = TaskMetadata()
        )
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

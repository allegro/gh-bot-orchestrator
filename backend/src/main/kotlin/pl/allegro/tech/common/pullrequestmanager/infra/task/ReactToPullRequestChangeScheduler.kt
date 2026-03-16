package pl.allegro.tech.common.pullrequestmanager.infra.task

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.allegro.tech.common.pullrequestmanager.api.PullRequestEvent
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.WorkflowDefinitionsReader
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskAdder
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskId
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskMetadata
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskParams
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskSpecification
import java.time.Clock
import java.util.*

@Component
class ReactToPullRequestChangeScheduler(
    private val clock: Clock,
    private val taskAdder: TaskAdder,
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

                    taskAdder.addTask(taskBasedOn(workflow.id, event))
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

    private fun taskBasedOn(workflowId: UUID, event: PullRequestEvent): TaskSpecification<ReactToPullRequestChangeTaskData> {
        return TaskSpecification(
            id = TaskId("${event.delivery}@$workflowId"),
            name = HANDLE_PULL_REQUEST,
            params = TaskParams(ReactToPullRequestChangeTaskData(workflowId, event)),
            scheduleAt = clock.instant(),
            meta = TaskMetadata()
        )
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

package pl.allegro.tech.github.botorchestrator.infra.task

import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import pl.allegro.tech.github.botorchestrator.api.PullRequestEvent
import java.util.UUID

val HANDLE_PULL_REQUEST: TaskDescriptor<ReactToPullRequestChangeTaskData> =
    TaskDescriptor.of("handle-pull-request", ReactToPullRequestChangeTaskData::class.java)

data class ReactToPullRequestChangeTaskData(val workflowId: UUID, val event: PullRequestEvent)

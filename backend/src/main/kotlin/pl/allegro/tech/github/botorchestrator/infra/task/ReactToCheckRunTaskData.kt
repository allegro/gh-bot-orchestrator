package pl.allegro.tech.github.botorchestrator.infra.task

import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import pl.allegro.tech.github.botorchestrator.api.CheckRunEvent
import java.util.UUID

val HANDLE_CHECK_RUN: TaskDescriptor<ReactToCheckRunTaskData> =
    TaskDescriptor.of("handle-check-run", ReactToCheckRunTaskData::class.java)

data class ReactToCheckRunTaskData(val workflowId: UUID, val pullRequestNumber: Int, val event: CheckRunEvent)

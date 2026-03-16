package pl.allegro.tech.common.pullrequestmanager.infra.task

import com.github.kagkarlsson.scheduler.task.TaskDescriptor
import pl.allegro.tech.common.pullrequestmanager.api.CheckRunEvent
import java.util.UUID

val HANDLE_CHECK_RUN: TaskDescriptor<ReactToCheckRunTaskData> =
    TaskDescriptor.of("handle-check-run", ReactToCheckRunTaskData::class.java)

data class ReactToCheckRunTaskData(val workflowId: UUID, val pullRequestNumber: Int, val event: CheckRunEvent)

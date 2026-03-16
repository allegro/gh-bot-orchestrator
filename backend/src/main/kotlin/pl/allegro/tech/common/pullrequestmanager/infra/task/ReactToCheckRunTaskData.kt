package pl.allegro.tech.common.pullrequestmanager.infra.task

import pl.allegro.tech.common.pullrequestmanager.api.CheckRunEvent
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskName
import java.util.UUID

val HANDLE_CHECK_RUN = TaskName("handle-check-run")

data class ReactToCheckRunTaskData(val workflowId: UUID, val pullRequestNumber: Int, val event: CheckRunEvent)

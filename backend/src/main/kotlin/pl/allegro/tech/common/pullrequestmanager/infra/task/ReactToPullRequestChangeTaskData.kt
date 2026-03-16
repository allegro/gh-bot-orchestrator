package pl.allegro.tech.common.pullrequestmanager.infra.task

import pl.allegro.tech.common.pullrequestmanager.api.PullRequestEvent
import pl.allegro.tech.tech.postgrestaskscheduler.api.TaskName
import java.util.UUID

val HANDLE_PULL_REQUEST = TaskName("handle-pull-request")

data class ReactToPullRequestChangeTaskData(val workflowId: UUID, val event: PullRequestEvent) {
}

package pl.allegro.tech.github.botorchestrator.api

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.allegro.tech.github.botorchestrator.application.WorkflowRunFinishedEventHandler
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.SlotId
import pl.allegro.tech.github.botorchestrator.domain.workflows.WorkflowDefinitionsReader
import java.util.*

@RestController
@RequestMapping("workflow-runs")
class WorkflowRunsEndpoint(
    private val handler: WorkflowRunFinishedEventHandler,
    private val workflowDefinitionsReader: WorkflowDefinitionsReader
) {

    @PostMapping
    fun handle(@RequestBody event: WorkflowRunFinishedEvent) {
        logger.debug { "Received event: $event" }
        val workflowId = workflowDefinitionsReader.exists(event.repository, event.ref, event.path)
        if (!workflowId) {
            // nothing to do
            return
        }
        if (event.action == "completed") {
            handler.handle(event.slotId, event)
        } else {
            logger.warn { "No workflow label found for $event" }
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}

data class WorkflowRunFinishedEvent(
    val event: WorkflowRunFinishedEventDto
) {

    val slotId: SlotId = UUID.fromString(event.workflowRun.displayTitle.substringAfterLast("| "))
    val repository: Repository = Repository(event.workflowRun.repository.owner.login, event.workflowRun.repository.name)
    val ref = event.workflowRun.headBranch
    val path = event.workflowRun.path
    val action = event.action
}

data class WorkflowRunFinishedEventDto(
    @JsonProperty("workflow_run") val workflowRun: WorkflowRunDto,
    val action: String
)

data class WorkflowRunDto(
    val id: String,
    @JsonProperty("head_branch") val headBranch: String,
    val repository: RepositoryDto,
    val path: String,
    @JsonProperty("display_title") val displayTitle: String,
    val conclusion: String,
)

data class RepositoryDto(
    val name: String,
    val owner: OwnerDto
)

data class OwnerDto(
    val login: String
)

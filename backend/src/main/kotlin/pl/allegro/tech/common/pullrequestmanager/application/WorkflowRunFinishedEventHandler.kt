package pl.allegro.tech.common.pullrequestmanager.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.MDC
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.allegro.tech.common.andamio.metrics.micrometer.Timed
import pl.allegro.tech.common.pullrequestmanager.api.CheckRunUpdateRequest.DetailsPageUpdateRequest
import pl.allegro.tech.common.pullrequestmanager.api.WorkflowRunFinishedEvent
import pl.allegro.tech.common.pullrequestmanager.domain.AvailableSlots
import pl.allegro.tech.common.pullrequestmanager.domain.GithubClient
import pl.allegro.tech.common.pullrequestmanager.domain.SlotContext
import pl.allegro.tech.common.pullrequestmanager.domain.SlotId
import pl.allegro.tech.common.pullrequestmanager.domain.comment.CommentCallbackUrlRepository
import java.util.UUID

@Component
class WorkflowRunFinishedEventHandler(
    private val availableSlots: AvailableSlots,
    private val callbackUrlRepository: CommentCallbackUrlRepository,
    private val githubClient: GithubClient,
) {

    @Timed(name = "workflow-finished-event-handler")
    @Transactional
    fun handle(slotId: SlotId, event: WorkflowRunFinishedEvent) { // todo: fix packages to not have dependnecy on API
        try {
            event.toMdc()
            logger.info { "Processing workflow finish event, status: ${event.event.workflowRun.conclusion}" }
            availableSlots.releaseSlot(slotId)
            callbackUrlRepository.removeBySlotId(slotId)

            if (event.event.workflowRun.conclusion in cancelledStates) {
                handleCancelledWorkflow(slotId)
            }
        } catch (e: DuplicateKeyException) {
            logger.info { "Duplicated key for run with id: $slotId" }
        } catch (e: Exception) {
            logger.error(e) { "Can't process finished workflow ${event.event.workflowRun.displayTitle}" }
        } finally {
            clearMdc()
        }
    }

    private fun handleCancelledWorkflow(slotId: SlotId) {
        logger.info { "Check has to be cancelled" }
        val slotContext = availableSlots.getContext(slotId)
        if (slotContext == null) {
            logger.warn { "No slot found for id: $slotId" }
            return
        } else slotContext.toMdc()

        val details = DetailsPageUpdateRequest(slotContext.checkTitle, "Workflow was cancelled by GitHub")
        githubClient.updateCheck(
            checkId = slotContext.checkRunId,
            owner = slotContext.repository.id.owner,
            repo = slotContext.repository.id.name,
            conclusion = "skipped",
            detailsPage = details
        )
    }

    private fun WorkflowRunFinishedEvent.toMdc() {
        MDC.put("workflowTitle", event.workflowRun.displayTitle)
        MDC.put("workflowRepo", event.workflowRun.repository.name)
        MDC.put("workflowPath", event.workflowRun.path)
    }

    private fun SlotContext.toMdc() {
        MDC.put("repo", repository.fullName)
        MDC.put("pullRequestNumber", pullRequestNumber.toString())
    }

    private fun clearMdc() {
        MDC.remove("repo")
        MDC.remove("pullRequestNumber")
        MDC.remove("workflowTitle")
        MDC.remove("workflowRepo")
        MDC.remove("workflowPath")
    }

    companion object {

        val logger = KotlinLogging.logger {}

        private val cancelledStates = setOf("cancelled", "stale", "timed_out")
    }
}

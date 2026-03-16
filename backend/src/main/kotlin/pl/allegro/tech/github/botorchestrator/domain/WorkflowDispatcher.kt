package pl.allegro.tech.github.botorchestrator.domain

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import pl.allegro.tech.github.botorchestrator.domain.comment.CommentUrlGenerator
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.PullRequestCheck

@Component
class WorkflowDispatcher(
    private val githubClient: GithubClient,
    private val urlProvider: UrlProvider,
    private val availableSlots: AvailableSlots,
    private val commentUrlGenerator: CommentUrlGenerator,
) {
    /**
     * Dispatches a GitHub Actions workflow for a given pull request.
     *
     * Acquires a concurrency slot, creates a corresponding check run in GitHub, and triggers the workflow dispatch.
     * If the dispatch fails, the check run is marked as failed and the slot is released.
     *
     * @param workflowDefinition defines the workflow to dispatch, including filters, concurrency group, and check configuration
     * @param pullRequest the pull request that triggered the dispatch
     */
    fun dispatch(workflowDefinition: WorkflowDefinition, pullRequest: PullRequest) {
        val concurrencyGroup = workflowDefinition.concurrencyGroup.generateJobId(pullRequest)
        availableSlots.releaseSlotBy(workflowDefinition.id, concurrencyGroup, pullRequest.lastUpdateTimestamp)
        val slot = availableSlots.takeAvailableSlot(workflowDefinition.id, concurrencyGroup, pullRequest.lastUpdateTimestamp)

        when (val check = createCheck(workflowDefinition.check, pullRequest, slot.id)) {
            is CheckDetails -> {
                logger.info { "Check ${check.checkId} created" }
                try {
                    dispatchWorkflowWithRetry(workflowDefinition, pullRequest, slot, check.callbackUrl(urlProvider))
                } catch (e: Exception) {
                    logger.error(e) { "Failed to dispatch workflow ${workflowDefinition.fileName} for ${pullRequest.fullName()}, marking check as timed_out and releasing slot" }
                    failCheckAndReleaseSlot(check, pullRequest, slot.id)
                    return
                }
                val context = SlotContext(
                    slotId = slot.id,
                    repository = pullRequest.repository,
                    checkRunId = check.checkId,
                    ref = pullRequest.headSha,
                    checkTitle = workflowDefinition.check.name,
                    pullRequestNumber = pullRequest.number.toLong()
                )
                availableSlots.saveContext(slot.id, context)
            }

            is FailedToCreateCheck -> {
                availableSlots.releaseSlot(slot.id)
                logger.warn { "Unable to handle event for ${pullRequest.fullName()}. Failed creating check: ${check.reason}" }
            }
        }
    }

    private fun createCheck(check: PullRequestCheck, pullRequest: PullRequest, slotId: SlotId): CreateCheckRunResult {
        return githubClient.createCheck(
            repository = pullRequest.repository,
            headSha = pullRequest.headSha,
            name = check.name,
            detailsPage = check.detailsPage,
            externalId = slotId.toString()
        )
    }

    private fun dispatchWorkflowWithRetry(
        workflowDefinition: WorkflowDefinition,
        pullRequest: PullRequest,
        slot: TakenSlot,
        callbackUrl: String?,
    ) {
        repeat(DISPATCH_MAX_RETRIES) { attempt ->
            try {
                dispatchWorkflow(workflowDefinition, pullRequest, slot, callbackUrl)
                return
            } catch (e: Exception) {
                if (attempt == DISPATCH_MAX_RETRIES - 1) throw e
                logger.warn(e) { "Dispatch attempt ${attempt + 1}/$DISPATCH_MAX_RETRIES failed, retrying in ${DISPATCH_RETRY_DELAY_MS}ms" }
                Thread.sleep(DISPATCH_RETRY_DELAY_MS)
            }
        }
    }

    private fun dispatchWorkflow(
        workflowDefinition: WorkflowDefinition,
        pullRequest: PullRequest,
        slot: TakenSlot,
        callbackUrl: String? = null,
    ) {
        logger.info { "Preparing ${workflowDefinition.fileName} workflow dispatch for ${pullRequest.fullName()}" }
        val workflow = Workflow(
            repository = workflowDefinition.repository.id,
            ref = workflowDefinition.ref,
            workflowFileName = workflowDefinition.fileName
        )
        val eventMapper = workflowDefinition.eventMapper
        val concurrencyGroup = workflowDefinition.concurrencyGroup.generateJobId(pullRequest)
        val mappedEvent = eventMapper.map(pullRequest)

        val commentUrl = commentUrlGenerator.generate(
            workflowRepository = workflowDefinition.repository,
            targetRepository = pullRequest.repository,
            pullRequestNumber = pullRequest.number,
            slotId = slot.id
        )

        githubClient.dispatchWorkflow(
            workflow = workflow,
            inputs = WorkflowInputs(
                event = mappedEvent,
                concurrencyGroup = "${workflowDefinition.id}-$concurrencyGroup",
                checkCallbackUrl = callbackUrl,
                slotId = "${pullRequest.fullName()} | ${slot.id}",
                workflowConcurrencyGroup = "${workflowDefinition.id}-${slot.workflowConcurrencyGroup}",
                commentCallbackUrl = commentUrl,
                extraParams = ExtraParams(
                    dependabot = pullRequest.dependabotMetadata?.versionUpdates ?: emptyList(),
                    githubCloneToken = githubClient.getWorkflowToken(pullRequest.repository)
                )
            )
        )

        logger.info { "Successfully dispatched workflow ${workflow.workflowFileName}." }
    }

    private fun failCheckAndReleaseSlot(check: CheckDetails, pullRequest: PullRequest, slotId: SlotId) {
        try {
            githubClient.updateCheck(
                checkId = check.checkId,
                owner = pullRequest.repository.id.owner,
                repo = pullRequest.repository.id.name,
                conclusion = "timed_out",
                detailsPage = null
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to update check ${check.checkId} to timed_out — check may remain orphaned in_progress" }
        }
        try {
            availableSlots.releaseSlot(slotId)
        } catch (e: Exception) {
            logger.error(e) { "Failed to release slot $slotId — slot may leak until 24h cleanup" }
        }
    }

    companion object {
        private const val DISPATCH_MAX_RETRIES = 3
        private const val DISPATCH_RETRY_DELAY_MS = 2000L

        private val logger = KotlinLogging.logger { }
    }
}

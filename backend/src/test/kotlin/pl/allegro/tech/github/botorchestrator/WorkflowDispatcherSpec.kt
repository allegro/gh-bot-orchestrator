package pl.allegro.tech.github.botorchestrator

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.allegro.tech.github.botorchestrator.PullRequestFixture.pullRequest
import pl.allegro.tech.github.botorchestrator.WorkflowDefinitionFixture.randomWorkflowDefinition
import pl.allegro.tech.github.botorchestrator.domain.WorkflowDispatcher
import pl.allegro.tech.github.botorchestrator.domain.AvailableSlots
import pl.allegro.tech.github.botorchestrator.domain.CheckDetails
import pl.allegro.tech.github.botorchestrator.domain.FailedToCreateCheck
import pl.allegro.tech.github.botorchestrator.domain.GithubClient
import pl.allegro.tech.github.botorchestrator.domain.TakenSlot
import pl.allegro.tech.github.botorchestrator.domain.UrlProvider
import pl.allegro.tech.github.botorchestrator.domain.comment.CommentUrlGenerator
import java.time.Instant
import java.util.UUID

class WorkflowDispatcherSpec : FunSpec() {
    private val githubClient = mockk<GithubClient>(relaxed = true)
    private val availableSlots = mockk<AvailableSlots>(relaxed = true)
    private val urlProvider = mockk<UrlProvider>(relaxed = true)
    private val commentUrlGenerator = mockk<CommentUrlGenerator>(relaxed = true)

    private val dispatcher = WorkflowDispatcher(githubClient, urlProvider, availableSlots, commentUrlGenerator)

    private val takenSlot = TakenSlot(
        id = UUID.randomUUID(),
        workflowConcurrencyGroup = 1,
        changeTimestamp = Instant.now()
    )

    init {
        beforeTest {
            every { availableSlots.takeAvailableSlot(any(), any(), any()) } returns takenSlot
        }

        test("should update check to timed_out and release slot when workflow dispatch throws") {
            // given
            val workflowDefinition = randomWorkflowDefinition()
            val pullRequest = pullRequest()
            val checkId = "check-123"

            every { githubClient.createCheck(any(), any(), any(), any(), any()) } returns
                CheckDetails(pullRequest.repository, checkId)
            every { githubClient.dispatchWorkflow(any(), any()) } throws RuntimeException("dispatch failed")

            // when
            shouldNotThrow<Exception> {
                dispatcher.dispatch(workflowDefinition, pullRequest)
            }

            // then — dispatch retried 3 times
            verify(exactly = 3) { githubClient.dispatchWorkflow(any(), any()) }
            // and check updated to timed_out
            verify(exactly = 1) {
                githubClient.updateCheck(
                    checkId = checkId,
                    owner = pullRequest.repository.id.owner,
                    repo = pullRequest.repository.id.name,
                    conclusion = "timed_out",
                    detailsPage = null
                )
            }
            // and slot released
            verify(exactly = 1) { availableSlots.releaseSlot(takenSlot.id) }
        }

        test("should retry and succeed when workflow dispatch fails temporarily") {
            // given
            val workflowDefinition = randomWorkflowDefinition()
            val pullRequest = pullRequest()

            every { githubClient.createCheck(any(), any(), any(), any(), any()) } returns
                CheckDetails(pullRequest.repository, "check-retry")
            every { githubClient.dispatchWorkflow(any(), any()) } throws
                RuntimeException("temporary failure") andThenThrows
                RuntimeException("temporary failure") andThen Unit

            // when
            shouldNotThrow<Exception> {
                dispatcher.dispatch(workflowDefinition, pullRequest)
            }

            // then — dispatch called 3 times (2 failures + 1 success)
            verify(exactly = 3) { githubClient.dispatchWorkflow(any(), any()) }
            // and context saved (success path)
            verify(exactly = 1) { availableSlots.saveContext(takenSlot.id, any()) }
            // and check NOT marked as timed_out
            verify(exactly = 0) { githubClient.updateCheck(any(), any(), any(), any(), any()) }
        }

        test("should release slot and not dispatch when check creation fails") {
            // given
            val workflowDefinition = randomWorkflowDefinition()
            val pullRequest = pullRequest()

            every { githubClient.createCheck(any(), any(), any(), any(), any()) } returns
                FailedToCreateCheck(reason = "GitHub returned 422")

            // when
            shouldNotThrow<Exception> {
                dispatcher.dispatch(workflowDefinition, pullRequest)
            }

            // then — slot released
            verify(exactly = 1) { availableSlots.releaseSlot(takenSlot.id) }
            // and no workflow dispatch
            verify(exactly = 0) { githubClient.dispatchWorkflow(any(), any()) }
        }

        test("should save slot context after successful dispatch") {
            // given
            val workflowDefinition = randomWorkflowDefinition()
            val pullRequest = pullRequest()

            every { githubClient.createCheck(any(), any(), any(), any(), any()) } returns
                CheckDetails(pullRequest.repository, "check-456")

            // when
            shouldNotThrow<Exception> {
                dispatcher.dispatch(workflowDefinition, pullRequest)
            }

            // then
            verify(exactly = 1) { availableSlots.saveContext(takenSlot.id, any()) }
            verify(exactly = 0) { githubClient.updateCheck(any(), any(), any(), any(), any()) }
        }

        test("should still release slot when updateCheck throws during failure handling") {
            // given
            val workflowDefinition = randomWorkflowDefinition()
            val pullRequest = pullRequest()

            every { githubClient.createCheck(any(), any(), any(), any(), any()) } returns
                CheckDetails(pullRequest.repository, "check-789")
            every { githubClient.dispatchWorkflow(any(), any()) } throws RuntimeException("dispatch failed")
            every { githubClient.updateCheck(any(), any(), any(), any(), any()) } throws RuntimeException("update check failed")

            // when / then — should not propagate exception
            shouldNotThrow<Exception> {
                dispatcher.dispatch(workflowDefinition, pullRequest)
            }

            // then — dispatch retried 3 times
            verify(exactly = 3) { githubClient.dispatchWorkflow(any(), any()) }
            // and slot still released despite updateCheck failure
            verify(exactly = 1) { availableSlots.releaseSlot(takenSlot.id) }
        }

        test("should not propagate exception when releaseSlot throws during failure handling") {
            // given
            val workflowDefinition = randomWorkflowDefinition()
            val pullRequest = pullRequest()

            every { githubClient.createCheck(any(), any(), any(), any(), any()) } returns
                CheckDetails(pullRequest.repository, "check-999")
            every { githubClient.dispatchWorkflow(any(), any()) } throws RuntimeException("dispatch failed")
            every { availableSlots.releaseSlot(any()) } throws RuntimeException("release slot failed")

            // when / then — should not propagate exception
            shouldNotThrow<Exception> {
                dispatcher.dispatch(workflowDefinition, pullRequest)
            }

            // then — dispatch retried 3 times
            verify(exactly = 3) { githubClient.dispatchWorkflow(any(), any()) }
        }
    }
}

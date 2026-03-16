package pl.allegro.tech.github.botorchestrator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import pl.allegro.tech.github.botorchestrator.CheckRunEventFixture.checkRunEvent
import pl.allegro.tech.github.botorchestrator.WorkflowDefinitionFixture.randomWorkflowDefinition
import pl.allegro.tech.github.botorchestrator.application.CheckRunEventHandler
import pl.allegro.tech.github.botorchestrator.domain.WorkflowDispatcher
import pl.allegro.tech.github.botorchestrator.domain.AvailableSlots
import pl.allegro.tech.github.botorchestrator.domain.GithubClient
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.UrlProvider
import pl.allegro.tech.github.botorchestrator.domain.comment.CommentUrlGenerator
import pl.allegro.tech.github.botorchestrator.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.MatchEverything
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.MatchingStrategy.ALL
import java.time.Instant

class CheckRunEventHandlerSpec : FunSpec() {

    private val githubClient = mockk<GithubClient>(relaxed = true)
    private val availableSlots = mockk<AvailableSlots>(relaxed = true)
    private val urlProvider = mockk<UrlProvider>(relaxed = true)
    private val commentUrlGenerator = mockk<CommentUrlGenerator>(relaxed = true)
    private val objectMapper = jacksonObjectMapper()

    private val workflowDispatcher = WorkflowDispatcher(githubClient, urlProvider, availableSlots, commentUrlGenerator)
    private val handler = CheckRunEventHandler(workflowDispatcher, objectMapper)

    init {
        test("should create check and dispatch workflow when event matches filters") {
            // given
            val workflowDefinition = workflowDefinitionMatchingEverything()
            val event = checkRunEvent()

            // when
            handler.handle(workflowDefinition, event, pullRequestNumber = 42)

            // then
            verify(exactly = 1) {
                githubClient.createCheck(
                    repository = Repository("some-owner", "some-repository"),
                    headSha = "d1a6c13bff04dc0f090f97c57a61ddd9f8af1816",
                    name = workflowDefinition.check.name,
                    detailsPage = workflowDefinition.check.detailsPage,
                    externalId = any()
                )
            }
            verify(exactly = 1) { githubClient.dispatchWorkflow(any(), any()) }
        }

        test("should not dispatch workflow when filters do not match") {
            // given
            val workflowDefinition = randomWorkflowDefinition {
                filters { jsonPathFilter("$.check_run.conclusion", "^success$") }
            }
            val event = checkRunEvent(conclusion = "failure")

            // when
            handler.handle(workflowDefinition, event, pullRequestNumber = 42)

            // then
            verify(exactly = 0) { githubClient.createCheck(any(), any(), any(), any(), any()) }
            verify(exactly = 0) { githubClient.dispatchWorkflow(any(), any()) }
        }

        test("should use completedAt as lastUpdateTimestamp when present") {
            // given
            val workflowDefinition = workflowDefinitionMatchingEverything()
            val timestampSlot = slot<Instant>()
            val event = checkRunEvent(completedAt = "2025-06-01T12:00:00Z", startedAt = "2025-06-01T11:00:00Z")

            // when
            handler.handle(workflowDefinition, event, pullRequestNumber = 42)

            // then
            verify(exactly = 1) { availableSlots.releaseSlotBy(any(), any(), capture(timestampSlot)) }
            timestampSlot.captured shouldBe Instant.parse("2025-06-01T12:00:00Z")
        }

        test("should fall back to startedAt when completedAt is null") {
            // given
            val workflowDefinition = workflowDefinitionMatchingEverything()
            val timestampSlot = slot<Instant>()
            val event = checkRunEvent(completedAt = null, startedAt = "2025-06-01T11:00:00Z")

            // when
            handler.handle(workflowDefinition, event, pullRequestNumber = 42)

            // then
            verify(exactly = 1) { availableSlots.releaseSlotBy(any(), any(), capture(timestampSlot)) }
            timestampSlot.captured shouldBe Instant.parse("2025-06-01T11:00:00Z")
        }

        test("should throw when both completedAt and startedAt are null") {
            val workflowDefinition = workflowDefinitionMatchingEverything()
            val event = checkRunEvent(completedAt = null, startedAt = null)

            // when / then
            shouldThrow<IllegalStateException> {
                handler.handle(workflowDefinition, event, pullRequestNumber = 42)
            }
        }
    }

    private fun workflowDefinitionMatchingEverything() = randomWorkflowDefinition().copy(
        filters = CompoundPullRequestMatcher(ALL, listOf(MatchEverything))
    )
}

package pl.allegro.tech.common.pullrequestmanager

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.allegro.tech.common.pullrequestmanager.PullRequestEventFixture.pullRequestEvent
import pl.allegro.tech.common.pullrequestmanager.PullRequestFixture.pullRequest
import pl.allegro.tech.common.pullrequestmanager.WorkflowDefinitionFixture.randomWorkflowDefinition
import pl.allegro.tech.common.pullrequestmanager.api.PullRequestEvent
import pl.allegro.tech.common.pullrequestmanager.application.PullRequestEventHandler
import pl.allegro.tech.common.pullrequestmanager.domain.WorkflowDispatcher
import pl.allegro.tech.common.pullrequestmanager.domain.AvailableSlots
import pl.allegro.tech.common.pullrequestmanager.domain.GithubClient
import pl.allegro.tech.common.pullrequestmanager.domain.PullRequest
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.UrlProvider
import pl.allegro.tech.common.pullrequestmanager.domain.Workflow
import pl.allegro.tech.common.pullrequestmanager.domain.comment.CommentUrlGenerator
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.FilePathMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.MatchEverything
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.MatchingStrategy.ALL
import pl.allegro.tech.common.pullrequestmanager.infra.github.PullRequestFileDto

class PullRequestHandlerSpec : FunSpec() {

    private val githubClient = mockk<GithubClient>(relaxed = true)
    private val availableSlots = mockk<AvailableSlots>(relaxed = true)
    private val urlProvider = mockk<UrlProvider>(relaxed = true)
    private val commentUrlGenerator = mockk<CommentUrlGenerator>(relaxed = true)
    private val objectMapper = jacksonObjectMapper()

    private val workflowDispatcher = WorkflowDispatcher(githubClient, urlProvider, availableSlots, commentUrlGenerator)

    init {

        test("should create check and dispatch workflow") {
            // given
            val handler = PullRequestEventHandler(githubClient, workflowDispatcher, ObjectMapper())
            val event = pullRequestEvent()
            val config = randomWorkflowDefinition().copy(filters = CompoundPullRequestMatcher(ALL, listOf(MatchEverything)))

            // when
            handler.handle(config, event)

            // then
            checkHasBeenCreated(config, event)
            commentHasBeenGenerated(config, event)
            workflowHasBeenDispatched(config, pullRequest())
        }

        test("should not trigger workflow that does not match filters") {
            // given
            val event = pullRequestEvent { withAction("synchronize") }
            val workflowConfig = randomWorkflowDefinition {
                filters {
                    jsonPathFilter("$.action", "^opened$")
                }
            }

            // and
            val handler = PullRequestEventHandler(githubClient, workflowDispatcher, ObjectMapper())

            // when
            handler.handle(workflowConfig, event)

            // then
            noInteractionWithGithubHappened()
        }

        test("should run workflow when file names match those in path filter") {
            // given
            val handler = PullRequestEventHandler(githubClient, workflowDispatcher, ObjectMapper())
            val event = pullRequestEvent()
            val config = randomWorkflowDefinition().copy(
                filters = CompoundPullRequestMatcher(ALL, listOf(FilePathMatcher("src/main/kotlin/.+\\.kt", setOf("added"))))
            )

            every {
                githubClient.getPullRequestFiles(
                    Repository(event.data.repository.owner.login, event.data.repository.name),
                    event.data.pullRequest.number
                )
            } returns sequenceOf(
                PullRequestFileDto("src/main/kotlin/SomeFile.kt", "added"),
                PullRequestFileDto("src/test/kotlin/SomeTest.kt", "modified")
            )

            // when
            handler.handle(config, event)

            // then
            checkHasBeenCreated(config, event)
            githubPullRequestFilesWereFetched(event)
            workflowHasBeenDispatched(config, pullRequest())
        }

        test("should skip workflow if none of the file names matches path filter") {
            // given
            val handler = PullRequestEventHandler(githubClient, workflowDispatcher, ObjectMapper())
            val event = pullRequestEvent()
            val config = randomWorkflowDefinition().copy(
                filters = CompoundPullRequestMatcher(ALL, listOf(FilePathMatcher("src/main/kotlin/.+\\.kt", setOf("added"))))
            )

            every {
                githubClient.getPullRequestFiles(
                    Repository(event.data.repository.owner.login, event.data.repository.name),
                    event.data.pullRequest.number
                )
            } returns sequenceOf(
                PullRequestFileDto("src/main/java/SomeFile.java", "added"),
            )

            // when
            handler.handle(config, event)

            // then
            githubPullRequestFilesWereFetched(event)
            noInteractionWithGithubHappened()
        }

        test("should skip workflow if file names matches path filter but the file had different status") {
            // given
            val handler = PullRequestEventHandler(githubClient, workflowDispatcher, ObjectMapper())
            val event = pullRequestEvent()
            val config = randomWorkflowDefinition().copy(
                filters = CompoundPullRequestMatcher(ALL, listOf(FilePathMatcher("src/main/kotlin/.+\\.kt", setOf("added"))))
            )

            every {
                githubClient.getPullRequestFiles(
                    Repository(event.data.repository.owner.login, event.data.repository.name),
                    event.data.pullRequest.number
                )
            } returns sequenceOf(
                PullRequestFileDto("src/main/java/SomeFile.java", "removed"),
            )

            // when
            handler.handle(config, event)

            // then
            githubPullRequestFilesWereFetched(event)
            noInteractionWithGithubHappened()
        }
    }

    private fun noInteractionWithGithubHappened() {
        verify(exactly = 0) { githubClient.dispatchWorkflow(any(), any()) }
    }

    private fun commentHasBeenGenerated(workflowDefinition: WorkflowDefinition, event: PullRequestEvent) {
        verify(exactly = 1) {
            commentUrlGenerator.generate(
                workflowDefinition.repository,
                Repository(id = Repository.Id(event.data.repository.owner.login, event.data.repository.name)),
                event.data.pullRequest.number,
                any()
            )
        }
    }

    private fun workflowHasBeenDispatched(workflowDefinition: WorkflowDefinition, pullRequest: PullRequest) {
        verify(exactly = 1) {
            githubClient.dispatchWorkflow(
                workflow = Workflow(
                    repository = workflowDefinition.repository.id,
                    ref = workflowDefinition.ref,
                    workflowFileName = workflowDefinition.fileName
                ),
                inputs = match {
                    objectMapper.readValue<Any>(it.event) == objectMapper.readValue<Any>(pullRequest.originalEventString)
                        && it.concurrencyGroup == "${workflowDefinition.id}-${workflowDefinition.concurrencyGroup.generateJobId(pullRequest)}"
                }
            )
        }
    }

    private fun checkHasBeenCreated(workflowDefinition: WorkflowDefinition, event: PullRequestEvent) {
        verify(exactly = 1) {
            githubClient.createCheck(
                repository = with(event.data.repository) { Repository(owner.login, name) },
                headSha = event.data.pullRequest.head.sha,
                name = workflowDefinition.check.name,
                detailsPage = workflowDefinition.check.detailsPage,
                externalId = any()
            )
        }
    }

    private fun githubPullRequestFilesWereFetched(event: PullRequestEvent) {
        verify(exactly = 1) {
            githubClient.getPullRequestFiles(
                Repository(event.data.repository.owner.login, event.data.repository.name),
                event.data.pullRequest.number
            )
        }
    }
}

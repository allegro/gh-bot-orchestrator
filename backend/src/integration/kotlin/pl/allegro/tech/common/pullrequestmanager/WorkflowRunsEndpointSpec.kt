package pl.allegro.tech.common.pullrequestmanager

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pl.allegro.tech.common.pullrequestmanager.domain.AvailableSlots
import pl.allegro.tech.common.pullrequestmanager.domain.NoSlotsAvailable
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.SlotContext
import pl.allegro.tech.common.pullrequestmanager.domain.comment.CommentCallbackUrlRepository
import pl.allegro.tech.common.pullrequestmanager.domain.comment.CommentUrlGenerator
import java.time.Instant
import java.util.*

class WorkflowRunsEndpointSpec(
    private val availableSlots: AvailableSlots,
    private val commentCallbackGenerator: CommentUrlGenerator,
    private val commentCallbackRepository: CommentCallbackUrlRepository,
    private val rest: MockMvc
) : BaseIntegrationSpec() {

    init {

        test("should bump available slots") {
            // given
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()

            takeAvailableSlot(someWorkflow.id)
            val slotId = takeAvailableSlot(someWorkflow.id)

            // expect
            noSlotsAvailable(someWorkflow.id)

            // when
            rest.post("/workflow-runs") {
                contentType = MediaType.APPLICATION_JSON
                content = exampleWorkflowRunEvent(slotId)
            }.andExpect {
                status { isOk() }
            }

            // then
            takeAvailableSlot(someWorkflow.id)

            // and then
            noSlotsAvailable(someWorkflow.id)
        }

        test("should ignore duplicated event") {
            // given
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            takeAvailableSlot(someWorkflow.id)
            val slotId = takeAvailableSlot(someWorkflow.id)

            // expect
            noSlotsAvailable(someWorkflow.id)

            // and
            rest.post("/workflow-runs") {
                contentType = MediaType.APPLICATION_JSON
                content = exampleWorkflowRunEvent(slotId)
            }.andExpect { status { isOk() } }

            // when
            rest.post("/workflow-runs") {
                contentType = MediaType.APPLICATION_JSON
                content = exampleWorkflowRunEvent(slotId)
            }.andExpect { status { isOk() } }

            // then
            takeAvailableSlot(someWorkflow.id)

            // and then
            noSlotsAvailable(someWorkflow.id)
        }

        test("should clean up used comment callback url") {
            // given
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            val targetRepository = Repository(id = Repository.Id("some-org", "target-repo"))
            val slotId = takeAvailableSlot(someWorkflow.id)
            commentCallbackGenerator.generate(someWorkflow.repository, targetRepository, 9, slotId)

            // when
            rest.post("/workflow-runs") {
                contentType = MediaType.APPLICATION_JSON
                content = exampleWorkflowRunEvent(slotId)
            }.andExpect { status { isOk() } }

            // then
            commentCallbackRepository.removeBySlotId(slotId) shouldBe false
        }

        test("should update check if workflow was cancelled") {
            // given
            var checkRunId = "my-check-id"
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            val targetRepository = Repository(id = Repository.Id("some-owner", "target-repo"))
            val slotId = takeAvailableSlot(someWorkflow.id)
            availableSlots.saveContext(slotId, SlotContext(slotId, targetRepository, checkRunId, "ref", "title", 123L))
            commentCallbackGenerator.generate(someWorkflow.repository, targetRepository, 9, slotId)
            githubApiMock.stubTokenResponse()
            githubApiMock.stubCheckUpdateResponse()

            // when
            rest.post("/workflow-runs") {
                contentType = MediaType.APPLICATION_JSON
                content = exampleWorkflowRunEvent(slotId, "cancelled")
            }.andExpect { status { isOk() } }

            // then
            githubApiMock.verifyCheckUpdated(targetRepository, checkRunId, """
                |{
                |  "output" : {
                |    "title" : "title",
                |    "summary" : "Workflow was cancelled by GitHub"
                |  },
                |  "conclusion" : "skipped"
                |}
                """.trimMargin())
        }

    }

    private fun noSlotsAvailable(workflowId: UUID) {
        shouldThrow<NoSlotsAvailable> {
            availableSlots.takeAvailableSlot(workflowId, randomConcurrencyGroup(), Instant.now())
        }
    }

    private fun takeAvailableSlot(workflowId: UUID): UUID {
        shouldNotThrowAny {
            return availableSlots.takeAvailableSlot(workflowId, randomConcurrencyGroup(), Instant.now()).id
        }
    }

    private fun exampleWorkflowRunEvent(slotId: UUID, conclusion: String = "failed"): String =
        getResourceAsText("example/workflow-run.json")!!
            .replace("{workflow_file_name}", "some-workflow-name.yml")
            .replace("{head_branch}", "main")
            .replace("{repository_owner}", "some-owner")
            .replace("{repository_name}", "some-repository")
            .replace("{display_title}", slotId.toString())
            .replace("{conclusion}", conclusion)
}

private fun randomConcurrencyGroup() = UUID.randomUUID().toString()

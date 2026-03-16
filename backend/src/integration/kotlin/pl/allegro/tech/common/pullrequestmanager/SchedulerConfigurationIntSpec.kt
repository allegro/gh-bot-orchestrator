package pl.allegro.tech.common.pullrequestmanager

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.nondeterministic.continually
import io.kotest.core.annotation.Ignored
import org.mockito.BDDMockito.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.internal.verification.Times
import org.springframework.test.context.bean.override.mockito.MockitoBean
import pl.allegro.tech.common.pullrequestmanager.api.PullRequestEvent
import pl.allegro.tech.common.pullrequestmanager.domain.AvailableSlots
import pl.allegro.tech.common.pullrequestmanager.domain.NoSlotsAvailable
import pl.allegro.tech.common.pullrequestmanager.domain.SlotContext
import pl.allegro.tech.common.pullrequestmanager.infra.task.ReactToPullRequestChangeScheduler
import kotlin.time.Duration.Companion.milliseconds

/*
This is exploratory test to play around with scheduler and see how it behaves
*/
@Ignored
class SchedulerConfigurationIntSpec(
    private val scheduler: ReactToPullRequestChangeScheduler,
    private val objectMapper: ObjectMapper
) : BaseIntegrationSpec() {

    @MockitoBean
    private lateinit var availableSlots: AvailableSlots

    val firstEvent = examplePullRequest(body = "1")
    val secondEvent = examplePullRequest(body = "2")

    init {

        test("should retry task after failure indefinitely; different workflows should independent") {
            // given
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            val someOtherWorkflow = workflowDefinitionFixtures.randomWorkflow()
            githubApiMock.stubTokenResponse()
            workflowDefinitionsReader.getDefinitionsIds().forEach { workflowLabel ->
                given(availableSlots.takeAvailableSlot(workflowLabel, any(), any()))
                    .willThrow(NoSlotsAvailable(workflowLabel))
            }

            // when
            scheduler.schedule(firstEvent)
            scheduler.schedule(secondEvent)

            // then
            continually(310.milliseconds) {
                githubApiMock.noWorkflowWasDispatched()
            }

            verify(availableSlots, Times(6))
                .takeAvailableSlot(someWorkflow.id, any(), PullRequestFixture.updatedAt)

            verify(availableSlots, Times(6))
                .takeAvailableSlot(someOtherWorkflow.id, any(), PullRequestFixture.updatedAt)
        }
    }

    private fun examplePullRequest(body: String) =
        objectMapper.readValue(examplePullRequestEvent(), PullRequestEvent::class.java)
            .withBody(body)

    private fun PullRequestEvent.withBody(body: String): PullRequestEvent {
        return copy(data = data.copy(pullRequest = data.pullRequest.copy(body = body)))
    }
}

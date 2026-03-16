package pl.allegro.tech.github.botorchestrator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import pl.allegro.tech.github.botorchestrator.domain.AvailableSlots
import pl.allegro.tech.github.botorchestrator.domain.NoSlotsAvailable
import pl.allegro.tech.github.botorchestrator.domain.SlotContext
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AvailableSlotsSpec(private val availableSlots: AvailableSlots) : BaseIntegrationSpec() {

    init {
        test("should throw exception when no more free slots") {
            val workflow = workflowDefinitionFixtures.randomWorkflow(maxConcurrentRuns = 1)

            availableSlots.takeAvailableSlot(workflow.id, "some-pull-request", Instant.now())
            availableSlots.takeAvailableSlot(workflow.id, "other-pull-request", Instant.now())

            // expect
            shouldThrow<NoSlotsAvailable> {
                availableSlots.takeAvailableSlot(workflow.id, "other-pull-request", Instant.now())
            }
        }

        test("should not allow to take more slots than specified in config") {
            // given lot-of-concurrent-runs has max-concurrent-runs set to 500
            val workflow = workflowDefinitionFixtures.randomWorkflow(maxConcurrentRuns = 500)
            var executed = AtomicInteger(0)
            val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(8))

            val jobs = (1..2_000).map { i ->
                scope.launch {
                    try {
                        availableSlots.takeAvailableSlot(workflow.id, "pull-request-$i", Instant.now())
                        executed.incrementAndGet()
                    } catch (_: NoSlotsAvailable) {
                    }
                }
            }

            jobs.joinAll()
            executed.get() shouldBeLessThanOrEqual 1_000
        }
    }
}

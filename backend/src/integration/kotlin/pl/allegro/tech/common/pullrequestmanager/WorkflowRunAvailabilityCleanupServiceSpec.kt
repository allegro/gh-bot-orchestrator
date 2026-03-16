package pl.allegro.tech.common.pullrequestmanager

import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import pl.allegro.tech.common.pullrequestmanager.infra.WorkflowRunAvailabilityCleanupService
import pl.allegro.tech.common.pullrequestmanager.domain.AvailableSlots
import pl.allegro.tech.common.pullrequestmanager.domain.SlotContext
import java.sql.Timestamp
import java.time.Instant
import java.util.*

class WorkflowRunAvailabilityCleanupServiceSpec(
    private val cleanupService: WorkflowRunAvailabilityCleanupService,
    private val availableSlots: AvailableSlots,
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : BaseIntegrationSpec() {

    init {
        test("should clean up old workflow_run_availability records") {
            // given
            val workflow = workflowDefinitionFixtures.randomWorkflow(maxConcurrentRuns = 10)
            val now = Instant.now()
            val oldTimestamp = now.minusSeconds(11 * 60)
            val recentTimestamp = now.minusSeconds(9 * 60)

            availableSlots.takeAvailableSlot(workflow.id, "old-concurrency-group", oldTimestamp)
            availableSlots.takeAvailableSlot(workflow.id, "old-concurrency-group-2", oldTimestamp)
            availableSlots.takeAvailableSlot(workflow.id, "recent-concurrency-group", recentTimestamp)

            // Verify initial state
            countWorkflowRunAvailabilityRecords() shouldBe 3

            // when
            cleanupService.cleanupOldWorkflowRunAvailabilityRecords()

            // then
            val remainingCount = countWorkflowRunAvailabilityRecords()
            remainingCount shouldBe 1

            val remainingRecord = getRemainingWorkflowRunAvailabilityRecord()
            remainingRecord["concurrency_group"] shouldBe "recent-concurrency-group"
        }

        test("should not affect recent workflow_run_availability records") {
            // given
            val workflow = workflowDefinitionFixtures.randomWorkflow(maxConcurrentRuns = 5)
            val now = Instant.now()
            val recentTimestamp = now.minusSeconds(2 * 60)

            availableSlots.takeAvailableSlot(workflow.id, "recent-concurrency-group-1", recentTimestamp)
            availableSlots.takeAvailableSlot(workflow.id, "recent-concurrency-group-2", recentTimestamp)

            countWorkflowRunAvailabilityRecords() shouldBe 2

            // when
            cleanupService.cleanupOldWorkflowRunAvailabilityRecords()

            // then
            val remainingCount = countWorkflowRunAvailabilityRecords()
            remainingCount shouldBe 2
        }

        test("should handle empty table gracefully") {
            // given
            countWorkflowRunAvailabilityRecords() shouldBe 0

            // when
            cleanupService.cleanupOldWorkflowRunAvailabilityRecords()

            // then
            countWorkflowRunAvailabilityRecords() shouldBe 0
        }
    }

    private fun countWorkflowRunAvailabilityRecords(): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM workflow_run_availability",
            emptyMap<String, Any>(),
            Int::class.java
        ) ?: 0
    }

    private fun getRemainingWorkflowRunAvailabilityRecord(): Map<String, Any> {
        return jdbcTemplate.queryForMap(
            "SELECT * FROM workflow_run_availability LIMIT 1",
            emptyMap<String, Any>()
        )
    }
}

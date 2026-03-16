package pl.allegro.tech.common.pullrequestmanager.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import pl.allegro.tech.common.andamio.metrics.micrometer.Timed
import java.sql.Timestamp
import java.time.Instant

@Component
class WorkflowRunAvailabilityCleanupService(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val properties: WorkflowRunAvailabilityCleanupProperties
) {

    @Scheduled(fixedRateString = "\${app.cleanup.workflow-run-availability.interval}")
    @Timed(name = "workflow-run-availability-cleanup")
    fun cleanupOldWorkflowRunAvailabilityRecords() {
        val cutoffTime = Instant.now().minus(properties.maxAge)

        logger.info { "Starting cleanup of workflow_run_availability records older than $cutoffTime" }

        val query = """
            DELETE FROM workflow_run_availability
            WHERE change_timestamp < :cutoff_time
        """

        val params = mapOf("cutoff_time" to Timestamp.from(cutoffTime))
        val deletedCount = jdbcTemplate.update(query, params)

        if (deletedCount > 0) {
            logger.info { "Cleaned up $deletedCount old workflow_run_availability records" }
        } else {
            logger.debug { "No old workflow_run_availability records to cleanup" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

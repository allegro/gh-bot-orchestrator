package pl.allegro.tech.common.pullrequestmanager.infra.task

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val CLEAN_UP_STALE_CONTEXTS = "clean-up-stale-contexts"

class CleanUpStaleContexts(
    private val jdbcTemplate: JdbcTemplate,
) : RecurringTask<Unit>(CLEAN_UP_STALE_CONTEXTS, FixedDelay.ofHours(24), Unit::class.java) {

    override fun executeRecurringly(
        taskInstance: TaskInstance<Unit?>?,
        executionContext: ExecutionContext?
    ) {
        try {
            val cutoffTime = Instant.now().minus(1, ChronoUnit.DAYS)
            val rowsAffected = jdbcTemplate.update("DELETE FROM slot_context WHERE created_at < ?", Timestamp.from(cutoffTime))
            logger.info { "Cleaned up $rowsAffected stale contexts" }
        } catch (e: Exception) {
            logger.error(e) { "Could not clean up stale-context" }
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}

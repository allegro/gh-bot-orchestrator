package pl.allegro.tech.common.pullrequestmanager.infra.postgres

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import pl.allegro.tech.common.pullrequestmanager.domain.AvailableSlots
import pl.allegro.tech.common.pullrequestmanager.domain.NoSlotsAvailable
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.SlotContext
import pl.allegro.tech.common.pullrequestmanager.domain.SlotId
import pl.allegro.tech.common.pullrequestmanager.domain.TakenSlot
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.WorkflowDefinitionsReader
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Component
class PostgresAvailableSlots(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val workflowDefinitionsReader: WorkflowDefinitionsReader
) : AvailableSlots {

    override fun takeAvailableSlot(workflowId: UUID, concurrencyGroup: String, changeTimestamp: Instant): TakenSlot {
        try {
            return takeSlot(workflowId, concurrencyGroup, changeTimestamp)
        } catch (_: DataIntegrityViolationException) {
            throw NoSlotsAvailable(workflowId)
        }
    }

    private fun takeSlot(
        workflowId: UUID,
        concurrencyGroup: String,
        changeTimestamp: Instant
    ): TakenSlot {
        val maxConcurrentRuns = workflowDefinitionsReader.getDefinitionFor(workflowId).maxConcurrentRuns
        val slotId = UUID.randomUUID()
        val params = mapOf(
            "slot_id" to slotId,
            "workflow_id" to workflowId,
            "concurrency_group" to concurrencyGroup,
            "change_timestamp" to Timestamp.from(changeTimestamp),
            "max_concurrent_runs" to maxConcurrentRuns * 2,
        )
        val query = """
            INSERT INTO workflow_run_availability(
               slot_id,
               workflow_id,
               concurrency_group,
               change_timestamp,
               workflow_concurrency_group
           )
           VALUES(
               :slot_id,
               :workflow_id,
               :concurrency_group,
               :change_timestamp,
                (
                    SELECT generate_series FROM generate_series(1, :max_concurrent_runs)
                    WHERE generate_series NOT IN (
                         SELECT workflow_concurrency_group FROM workflow_run_availability WHERE workflow_id = :workflow_id
                    ) LIMIT 1
                )
           )
           RETURNING workflow_concurrency_group;
    """

        val workflowConcurrencyGroup = jdbcTemplate.query(query, params, ResultSetExtractor {
            if (it.next()) {
                it.getInt("workflow_concurrency_group")
            } else {
                null
            }
        })

        if (workflowConcurrencyGroup == null) {
            throw NoSlotsAvailable(workflowId)
        }

        return TakenSlot(slotId, workflowConcurrencyGroup % maxConcurrentRuns, changeTimestamp)
    }

    override fun releaseSlot(slotId: SlotId) {
        val query = """
                DELETE
                FROM workflow_run_availability
                WHERE slot_id = :slot_id;
            """
        val params = MapSqlParameterSource(mapOf("slot_id" to slotId))
        jdbcTemplate.update(query, params)
    }

    override fun releaseSlotBy(workflowId: UUID, concurrencyGroup: String, changeTimestamp: Instant) {
        val query = """
                UPDATE workflow_run_availability
                SET finished_at = now()
                WHERE concurrency_group = :concurrency_group
                AND workflow_id = :workflow_id
                AND change_timestamp < :change_timestamp;
            """
        val params = MapSqlParameterSource(mapOf(
            "concurrency_group" to concurrencyGroup,
            "change_timestamp" to Timestamp.from(changeTimestamp),
            "workflow_id" to workflowId
        ))
        jdbcTemplate.update(query, params)
    }

    override fun saveContext(
        slotId: SlotId,
        slotContext: SlotContext
    ) {
        val query = """
            INSERT INTO slot_context (slot_id, check_run_id, repo_owner, repo_name, ref, check_title, pull_request_number)
            VALUES (:id, :check_run_id, :repo_owner, :repo_name, :ref, :check_title, :pull_request_number);
        """.trimIndent()
        jdbcTemplate.update(query, MapSqlParameterSource(mapOf(
            "id" to slotId,
            "check_run_id" to slotContext.checkRunId,
            "repo_owner" to slotContext.repository.id.owner,
            "repo_name" to slotContext.repository.id.name,
            "ref" to slotContext.ref,
            "check_title" to slotContext.checkTitle,
            "pull_request_number" to slotContext.pullRequestNumber,
        )))
    }

    override fun getContext(slotId: SlotId): SlotContext? {
        val query = """
            SELECT slot_id, check_run_id, repo_owner, repo_name, ref, check_title, pull_request_number FROM slot_context WHERE slot_id = :slotId;
        """.trimIndent()
        return jdbcTemplate.query<SlotContext?>(query, MapSqlParameterSource(mapOf("slotId" to slotId))) {
            if (it.next()) {
                SlotContext(
                    slotId = UUID. fromString(it.getString("slot_id")),
                    repository = Repository(it.getString("repo_owner"), it.getString("repo_name")),
                    ref = it.getString("ref"),
                    checkRunId = it.getString("check_run_id"),
                    checkTitle = it.getString("check_title"),
                    pullRequestNumber = it.getLong("pull_request_number"),
                )
            } else null
        }
    }

    companion object {

        val logger = KotlinLogging.logger {}
    }
}

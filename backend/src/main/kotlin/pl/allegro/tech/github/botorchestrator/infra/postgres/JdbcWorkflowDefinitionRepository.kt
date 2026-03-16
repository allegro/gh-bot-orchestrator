package pl.allegro.tech.github.botorchestrator.infra.postgres

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.transaction.annotation.Transactional
import pl.allegro.tech.github.botorchestrator.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.DependabotMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.FilePathMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.JsonPathMatcher
import pl.allegro.tech.github.botorchestrator.domain.workflows.PersistedWorkflowDefinition
import pl.allegro.tech.github.botorchestrator.domain.workflows.PersistedWorkflowDefinition.Check
import pl.allegro.tech.github.botorchestrator.domain.workflows.PersistedWorkflowDefinition.Check.DetailsPage
import pl.allegro.tech.github.botorchestrator.domain.workflows.PersistedWorkflowDefinition.Filters
import pl.allegro.tech.github.botorchestrator.domain.workflows.PersistedWorkflowDefinition.Mapping
import pl.allegro.tech.github.botorchestrator.domain.workflows.PersistedWorkflowDefinition.Matcher
import pl.allegro.tech.github.botorchestrator.domain.workflows.PersistedWorkflowDefinition.Repository
import pl.allegro.tech.github.botorchestrator.domain.workflows.WorkflowDefinitionRepository
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.ConcurrencyGroupType
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.MatchingStrategy
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinitionToAdd
import java.sql.ResultSet
import java.util.*

open class JdbcWorkflowDefinitionRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : WorkflowDefinitionRepository {

    override fun findById(id: UUID): PersistedWorkflowDefinition? {
        return jdbcTemplate.query(
            """
            SELECT
                workflow_definitions.*,
                workflow_filters.id AS filter_id,
                workflow_filters.matching_strategy,
                workflow_json_path_matchers.path,
                workflow_json_path_matchers.regex,
                workflow_file_matchers.pattern,
                workflow_file_matchers.statuses,
                workflow_dependabot_matchers.artifact_regex,
                workflow_field_mappings.json_path
            FROM workflow_definitions
            LEFT JOIN workflow_filters ON workflow_definitions.id = workflow_filters.workflow_id
            LEFT JOIN workflow_json_path_matchers ON workflow_filters.id = workflow_json_path_matchers.filter_id
            LEFT JOIN workflow_file_matchers ON workflow_filters.id = workflow_file_matchers.filter_id
            LEFT JOIN workflow_dependabot_matchers ON workflow_filters.id = workflow_dependabot_matchers.filter_id
            LEFT JOIN workflow_field_mappings ON workflow_definitions.id = workflow_field_mappings.workflow_id
            WHERE workflow_definitions.id = :id
            ORDER BY created_at DESC
        """,
            mapOf<String, UUID>("id" to id),
            ::toPersistedWorkflowDefinitions
        )?.firstOrNull()
    }

    override fun exists(repositoryName: String, repositoryOwner: String, ref: String, path: String): Boolean {
        return jdbcTemplate.queryForObject(
            """
                SELECT COUNT(id)
                FROM workflow_definitions
                WHERE ref = :ref
                  AND repository_name = :repositoryName
                  AND repository_owner = :repositoryOwner
                  AND :path LIKE CONCAT('%', workflow_filename, '%')
                LIMIT 1
            """, mapOf(
                "ref" to ref,
                "repositoryName" to repositoryName,
                "repositoryOwner" to repositoryOwner,
                "path" to path
            ), Int::class.java
        ).let { count -> count != null && count > 0 }
    }

    override fun findAll(): List<PersistedWorkflowDefinition> {
        return jdbcTemplate.query(
            """
            SELECT
                workflow_definitions.*,
                workflow_filters.id AS filter_id,
                workflow_filters.matching_strategy,
                workflow_json_path_matchers.path,
                workflow_json_path_matchers.regex,
                workflow_file_matchers.pattern,
                workflow_file_matchers.statuses,
                workflow_dependabot_matchers.artifact_regex,
                workflow_field_mappings.json_path
            FROM workflow_definitions
            LEFT JOIN workflow_filters ON workflow_definitions.id = workflow_filters.workflow_id
            LEFT JOIN workflow_json_path_matchers ON workflow_filters.id = workflow_json_path_matchers.filter_id
            LEFT JOIN workflow_file_matchers ON workflow_filters.id = workflow_file_matchers.filter_id
            LEFT JOIN workflow_dependabot_matchers ON workflow_filters.id = workflow_dependabot_matchers.filter_id
            LEFT JOIN workflow_field_mappings ON workflow_definitions.id = workflow_field_mappings.workflow_id
            ORDER BY created_at DESC
        """,
            ::toPersistedWorkflowDefinitions
        ) ?: emptyList()
    }

    override fun findEnabled(): Set<UUID> {
        return jdbcTemplate.query("SELECT id FROM workflow_definitions WHERE enabled = 'true'") { rs, _ -> UUID.fromString(rs.getString("id")) }.toSet()
    }

    @Transactional
    override fun insert(workflowDefinitionToAdd: WorkflowDefinitionToAdd): UUID {
        val persistedWorkflowId = insertWorkflow(workflowDefinitionToAdd)
        insertMapping(persistedWorkflowId, workflowDefinitionToAdd.eventMapper.paths)
        insertFilters(persistedWorkflowId, workflowDefinitionToAdd.filters)
        return persistedWorkflowId
    }

    @Transactional
    override fun update(id: UUID, workflowDefinitionToAdd: WorkflowDefinitionToAdd) {
        updateWorkflow(id, workflowDefinitionToAdd)
        deleteMapping(id)
        deleteFilters(id)
        insertMapping(id, workflowDefinitionToAdd.eventMapper.paths)
        insertFilters(id, workflowDefinitionToAdd.filters)
    }

    @Transactional
    override fun delete(id: UUID) {
        deleteMapping(id)
        deleteFilters(id)
        jdbcTemplate.update(
            "DELETE FROM workflow_definitions WHERE id = :id",
            mapOf("id" to id)
        )
    }

    private fun insertFilters(workflowId: UUID, filters: CompoundPullRequestMatcher) {
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update(
            "INSERT INTO workflow_filters (workflow_id, matching_strategy) VALUES (:workflowId, :matchingStrategy) RETURNING id",
            MapSqlParameterSource(mapOf("workflowId" to workflowId, "matchingStrategy" to filters.matchingStrategy.name)),
            keyHolder
        )
        val generatedFilterId = keyHolder.keys?.get("id") as? Number ?: throw IllegalStateException("Failed to get generated filter id")

        filters.matchers.forEach { matcher ->
            when (matcher) {
                is JsonPathMatcher -> {
                    jdbcTemplate.update(
                        "INSERT INTO workflow_json_path_matchers (filter_id, path, regex) VALUES (:filterId, :path, :regex)",
                        mapOf("filterId" to generatedFilterId, "path" to matcher.path, "regex" to matcher.pattern.pattern)
                    )
                }

                is FilePathMatcher -> {
                    jdbcTemplate.update(
                        "INSERT INTO workflow_file_matchers (filter_id, pattern, statuses) VALUES (:filterId, :pattern, :statuses)",
                        mapOf("filterId" to generatedFilterId, "pattern" to matcher.files.pattern, "statuses" to matcher.statuses.toTypedArray<String>())
                    )
                }

                is DependabotMatcher -> {
                    jdbcTemplate.update(
                        "INSERT INTO workflow_dependabot_matchers (filter_id, artifact_regex) VALUES (:filterId, :artifactRegex)",
                        mapOf("filterId" to generatedFilterId, "artifactRegex" to matcher.artifactRegex.pattern)
                    )
                }

                else -> {
                    throw IllegalArgumentException("Unknown matcher type: ${matcher::class.java}")
                }
            }
        }
    }

    private fun insertMapping(workflowId: UUID, mapping: List<String>) {
        mapping.forEach { field ->
            val sql = "INSERT INTO workflow_field_mappings (workflow_id, json_path) VALUES (:workflowId, :jsonPath)"
            jdbcTemplate.update(sql, mapOf("workflowId" to workflowId, "jsonPath" to field))
        }
    }

    private fun toPersistedWorkflowDefinitions(rs: ResultSet): List<PersistedWorkflowDefinition> {
        val workflows = mutableMapOf<UUID, PersistedWorkflowDefinition>()
        val filters = mutableMapOf<UUID, Filters>()
        val matchers = mutableMapOf<UUID, MutableSet<Matcher>>()
        val mappings = mutableMapOf<UUID, MutableSet<String>>()

        while (rs.next()) {
            val workflowId = UUID.fromString(rs.getString("id"))
            workflows.computeIfAbsent(workflowId) {
                PersistedWorkflowDefinition(
                    id = workflowId,
                    enabled = rs.getBoolean("enabled"),
                    workflowFileName = rs.getString("workflow_filename"),
                    ref = rs.getString("ref"),
                    repository = Repository(
                        owner = rs.getString("repository_owner"),
                        name = rs.getString("repository_name")
                    ),
                    concurrencyGroup = ConcurrencyGroupType.valueOf(rs.getString("concurrency_group")),
                    maxConcurrentRuns = rs.getInt("max_concurrent_runs"),
                    filters = Filters(
                        matchingStrategy = MatchingStrategy.valueOf(rs.getString("matching_strategy")),
                        matchers = emptyList()
                    ),
                    mapping = Mapping(fields = emptyList()),
                    check = Check(
                        name = rs.getString("check_name"),
                        detailsPage = DetailsPage(
                            title = rs.getString("check_title"),
                            summary = rs.getString("check_summary"),
                            details = rs.getString("check_details")
                        )
                    ),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                    updatedAt = rs.getTimestamp("updated_at").toInstant()
                )
            }

            val matcherSet = matchers.computeIfAbsent(workflowId) { mutableSetOf() }

            rs.getString("path")?.let { path ->
                rs.getString("regex")?.let { regex ->
                    matcherSet.add(PersistedWorkflowDefinition.JsonPathMatcher(path = path, regex = regex))
                }
            }

            rs.getString("pattern")?.let { pattern ->
                val statuses = (rs.getArray("statuses")?.array as? Array<String>)?.toSet() ?: emptySet()
                matcherSet.add(PersistedWorkflowDefinition.FileMatcher(files = pattern, statuses = statuses))
            }

            if (rs.getString("artifact_regex") != null) {
                matcherSet.add(PersistedWorkflowDefinition.DependabotMatcher(artifactRegex = rs.getString("artifact_regex")))
            }

            if (!filters.containsKey(workflowId)) {
                filters[workflowId] = Filters(
                    matchingStrategy = MatchingStrategy.valueOf(rs.getString("matching_strategy")),
                    matchers = emptyList(),
                )
            }

            val jsonPath = rs.getString("json_path")
            if (jsonPath != null) {
                mappings.computeIfAbsent(workflowId) { mutableSetOf() }.add(jsonPath)
            }
        }

        return workflows.map { (id, workflow) ->
            workflow.copy(
                filters = filters[id]!!.copy(
                    matchers = matchers[id]?.toList() ?: emptyList(),
                ),
                mapping = Mapping(fields = mappings[id]?.toList() ?: emptyList())
            )
        }
    }

    private fun insertWorkflow(workflowDefinitionToAdd: WorkflowDefinitionToAdd): UUID {
        val keyHolder = GeneratedKeyHolder()
        val sql = """
                INSERT INTO workflow_definitions (ref, max_concurrent_runs, concurrency_group,
                                                  repository_name, repository_owner, check_name, check_title,
                                                  check_summary, check_details, workflow_filename, enabled)
                VALUES (:ref, :maxConcurrentRuns, :concurrencyGroup,
                        :repositoryName, :repositoryOwner, :checkName, :checkTitle,
                        :checkSummary, :checkDetails, :workflowFilename, :enabled)
                RETURNING id
                """
        val params = MapSqlParameterSource(
            mapOf(
                "maxConcurrentRuns" to workflowDefinitionToAdd.maxConcurrentRuns,
                "concurrencyGroup" to workflowDefinitionToAdd.concurrencyGroup.getType().name,
                "repositoryName" to workflowDefinitionToAdd.repository.id.name,
                "repositoryOwner" to workflowDefinitionToAdd.repository.id.owner,
                "workflowFilename" to workflowDefinitionToAdd.fileName,
                "ref" to workflowDefinitionToAdd.ref,
                "checkName" to workflowDefinitionToAdd.check.name,
                "checkTitle" to workflowDefinitionToAdd.check.detailsPage.title,
                "checkSummary" to workflowDefinitionToAdd.check.detailsPage.summary,
                "checkDetails" to workflowDefinitionToAdd.check.detailsPage.details,
                "enabled" to workflowDefinitionToAdd.enabled,
            )
        )
        jdbcTemplate.update(sql, params, keyHolder)
        return UUID.fromString(keyHolder.keys?.get("id")?.toString()) ?: throw IllegalStateException("Failed to insert workflow definition")
    }

    private fun updateWorkflow(id: UUID, workflowDefinitionToAdd: WorkflowDefinitionToAdd) {
        val sql = """
                UPDATE workflow_definitions
                SET ref = :ref,
                    max_concurrent_runs = :maxConcurrentRuns,
                    concurrency_group = :concurrencyGroup,
                    repository_name = :repositoryName,
                    repository_owner = :repositoryOwner,
                    check_name = :checkName,
                    check_title = :checkTitle,
                    check_summary = :checkSummary,
                    check_details = :checkDetails,
                    workflow_filename = :workflowFilename,
                    enabled = :enabled,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """
        val params = MapSqlParameterSource(
            mapOf(
                "id" to id,
                "maxConcurrentRuns" to workflowDefinitionToAdd.maxConcurrentRuns,
                "concurrencyGroup" to workflowDefinitionToAdd.concurrencyGroup.getType().name,
                "repositoryName" to workflowDefinitionToAdd.repository.id.name,
                "repositoryOwner" to workflowDefinitionToAdd.repository.id.owner,
                "workflowFilename" to workflowDefinitionToAdd.fileName,
                "ref" to workflowDefinitionToAdd.ref,
                "checkName" to workflowDefinitionToAdd.check.name,
                "checkTitle" to workflowDefinitionToAdd.check.detailsPage.title,
                "checkSummary" to workflowDefinitionToAdd.check.detailsPage.summary,
                "checkDetails" to workflowDefinitionToAdd.check.detailsPage.details,
                "enabled" to workflowDefinitionToAdd.enabled,
            )
        )
        jdbcTemplate.update(sql, params)
    }

    private fun deleteMapping(workflowId: UUID) {
        jdbcTemplate.update(
            "DELETE FROM workflow_field_mappings WHERE workflow_id = :workflowId",
            mapOf("workflowId" to workflowId)
        )
    }

    private fun deleteFilters(workflowId: UUID) {
        jdbcTemplate.update(
            "DELETE FROM workflow_filters WHERE workflow_id = :workflowId",
            mapOf("workflowId" to workflowId)
        )
    }
}

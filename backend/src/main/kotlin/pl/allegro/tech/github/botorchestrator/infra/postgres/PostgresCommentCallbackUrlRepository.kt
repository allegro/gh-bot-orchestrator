package pl.allegro.tech.github.botorchestrator.infra.postgres

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.comment.CommentCallbackUrl
import pl.allegro.tech.github.botorchestrator.domain.comment.CommentCallbackUrlRepository
import java.sql.ResultSet
import java.util.*

@Component
class PostgresCommentCallbackUrlRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : CommentCallbackUrlRepository {

    override fun find(id: UUID): CommentCallbackUrl {
        val query = """
            SELECT * FROM comment_callback_urls
            WHERE id = :id
        """.trimIndent()

        return jdbcTemplate.query(query, mapOf("id" to id), ResultSetExtractor {
            if (it.next()) {
                CommentCallbackUrl(
                    workflowRepository = Repository(it.getString("owner"), it.getString("repository")),
                    targetRepository = Repository(it.getString("target_owner"), it.getString("target_repository")),
                    pullRequestNumber = it.getInt("pull_request_number"),
                    id = UUID.fromString(it.getString("id")),
                    slotId = UUID.fromString(it.getString("slot_id")),
                    githubCommentId = getGithubCommentId(it),
                    body = it.getString("body")
                )
            } else {
                null
            }
        }) ?: error("Expected to find comment callback url for id $id")
    }

    private fun getGithubCommentId(it: ResultSet): Long? {
        val githubCommentId = it.getLong("github_comment_id")
        return if (it.wasNull()) {
            null
        } else {
            githubCommentId
        }
    }

    override fun removeBySlotId(slotId: UUID): Boolean {
        logger.info { "removing comment for slotId=$slotId" }
        val query = """
            DELETE FROM comment_callback_urls
            WHERE slot_id = :slot_id
        """.trimIndent()

        return jdbcTemplate.update(query, mapOf("slot_id" to slotId)) == 1
    }

    override fun updateComment(id: UUID, githubCommentId: Long, body: String) {
        logger.info { "update comment for id=$id githubCommentId=$githubCommentId body=$body" }

        val query = """
            UPDATE comment_callback_urls
            SET github_comment_id = :github_comment_id, body = :body
            WHERE id = :id
        """.trimIndent()

        jdbcTemplate.update(query, mapOf("id" to id, "github_comment_id" to githubCommentId, "body" to body))
    }

    override fun save(commentCallbackUrl: CommentCallbackUrl) {
        logger.info { "update comment for id=${commentCallbackUrl.id} repository=${commentCallbackUrl.workflowRepository}" }
        val query = """
            INSERT INTO comment_callback_urls (id, slot_id, owner, repository, target_owner, target_repository, pull_request_number)
            VALUES (:id, :slot_id, :owner, :repository, :target_owner, :target_repository, :pull_request_number)
        """.trimIndent()
        jdbcTemplate.update(query, paramsBasedOn(commentCallbackUrl))
    }

    private fun paramsBasedOn(commentCallbackUrl: CommentCallbackUrl): Map<String, Any> = with(commentCallbackUrl) {
        mapOf(
            "id" to id,
            "slot_id" to slotId,
            "owner" to workflowRepository.id.owner,
            "repository" to workflowRepository.id.name,
            "pull_request_number" to pullRequestNumber,
            "target_owner" to targetRepository.id.owner,
            "target_repository" to targetRepository.id.name,
        )
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}

package pl.allegro.tech.github.botorchestrator.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import pl.allegro.tech.github.botorchestrator.domain.GithubClient
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.comment.CommentCallbackUrlRepository
import pl.allegro.tech.github.botorchestrator.infra.github.oidc.OwnershipValidator
import java.util.*

@Component
class CommentHandler(
    private val ownershipValidator: OwnershipValidator,
    private val commentCallbackUrlRepository: CommentCallbackUrlRepository,
    private val githubClient: GithubClient
) {

    fun handle(token: String, comment: Comment, commentId: UUID): HandleCommentResult {
        try {
            val (workflowRepository, targetRepository, pullRequestNumber) = commentCallbackUrlRepository.find(commentId)
            logger.atInfo {
                message = "Matched commentId to ${workflowRepository.fullName}#${pullRequestNumber}"
                payload = mapOf(
                    "repo" to workflowRepository.fullName,
                    "commentId" to commentId,
                )
            }

            // if OIDC token belongs to the workflow-owner repository, then post a comment to targetRepository
            return if (ownershipValidator.belongsToExpectedRepository(token, workflowRepository.fullName)) {
                handleComment(targetRepository, pullRequestNumber, comment, commentId)
            } else {
                WrongRepository
            }
        } catch (e: Exception) {
            logger.atError {
                cause = e
                message = "Failed to handle comment callback"
                payload = mapOf(
                    "commentId" to commentId,
                )
            }
            throw e
        }
    }

    private fun handleComment(repository: Repository, pullRequestNumber: Int, comment: Comment, commentId: UUID): HandleCommentResult {
        when (comment.mode) {
            CommentMode.NEW -> publishNewComment(repository, pullRequestNumber, comment, commentId)
            CommentMode.APPEND -> appendComment(commentId, repository, pullRequestNumber, comment)
            CommentMode.OVERWRITE -> overwriteComment(commentId, repository, pullRequestNumber, comment)
        }
        return Success
    }

    private fun publishNewComment(
        repository: Repository,
        pullRequestNumber: Int,
        comment: Comment,
        commentId: UUID
    ) {
        val githubCommentId = githubClient.publishNewComment(repository, pullRequestNumber, comment.body, commentId)
        commentCallbackUrlRepository.updateComment(commentId, githubCommentId, comment.body)
    }

    private fun appendComment(
        commentId: UUID,
        repository: Repository,
        pullRequestNumber: Int,
        comment: Comment
    ) {
        val savedComment = commentCallbackUrlRepository.find(commentId)
        val githubCommentId = savedComment.githubCommentId

        if (githubCommentId != null) {
            val updatedComment = savedComment.body + "\n" + comment.body
            githubClient.overwriteComment(repository, pullRequestNumber, updatedComment, githubCommentId)
            commentCallbackUrlRepository.updateComment(commentId, githubCommentId, updatedComment)
        } else {
            publishNewComment(repository, pullRequestNumber, comment, commentId)
        }
    }

    private fun overwriteComment(
        commentId: UUID,
        repository: Repository,
        pullRequestNumber: Int,
        comment: Comment
    ) {
        val githubCommentId = commentCallbackUrlRepository.find(commentId).githubCommentId
        if (githubCommentId != null) {
            githubClient.overwriteComment(repository, pullRequestNumber, comment.body, githubCommentId)
            commentCallbackUrlRepository.updateComment(commentId, githubCommentId, comment.body)
        } else {
            publishNewComment(repository, pullRequestNumber, comment, commentId)
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }

}

sealed interface HandleCommentResult
data object Success : HandleCommentResult
data object WrongRepository : HandleCommentResult
data class Comment(val mode: CommentMode = CommentMode.APPEND, val body: String)
enum class CommentMode {
    NEW, APPEND, OVERWRITE
}

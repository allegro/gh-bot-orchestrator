package pl.allegro.tech.common.pullrequestmanager.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.http.ResponseEntity.status
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.allegro.tech.common.pullrequestmanager.application.Comment
import pl.allegro.tech.common.pullrequestmanager.application.CommentHandler
import pl.allegro.tech.common.pullrequestmanager.application.CommentMode
import pl.allegro.tech.common.pullrequestmanager.application.Success
import pl.allegro.tech.common.pullrequestmanager.application.WrongRepository
import java.util.*

@RestController
@RequestMapping("/comments/{comment-id}")
class CommentsEndpoint(private val commentHandler: CommentHandler) {

    @PostMapping
    fun handleCommandCallback(
        @RequestHeader("Authorization") token: String,
        @RequestBody comment: CommentDto,
        @PathVariable("comment-id") commentId: UUID
    ): ResponseEntity<Unit> {
        logger.info { "Received comment $comment" }
        return when (commentHandler.handle(token, comment.toDomain(), commentId)) {
            Success -> ok().build()
            WrongRepository -> status(FORBIDDEN).build()
        }
    }

    companion object {

        val logger = KotlinLogging.logger {}
    }
}

@JsonInclude(NON_NULL)
data class CommentDto(val mode: String? = null, val body: String) {

    fun toDomain(): Comment {
        return Comment(mode = mode.toDomain(), body = body)
    }
}

private fun String?.toDomain(): CommentMode =
    CommentMode.valueOf(this ?: "APPEND")

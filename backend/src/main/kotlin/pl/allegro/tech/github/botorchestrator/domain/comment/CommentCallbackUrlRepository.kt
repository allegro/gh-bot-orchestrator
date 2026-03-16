package pl.allegro.tech.github.botorchestrator.domain.comment

import pl.allegro.tech.github.botorchestrator.domain.SlotId
import java.util.*

interface CommentCallbackUrlRepository {

    fun find(id: UUID): CommentCallbackUrl
    fun save(commentCallbackUrl: CommentCallbackUrl)
    fun removeBySlotId(slotId: SlotId): Boolean
    fun updateComment(id: UUID, githubCommentId: Long, body: String)

}

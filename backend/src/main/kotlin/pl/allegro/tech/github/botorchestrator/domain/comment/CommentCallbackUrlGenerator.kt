package pl.allegro.tech.github.botorchestrator.domain.comment

import org.springframework.stereotype.Component
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.SlotId
import pl.allegro.tech.github.botorchestrator.domain.UrlProvider
import pl.allegro.tech.github.botorchestrator.domain.Workflow
import java.util.*

@Component
class CommentUrlGenerator(
    private val urlProvider: UrlProvider,
    private val commentCallbackUrlRepository: CommentCallbackUrlRepository
) {

    fun generate(workflowRepository: Repository, targetRepository: Repository, pullRequestNumber: Int, slotId: SlotId): String {
        val id = UUID.randomUUID()
        val url = "${urlProvider.baseUrl()}/comments/$id"
        commentCallbackUrlRepository.save(
            CommentCallbackUrl(
                workflowRepository = workflowRepository,
                targetRepository = targetRepository,
                pullRequestNumber = pullRequestNumber,
                id = id,
                slotId = slotId
            )
        )

        return url
    }
}

data class CommentCallbackUrl(
    val workflowRepository: Repository,
    val targetRepository: Repository,
    val pullRequestNumber: Int,
    val id: UUID,
    val slotId: SlotId,
    val githubCommentId: Long? = null,
    val body: String? = null
)

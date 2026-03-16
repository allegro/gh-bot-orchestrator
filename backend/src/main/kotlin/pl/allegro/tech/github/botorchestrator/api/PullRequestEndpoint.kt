package pl.allegro.tech.github.botorchestrator.api

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.allegro.tech.github.botorchestrator.infra.task.ReactToPullRequestChangeScheduler

@RestController
@RequestMapping("/pull-requests")
class PullRequestEndpoint(
    private val scheduler: ReactToPullRequestChangeScheduler
) {

    @PostMapping
    fun handlePullRequestEvent(@RequestBody event: PullRequestEvent) {
        logger.atInfo {
            message = "Received event: ${event.copy(event.data.copy(pullRequest = event.data.pullRequest.copy(body = "")))}"
            payload = mapOf(
                "repo" to event.data.repository.fullName,
                "pullRequest" to event.data.pullRequest.htmlUrl,
                "pullRequestNumber" to event.data.pullRequest.number,
            )
        }

        scheduler.schedule(event)
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}

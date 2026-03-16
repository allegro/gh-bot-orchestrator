package pl.allegro.tech.common.pullrequestmanager.api

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.allegro.tech.common.pullrequestmanager.domain.CheckRunUpdated
import pl.allegro.tech.common.pullrequestmanager.domain.FailedToUpdateCheckRun
import pl.allegro.tech.common.pullrequestmanager.domain.GithubClient

@RestController
@RequestMapping("/check-callbacks")
class CheckCallbacksEndpoint(private val githubClient: GithubClient) {

    @PostMapping(path = ["/{owner}/{repo}/{checkId}"])
    fun handleCheckRunCallback(
        @PathVariable owner: String,
        @PathVariable repo: String,
        @PathVariable checkId: String,
        @RequestBody updateRequest: CheckRunUpdateRequest
    ): ResponseEntity<Any> {
        try {
            logger.atInfo {
                message = "Processing check callback to status: ${updateRequest.status}"
                payload = mapOf(
                    "repo" to "$owner/$repo",
                    "checkId" to checkId,
                )
            }

            return doHandle(owner, repo, checkId, updateRequest)
        } catch (e: Exception) {
            logger.atError {
                cause = e
                message = "Failed to process check callback"
                payload = mapOf(
                    "repo" to "$owner/$repo",
                    "checkId" to checkId,
                )
            }
            throw e
        }
    }

    private fun doHandle(
        owner: String,
        repo: String,
        checkId: String,
        updateRequest: CheckRunUpdateRequest
    ): ResponseEntity<Any> {
        val result = githubClient.updateCheck(
            checkId = checkId,
            owner = owner,
            repo = repo,
            conclusion = updateRequest.status,
            detailsPage = updateRequest.detailsPage
        )

        return when (result) {
            CheckRunUpdated -> ok().build()
            is FailedToUpdateCheckRun -> badRequest().build()
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}

data class CheckRunUpdateRequest(
    val status: String,
    val detailsPage: DetailsPageUpdateRequest?
) {

    data class DetailsPageUpdateRequest(
        val title: String,
        val summary: String,
        val details: String? = null,
        val customUrl: String? = null
    )
}

